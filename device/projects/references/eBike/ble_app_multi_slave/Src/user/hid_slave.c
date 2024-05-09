#include "multi_slave_config.h"
#include "user_keyboard.h"
#include "user_mouse.h"
#include "app_error.h"
#include "hids.h"
#include "dis.h"
#include "otas.h"
#include "utility.h"
#include "app_timer.h"
#include "multi_slave_manage.h"
#include "sample_service.h"
#include "rssi_check.h"
#include "long_term_user.h"
#include "user_gui.h"

static const uint8_t s_adv_fast_min_interval = 32;                /**< The fast advertising min interval (in units of 0.625 ms). */
static const uint8_t s_adv_fast_max_interval = 48;                /**< The fast advertising max interval (in units of 0.625 ms). */
static const uint8_t s_max_mtu_defualt = 247;                     /**< Defualt length of maximal MTU acceptable for device. */
static const uint8_t s_max_mps_default = 247;                     /**< Defualt length of maximal packet size acceptable for device. */
static const uint8_t s_max_nb_lecb_default = 10;                  /**< Defualt length of maximal number of LE Credit based connection. */
static const uint8_t s_max_tx_octel_default = 251;                /**< Default maximum transmitted number of payload octets. */
static const uint16_t s_max_tx_time_default = 2120;               /**< Defualt maximum packet transmission time. */

static  uint8_t s_slave_adv_data_set[] =                 /**< Advertising data. */
{
    0x0C,   // Length of this data
    BLE_GAP_AD_TYPE_COMPLETE_NAME,
    'E', 'b', 'i', 'k', 'e', 'x', '_', 'K', 'B','4','5',

    0x03,
    BLE_GAP_AD_TYPE_APPEARANCE,
    LO_U16(BLE_APPEARANCE_HID_MOUSE),
    HI_U16(BLE_APPEARANCE_HID_MOUSE),

    0x05,   // Length
    BLE_GAP_AD_TYPE_COMPLETE_LIST_16_BIT_UUID,
    LO_U16(BLE_ATT_SVC_HID),
    HI_U16(BLE_ATT_SVC_HID),
    LO_U16(BLE_ATT_SVC_DEVICE_INFO),
    HI_U16(BLE_ATT_SVC_DEVICE_INFO),
};

static const uint8_t s_slave_adv_rsp_data_set[] =             /**< Scan responce data. */
{
    // Manufacturer specific adv data type
    0x05,
    BLE_GAP_AD_TYPE_MANU_SPECIFIC_DATA,
    // Goodix SIG Company Identifier: 0x04F7
    0xF7,
    0x04,
    // Goodix specific adv data
    0x02, 0x03,
};

static ble_gap_adv_param_t      s_gap_adv_param;                  /**< Advertising parameters for legay advertising. */
static ble_gap_adv_time_param_t s_gap_adv_time_param;             /**< Advertising time parameter. */

static ble_gap_bdaddr_t s_bonded_bdaddr;

static void slave_adv_params_init(void);
static void slave_adv_start(void);
static void slave_hide_services(uint8_t conn_idx);
static void slave_ble_evt_handler(const ble_evt_t *p_evt);
static void slave_services_init(void);


const multi_dev_func_t hid_slave_func = 
{
    .adv_params_init = slave_adv_params_init,
    .ble_evt_handler = slave_ble_evt_handler,
    .adv_start = slave_adv_start,
    .hide_services = slave_hide_services,
    .services_init = slave_services_init,
};


static void slave_adv_params_init(void)
{
    sdk_err_t   error_code;

    s_gap_adv_param.adv_intv_max = s_adv_fast_max_interval;
    s_gap_adv_param.adv_intv_min = s_adv_fast_min_interval;
    s_gap_adv_param.adv_mode     = BLE_GAP_ADV_TYPE_ADV_IND;
    s_gap_adv_param.chnl_map     = BLE_GAP_ADV_CHANNEL_37_38_39;
    s_gap_adv_param.disc_mode    = BLE_GAP_DISC_MODE_GEN_DISCOVERABLE;
    s_gap_adv_param.filter_pol   = BLE_GAP_ADV_ALLOW_SCAN_ANY_CON_ANY;

    error_code = ble_gap_adv_param_set(HID_SLAVE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_gap_adv_param);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(HID_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_slave_adv_data_set, sizeof(s_slave_adv_data_set));
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(HID_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_SCAN_RSP, s_slave_adv_rsp_data_set, sizeof(s_slave_adv_rsp_data_set));
    APP_ERROR_CHECK(error_code);

    s_gap_adv_time_param.duration    = 0;
    s_gap_adv_time_param.max_adv_evt = 0;

    error_code = ble_gap_l2cap_params_set(s_max_mtu_defualt, s_max_mps_default, s_max_nb_lecb_default);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_data_length_set(s_max_tx_octel_default, s_max_tx_time_default);
    APP_ERROR_CHECK(error_code);

    ble_gap_pref_phy_set(BLE_GAP_PHY_ANY, BLE_GAP_PHY_ANY);
}

