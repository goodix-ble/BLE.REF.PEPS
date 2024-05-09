#include "fmna_application.h"
#include "fmna_log_platform.h"
#include "fmna_storage_platform.h"
#include "fmna_malloc_platform.h"
#include "fmna_queue_platform.h"

#include "fmna_sound_platform.h"
#include "fmna_battery_platform.h"
#include "fmna_nfc_platform.h"
#include "fmna_motion_detection_platform.h"
#include "fmna_peer_manager_platform.h"
#include "fmna_connection_platform.h"
#include "fmna_storage_platform.h"

#include "fmna_version.h"
#include "fmna_motion_detection.h"
#include "fmna_nfc.h"
#include "fmna_crypto.h"
#include "fmna_connection.h"
#include "fmna_state_machine.h"
#include "fmna_malloc_platform.h"
#include "fmna_uarp_control_point.h"
#include "fmna_paired_owner_control_point.h"


#include "fmna_commit_id.h"

#include "dfu_port.h"

#define FMNA_APPLICAITON_RET_VERIFY(ret)   \
do                                         \
{                                          \
    if (ret)                               \
    {                                      \
        return ret;                        \
    }                                      \
} while(0)

#define FMNA_APPLICAITON_UARP_NTF(evt, arg)     \
do                                              \
{                                               \
    if (s_uarp_pro_notify)                      \
    {                                           \
        s_uarp_ntf_evt.notify_type = evt;       \
        s_uarp_ntf_evt.param       = arg;       \
        s_uarp_pro_notify(&s_uarp_ntf_evt);     \
    }                                           \
} while(0)


fmna_startup_info_cfg_t g_fmna_info_cfg;
fmna_uarp_cfg_t         g_fmna_uarp_cfg;

bool                    g_is_enable_debug;

bool                    g_is_enable_nfc;
uint16_t                g_accessory_pid;


#if FMNA_UARP_ENABLE
static fmna_uarp_pro_notify_t   s_uarp_pro_notify;
static fmna_uarp_ntf_evt_t      s_uarp_ntf_evt;

static uint32_t                 s_ota_file_size;
static uint32_t                 s_ota_file_rev_size;
static uint32_t                 s_file_program_addr;
static uint32_t                 s_erase_sector_addr;
static dfu_info_t               s_dfu_info;

static void fmna_uarp_evt_handler(UARPAppEvent_t *p_evt)
{
    switch (p_evt->evt_id)
    {
        case kUARPAppEventBinarySize:
            s_ota_file_rev_size = 0;
            s_ota_file_size     = p_evt->param.BinaryFileSize;
            s_file_program_addr = g_fmna_uarp_cfg.dfu_fw_save_addr;
            s_erase_sector_addr = g_fmna_uarp_cfg.dfu_fw_save_addr;
            FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_DFU_FILE_INFO_RECV, p_evt->param.BinaryFileSize);

            if (!fmna_storage_platform_flash_erase(g_fmna_uarp_cfg.dfu_fw_save_addr, FMNA_STORAGE_FLASH_SECTOR_SIZE))
            {
                FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_ERROR_OCCUR, 1);
            }
            break;

        case kUARPAppEventBinaryFragment:
            s_ota_file_rev_size += p_evt->param.BinaryFragment.length;
            FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_DFU_FILE_PROGRAM, s_ota_file_rev_size * 100 / s_ota_file_size);

            if ((s_file_program_addr + p_evt->param.BinaryFragment.length) > s_erase_sector_addr + FMNA_STORAGE_FLASH_SECTOR_SIZE)
            {
                if (!fmna_storage_platform_flash_erase(s_erase_sector_addr + FMNA_STORAGE_FLASH_SECTOR_SIZE, FMNA_STORAGE_FLASH_SECTOR_SIZE))
                {
                    FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_ERROR_OCCUR, 2);
                }
                s_erase_sector_addr += FMNA_STORAGE_FLASH_SECTOR_SIZE;
            }
            if (p_evt->param.BinaryFragment.length != fmna_storage_platform_flash_write(s_file_program_addr, p_evt->param.BinaryFragment.p_data, p_evt->param.BinaryFragment.length))
            {
                FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_ERROR_OCCUR, 3);
            }
            s_file_program_addr += p_evt->param.BinaryFragment.length;
            break;

        case kUARPAppEventBinaryComplete:
            memset(&s_dfu_info, 0, sizeof(s_dfu_info));
            s_dfu_info.dfu_fw_save_addr = g_fmna_uarp_cfg.dfu_fw_save_addr;
            s_dfu_info.dfu_mode_pattern = DFU_COPY_UPGRADE_MODE_PATTERN;
            if (sizeof(dfu_image_info_t) != fmna_storage_platform_flash_read(s_file_program_addr - 48, (uint8_t *)&s_dfu_info.dfu_img_info, sizeof(dfu_image_info_t)))
            {
                FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_ERROR_OCCUR, 4);
            }
            break;

        case kUARPAppEventBinaryApply:
            FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_DFU_INFO_UPDEATE, 0);
            if (!fmna_storage_platform_flash_erase(g_fmna_uarp_cfg.dfu_info_save_addr, FMNA_STORAGE_FLASH_SECTOR_SIZE))
            {
                FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_ERROR_OCCUR, 6);
            }
            if (sizeof(s_dfu_info) != fmna_storage_platform_flash_write(g_fmna_uarp_cfg.dfu_info_save_addr, (uint8_t *)&s_dfu_info, sizeof(s_dfu_info)))
            {
                FMNA_APPLICAITON_UARP_NTF(FMNA_UARP_ERROR_OCCUR, 7);
            }
            hal_nvic_system_reset();
            break;

        default:
            break;
    }
}
#endif

