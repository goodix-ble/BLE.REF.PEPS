
#include "fmna_connection_platform.h"
#include "fmna_constants.h"
#include "fmna_connection.h"
#include "fmna_state_machine.h"
#include "fmna_adv_platform.h"
#include "fmna_gatt_fmns_platform.h"
#include "fmna_storage_platform.h"
#include "fmna_log_platform.h"
#if FMNA_UARP_ENABLE
#include "fmna_uarp_control_point.h"
#endif

void fmna_sec_info_request_handler(uint8_t conn_idx)
{
    extern uint16_t ble_sec_ltk_set(uint8_t conn_idx, uint8_t *p_ltk, uint8_t size);

    ble_sec_ltk_set(conn_idx, fmna_connection_get_active_ltk(), 16);
}

void fmna_connection_platform_connected_handler(uint8_t conn_idx, const ble_gap_evt_connected_t *p_param)
{
    uint8_t local_con_addr[6];

    extern void ble_gap_conn_local_addr_get(uint8_t conidx,uint8_t *p_addr);

    ble_gap_conn_local_addr_get(conn_idx, local_con_addr);

    ble_gap_conn_local_addr_get(conn_idx,local_con_addr);

    if(memcmp(g_fmna_adv_addr.gap_addr.addr,local_con_addr,6) == 0)
    {
        ble_gattc_mtu_exchange(conn_idx);
        fmna_connection_connected_handler(conn_idx, p_param->conn_interval);
    }
}

void fmna_connection_platform_connection_update_req_handler(uint8_t conn_idx, const ble_gap_evt_conn_param_update_req_t *p_conn_param_update_req)
{
    ble_gap_conn_param_update_reply(conn_idx, true);
}

void fmna_connection_platform_connection_update_handler(uint8_t conn_idx, const ble_gap_evt_conn_param_updated_t *p_conn_param_update_info)
{
    fmna_connection_conn_param_update_handler(conn_idx, p_conn_param_update_info->conn_interval);
}

void fmna_connection_platform_sec_rcv_enc_req_handler(uint8_t conn_idx, const ble_sec_evt_enc_req_t *p_enc_req)
{
    ble_sec_cfm_enc_t cfm_enc;

    if (NULL == p_enc_req)
    {
        return;
    }

    memset((uint8_t *)&cfm_enc, 0, sizeof(ble_sec_cfm_enc_t));

    cfm_enc.req_type = p_enc_req->req_type;

    if (fmna_connection_is_fmna_paired())
    {
        FMNA_LOG_INFO("Incoming security request reject [Already paired]");
        cfm_enc.accept = false;
    }
    else
    {
        FMNA_LOG_INFO("Incoming security request accept [Not paired]");
        cfm_enc.accept = true;
    }


    ble_sec_enc_cfm(conn_idx, &cfm_enc);
}

void fmna_connection_platform_sec_rcv_enc_ind_handler(uint8_t conn_idx, uint16_t enc_ind, ble_sec_evt_enc_ind_t auth)
{
    if (BLE_SUCCESS == enc_ind)
    {
        fmna_connection_update_connection_info(conn_idx, FMNA_MULTI_STATUS_ENCRYPTED, true);
        fmna_evt_handler(FMNA_SM_EVENT_BONDED, NULL);
    }

    if (!fmna_connection_is_fmna_paired())
    {
        FMNA_ERROR_CHECK(enc_ind);
    }
}

void fmna_connection_platform_disconnect_handler(uint8_t conn_idx, uint8_t reason)
{
    fmna_connection_disconnected_handler(conn_idx, reason);
    #if FMNA_UARP_ENABLE
    fmna_uarp_disconnect(conn_idx);
    #endif
}

fmna_ret_code_t fmna_connection_platform_disconnect(uint8_t conn_handle)
{
    extern void ble_task_force_schedule(void);

    uint16_t ret = ble_gap_disconnect(conn_handle);
    ble_task_force_schedule();

    return ret;
}

void fmna_connection_platform_gap_params_init(void)
{
    fmna_ret_code_t          err_code;

    err_code = ble_gap_device_name_set(BLE_GAP_WRITE_PERM_UNAUTH, (const uint8_t *)g_fmna_info_cfg.model_name_str, strlen(g_fmna_info_cfg.model_name_str));
    FMNA_ERROR_CHECK(err_code);

    err_code = ble_gap_data_length_set(FMNA_BLE_DATA_LENGTH, 2120);
    FMNA_ERROR_CHECK(err_code);

    err_code = ble_gap_l2cap_params_set(FMNA_BLE_MTU_SIZE, FMNA_BLE_MTU_SIZE, 1);
    FMNA_ERROR_CHECK(err_code);

    ble_gap_pref_phy_set(BLE_GAP_PHY_ANY, BLE_GAP_PHY_ANY);
}


#define MFI_TOKEN_MAX_LOG_CHUNK 64
void fmna_connection_platform_log_token(void * auth_token, uint16_t token_size, uint8_t isCrash)
{
    uint16_t token_remaining = token_size;
    uint8_t* p_temp = (uint8_t *)auth_token;
    uint16_t to_print;

    FMNA_LOG_DEBUG("MFi token preview:");
    while (token_remaining)
    {
        if (token_remaining > MFI_TOKEN_MAX_LOG_CHUNK)
        {
            to_print = MFI_TOKEN_MAX_LOG_CHUNK;
        }
        else
        {
            to_print = token_remaining;
        }
        FMNA_LOG_HEXDUMP_DEBUG(p_temp, to_print);
        token_remaining -= to_print;
        p_temp += to_print;
    }
}

void fmna_connection_platform_get_serial_number(uint8_t * pSN, uint8_t length)
{
    memcpy(pSN, g_fmna_info_cfg.serial_number, length);

    FMNA_LOG_DEBUG("Serial number preview");
    FMNA_LOG_HEXDUMP_DEBUG(pSN, length);
}

bool m_new_token_stored = false;


void fmna_connection_update_mfi_token_storage(void *p_data, uint16_t data_size)
{
    m_new_token_stored = false;

    //erase the MFi Token / UUID
    if (!fmna_storage_platform_flash_erase(g_fmna_info_cfg.software_auth_uuid_save_addr, FMNA_STORAGE_FLASH_SECTOR_SIZE))
    {
        m_new_token_stored = false;
        goto dispatch;
    }

    if (data_size != fmna_storage_platform_flash_write(g_fmna_info_cfg.software_auth_uuid_save_addr, p_data, data_size))
    {
        m_new_token_stored = false;
        goto dispatch;
    }

    m_new_token_stored = true;

dispatch:
    fmna_state_machine_dispatch_event(FMNA_SM_EVENT_FMNA_PAIRING_MFITOKEN);
}



bool fmna_connection_mfi_token_stored(void)
{
    return m_new_token_stored;
}