void hid_slave_adv_data_set(uint8_t *p_data)
{
    sdk_err_t   error_code;
    for(uint8_t i=0; i<6; i++)
    {
        s_slave_adv_data_set[7+i] = p_data[i];
    }
    error_code = ble_gap_adv_data_set(HID_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_slave_adv_data_set, sizeof(s_slave_adv_data_set));
    APP_ERROR_CHECK(error_code);
}

void hid_slave_adv_data_update(uint8_t *p_data)
{
    for(uint8_t i=0; i<6; i++)
    {
        s_slave_adv_data_set[7+i] = p_data[i];
    }
}

static void slave_adv_start(void)
{
    sdk_err_t error_code;
    uint8_t* addr = multi_slave_get_addr(HID_SLAVE_ADV_INDEX);
    ble_gap_bdaddr_t adv_addr = 
    {
        .addr_type = BLE_GAP_ADDR_TYPE_RANDOM_STATIC,
    };
    memcpy(adv_addr.gap_addr.addr, addr, 6);
    adv_addr.gap_addr.addr[5] |= 0xC0;

    error_code = ble_gap_addr_set(&adv_addr);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_start(HID_SLAVE_ADV_INDEX, &s_gap_adv_time_param);
    APP_ERROR_CHECK(error_code);
}

static void slave_hide_services(uint8_t conn_idx)
{
    ble_gatts_service_hide_set(conn_idx, hids_service_start_handle_get());
    ble_gatts_service_hide_set(conn_idx, dis_service_start_handle_get());
    ble_gatts_service_hide_set(conn_idx, otas_service_start_handle_get());
}

extern uint32_t password ;
static void app_sec_rcv_enc_req_handler(uint8_t conn_idx, const ble_sec_evt_enc_req_t *p_enc_req)
{
    ble_sec_cfm_enc_t cfm_enc;
    uint32_t      tk;

    if (NULL == p_enc_req)
    {
        return;
    }
    memset((uint8_t *)&cfm_enc, 0, sizeof(ble_sec_cfm_enc_t));

    switch (p_enc_req->req_type)
    {
        // user need to decide whether to accept the pair request
        case BLE_SEC_PAIR_REQ:
            cfm_enc.req_type = BLE_SEC_PAIR_REQ;
            cfm_enc.accept   = true;
            break;
        
        // user need to input the password
        case BLE_SEC_TK_REQ:
            cfm_enc.req_type = BLE_SEC_TK_REQ;
            cfm_enc.accept   = true;
            tk = password;
            memset(cfm_enc.data.tk.key, 0, 16);
            cfm_enc.data.tk.key[0] = (uint8_t)((tk & 0x000000FF) >> 0);
            cfm_enc.data.tk.key[1] = (uint8_t)((tk & 0x0000FF00) >> 8);
            cfm_enc.data.tk.key[2] = (uint8_t)((tk & 0x00FF0000) >> 16);
            cfm_enc.data.tk.key[3] = (uint8_t)((tk & 0xFF000000) >> 24);
            break;
        
        default:
            break;
    }

    ble_sec_enc_cfm(conn_idx, &cfm_enc);
}

static app_timer_id_t    s_rssi_get_timer_id;
static void slave_ble_evt_handler(const ble_evt_t *p_evt)
{
    ble_gap_white_list_t whitelist;
    sdk_err_t error_code;
    switch(p_evt->evt_id)
    {
        case BLE_GAPC_EVT_DISCONNECTED:
            slave_adv_start();
            app_timer_stop(s_rssi_get_timer_id);
            lock_open(false);
            break;
        
        case BLE_GAPC_EVT_CONNECTED:
            
            break;
        
         case BLE_SEC_EVT_LINK_ENC_REQUEST:
            app_sec_rcv_enc_req_handler(p_evt->evt.sec_evt.index, &(p_evt->evt.sec_evt.params.enc_req));
            break;

        case BLE_SEC_EVT_LINK_ENCRYPTED:
            if (BLE_SUCCESS == p_evt->evt_status)
            {
                error_code = ble_gap_whitelist_get(&whitelist);
                APP_ERROR_CHECK(error_code);
                printf("whitelist num = %d\r\n", whitelist.num);
                s_bonded_bdaddr = whitelist.items[0];
                printf("Link has been successfully encrypted.");
                printf("white liste dev-%02X:%02X:%02X:%02X:%02X:%02X",
                               s_bonded_bdaddr.gap_addr.addr[5],
                               s_bonded_bdaddr.gap_addr.addr[4],
                               s_bonded_bdaddr.gap_addr.addr[3],
                               s_bonded_bdaddr.gap_addr.addr[2],
                               s_bonded_bdaddr.gap_addr.addr[1],
                               s_bonded_bdaddr.gap_addr.addr[0]);
                ble_gap_privacy_mode_set(s_bonded_bdaddr, BLE_GAP_PRIVACY_MODE_DEVICE);
                rssi_clear();
                error_code = app_timer_start(s_rssi_get_timer_id, 100, NULL);
                APP_ERROR_CHECK(error_code);
                //lock_open(true);
            }
            else
            {
                printf("Pairing failed for error 0x%x.", p_evt->evt_status);
            }
            break;
            
        case BLE_GAPC_EVT_CONN_INFO_GOT:
            if(p_evt->evt.gapc_evt.params.conn_info.opcode == BLE_GAP_GET_CON_RSSI)
            {
                int16_t rssi =  p_evt->evt.gapc_evt.params.conn_info.info.rssi;
                printf("rssi = %d\r\n",rssi);
                rssi_value_add(rssi);
            }
            break;
        
        default:break;
    }
}