fmna_ret_code_t fmna_application_startup(fmna_startup_info_cfg_t *p_info_cfg, fmna_startup_cb_cfg_t *p_cb_cfg)
{
    if (NULL == p_info_cfg || NULL == p_cb_cfg)
    {
        return FMNA_ERROR_NULL;
    }

    FMNA_APPLICAITON_RET_VERIFY(fmna_log_init(p_cb_cfg->log_output));
    FMNA_LOG_INFO("FMNADK [%s] [%s] [%s %s]", FMNADK_VERSION, COMMIT_ID, __DATE__, __TIME__);
    FMNA_APPLICAITON_RET_VERIFY(fmna_queue_platform_init(p_cb_cfg->queue_send, p_cb_cfg->queue_receive));
    FMNA_APPLICAITON_RET_VERIFY(fmna_malloc_platform_init(p_cb_cfg->mem_malloc, p_cb_cfg->mem_realloc, p_cb_cfg->mem_free));
    FMNA_APPLICAITON_RET_VERIFY(fmna_storage_platform_init(p_cb_cfg->flash_init, p_cb_cfg->flash_read, p_cb_cfg->flash_write, p_cb_cfg->flash_erase));
    FMNA_APPLICAITON_RET_VERIFY(fmna_sound_platform_init(p_cb_cfg->speaker_on, p_cb_cfg->speaker_off));
    FMNA_APPLICAITON_RET_VERIFY(fmna_battery_platform_init(p_cb_cfg->bat_level_get));
    FMNA_APPLICAITON_RET_VERIFY(fmna_motion_detection_platform_init(p_cb_cfg->motion_detect_start, p_cb_cfg->motion_detect_stop, p_cb_cfg->is_motion_detected));

    memcpy(&g_fmna_info_cfg, p_info_cfg, sizeof(g_fmna_info_cfg));

    fmna_peer_manager_init();
    fmna_connection_platform_gap_params_init();
    fmna_gatt_platform_services_init();

    fmna_connection_init();
    fmna_crypto_init();
    fmna_motion_detection_init();
    if (g_is_enable_nfc)
    {
        fmna_nfc_init();
    }
#if FMNA_UARP_ENABLE
    fmna_uarp_control_point_init(&fmna_uarp_evt_handler);
#endif
    fmna_state_machine_init();

    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_application_debug_enable(void)
{
    #if !FMNA_DEBUG_ENABLE
    return FMNA_ERROR_INVALID_STATE;
    #else
    g_is_enable_debug = true;

    return FMNA_SUCCESS;
    #endif
}

fmna_ret_code_t fmna_application_nfc_enable(fmna_nfc_info_update_t info_update, uint16_t accessory_pid)
{
    #if !FMNA_NFC_ENABLE
    return FMNA_ERROR_INVALID_STATE;
    #else
    FMNA_APPLICAITON_RET_VERIFY(fmna_nfc_platform_init(info_update));

    g_is_enable_nfc = true;
    g_accessory_pid = accessory_pid;

    return FMNA_SUCCESS;
    #endif
}

fmna_ret_code_t fmna_application_uarp_enable(fmna_uarp_cfg_t *p_uarp_cfg, fmna_uarp_pro_notify_t uarp_pro_notify)
{
    #if !FMNA_UARP_ENABLE

    return FMNA_ERROR_INVALID_STATE;
    #else

    if (NULL == p_uarp_cfg)
    {
        return FMNA_ERROR_NULL;
    }

    s_uarp_pro_notify = uarp_pro_notify;

    memcpy(&g_fmna_uarp_cfg, p_uarp_cfg, sizeof(fmna_uarp_cfg_t));

    return FMNA_SUCCESS;
    #endif
}

void fmna_application_task(void)
{
    fmna_task_evt_t fmna_event;

    while (!fmna_queue_platform_evt_get(&fmna_event))
    {
        if (fmna_event.evt_handler)
        {
            fmna_event.evt_handler(fmna_event.p_data, fmna_event.data_size);
        }

        if (fmna_event.p_data && fmna_event.data_size)
        {
            fmna_free(fmna_event.p_data);
        }
    }
}

void fmna_application_factory_reset(void)
{
    fmna_storage_platform_key_value_delete(FMNA_PAIRED_STATE_NV_TAG);
    hal_nvic_system_reset();
}

void fmna_application_active_set(bool is_enable)
{
    if (is_enable == g_fmna_active_enable)
    {
        return;
    }

    g_fmna_active_enable = is_enable;

    fmna_state_machine_fmna_active_set(g_fmna_active_enable);
}

static fmna_timer_handle_t  s_fmna_serial_num_report_timeout_id;

static bool s_fmna_serial_num_report_timer_creat;

static void fmna_serial_num_report_timeout_handler(void)
{
    g_serial_num_report_enable = false;
}

void fmna_application_serial_num_report_set(bool is_enable)
{
    bool ret;

    if (g_serial_num_report_enable == is_enable)
    {
        return;
    }

    g_serial_num_report_enable = is_enable;

    if (g_serial_num_report_enable)
    {
        if (!s_fmna_serial_num_report_timer_creat)
        {
            ret = fmna_timer_create(&s_fmna_serial_num_report_timeout_id, false, fmna_serial_num_report_timeout_handler);
            FMNA_BOOL_CHECK(ret);
            if (!ret)
            {
                return;
            }
            s_fmna_serial_num_report_timer_creat = true;
        }

        ret = fmna_timer_start(&s_fmna_serial_num_report_timeout_id, 300000);
        FMNA_BOOL_CHECK(ret);
    }
}


void fmna_application_ble_evt_handle(const ble_evt_t *p_evt)
{
    if(p_evt == NULL)
    {
        return;
    }

    switch(p_evt->evt_id)
    {
        case BLE_GAPM_EVT_ADV_START:
            if (FMNA_BLE_ADV_INDEX == (p_evt->evt.gapm_evt.index))
            {
                fmna_adv_platform_start_handler(p_evt->evt.gapm_evt.index,
                                                p_evt->evt_status);
            }
            break;

        case BLE_GAPM_EVT_ADV_STOP:
            if (FMNA_BLE_ADV_INDEX == (p_evt->evt.gapm_evt.index))
            {
                fmna_adv_platform_stop_handler(p_evt->evt.gapm_evt.index,
                                               p_evt->evt_status,
                                               p_evt->evt.gapm_evt.params.adv_stop.reason);
            }
            break;

        case BLE_GAPC_EVT_CONNECTED:
            fmna_connection_platform_connected_handler(p_evt->evt.gapc_evt.index,
                                                       &(p_evt->evt.gapc_evt.params.connected));
            break;

        case BLE_GAPC_EVT_DISCONNECTED:
            if (fmna_connection_is_fmna_active(p_evt->evt.gapc_evt.index))
            {
                fmna_connection_platform_disconnect_handler(p_evt->evt.gapc_evt.index, p_evt->evt.gapc_evt.params.disconnected.reason);
            }
            break;

        case BLE_GAPC_EVT_CONN_PARAM_UPDATE_REQ:
            if (fmna_connection_is_fmna_active(p_evt->evt.gapc_evt.index))
            {
                fmna_connection_platform_connection_update_req_handler(p_evt->evt.gapc_evt.index,
                                                                       &(p_evt->evt.gapc_evt.params.conn_param_update_req));
            }
            break;

        case BLE_GAPC_EVT_CONN_PARAM_UPDATED:
            if (fmna_connection_is_fmna_active(p_evt->evt.gapc_evt.index) && BLE_SUCCESS == p_evt->evt_status)
            {
                fmna_connection_platform_connection_update_handler(p_evt->evt.gapc_evt.index,&(p_evt->evt.gapc_evt.params.conn_param_updated));
            }
            break;

        case BLE_SEC_EVT_LINK_ENC_REQUEST:
            if (fmna_connection_is_fmna_active(p_evt->evt.sec_evt.index))
            {
                fmna_connection_platform_sec_rcv_enc_req_handler(p_evt->evt.sec_evt.index, &(p_evt->evt.sec_evt.params.enc_req));
            }
            break;

        case BLE_SEC_EVT_LINK_ENCRYPTED:
            if (fmna_connection_is_fmna_active(p_evt->evt.sec_evt.index))
            {
                fmna_connection_platform_sec_rcv_enc_ind_handler(p_evt->evt.sec_evt.index,
                                                                 p_evt->evt_status,
                                                                 p_evt->evt.sec_evt.params.enc_ind);
            }
            break;

        case BLE_SEC_EVT_LTK_REQ:
            if (fmna_connection_is_fmna_active(p_evt->evt.sec_evt.index))
            {
                fmna_sec_info_request_handler(p_evt->evt.sec_evt.index);
            }
            break;

        case BLE_GATT_COMMON_EVT_MTU_EXCHANGE:
            if (fmna_connection_is_fmna_active(p_evt->evt.gatt_common_evt.index))
            {
                fmna_gatt_platform_mtu_update_handler(p_evt->evt_status, p_evt->evt.gatt_common_evt.params.mtu_exchange.mtu);
            }
    }
}