#include "app_log.h"
void samples_evt_handler(samples_evt_t *p_evt)
{
    uint8_t send_data[40];
     uint8_t conn_idx = multi_slave_get_conn_idx(HID_SLAVE_ADV_INDEX);
    switch(p_evt->evt_type)
    {
       
        case SAMPLES_EVT_RX_RECEIVE_DATA:
            if(p_evt->p_data[0] == 0x01 && p_evt->p_data[1] == 0x02)
            {
                printf("get info r\n");
                send_data[0] = 0x01;
                send_data[1] = 0x02;
                send_data[2] = p_evt->p_data[2];
                send_data[3] = 0x01;
                send_data[4] = 0x00;
                send_data[5] = 0x00;
                if(p_evt->p_data[2] != 0x01)
                {
                    samples_notify_tx_data(conn_idx, 0,send_data, 6);
                }
                else
                {
                    ble_gap_white_list_t whitelist;
                    ble_gap_whitelist_get(&whitelist);
                    printf("whitelist num = %d\r\n", whitelist.num);
                    send_data[6] = whitelist.num;
                    uint8_t len = 7;
                    for(uint8_t i=0; i<whitelist.num; i++)
                    {
                        s_bonded_bdaddr = whitelist.items[i];
                        for(uint8_t j=0; j<6; j++)
                        {
                            send_data[len] =  s_bonded_bdaddr.gap_addr.addr[5-j];
                            len++;
                        }
                    }
                    samples_notify_tx_data(conn_idx, 0,send_data, len);
                }
            }
            else if(p_evt->p_data[0] == 0x03 && p_evt->p_data[1] == 0x04)//通知已经绑定成功,管理员用户下发数据
            {
                hid_name_save(&p_evt->p_data[2], 6);
                save_user_id(&p_evt->p_data[8], 6);
                save_share_key(&p_evt->p_data[14],32);
                APP_LOG_INFO("save bond info\r\n");
            }
            else if(p_evt->p_data[0] == 0x05 && p_evt->p_data[1] == 0x06)//获取user id
            {
                send_data[0] = 0x05; 
                send_data[1] = 0x06;
                uint8_t *p_user_id = get_user_id();
                for(uint8_t i=0; i<6; i++)
                {
                    send_data[2+i] = p_user_id[i];
                }
                samples_notify_tx_data(conn_idx, 0,send_data, 8);
            }
            else if(p_evt->p_data[0] == 0x07 && p_evt->p_data[1] == 0x08)//删除配对设备
            {
                printf("delete device\r\n");
                ble_gap_bdaddr_t peer;
                for(uint8_t i=0; i<6; i++)
                {
                    peer.gap_addr.addr[i] = p_evt->p_data[2+i];
                }
                uint16_t status = ble_gap_bond_dev_del(&peer);
                uint8_t send_data[3] = {0};
                send_data[0] =0x07;
                send_data[1] = 0x08;
                send_data[2] = status;
                samples_notify_tx_data(conn_idx, 0,send_data, 3);
            }
            break;
        
        default:break;
    }
}


static void rssi_get_timeout_handler(void* p_arg)
{
    ble_gap_conn_info_get(multi_slave_get_conn_idx(HID_SLAVE_ADV_INDEX), BLE_GAP_GET_CON_RSSI);
}

static void slave_services_init(void)
{
    sdk_err_t error_code;
    otas_init_t otas_init;
    samples_init_t sample_init;
    
    user_keyboard_service_init();
    
    otas_init.evt_handler = NULL;
    error_code = otas_service_init(&otas_init);
    APP_ERROR_CHECK(error_code);
    
    sample_init.evt_handler = samples_evt_handler;
    error_code = samples_service_init(&sample_init, 1);
    APP_ERROR_CHECK(error_code);
    
    error_code = app_timer_create(&s_rssi_get_timer_id, ATIMER_REPEAT, rssi_get_timeout_handler);
    APP_ERROR_CHECK(error_code);
}

