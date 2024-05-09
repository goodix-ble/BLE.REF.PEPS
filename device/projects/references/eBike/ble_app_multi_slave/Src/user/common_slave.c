#include "multi_slave_config.h"
#include "multi_slave_manage.h"
#include "gus.h"
#include "communication.h"
#include "app_timer.h"
#include "app_error.h"
#include "long_term_user.h"
#include "user_gui.h"
#include "rssi_check.h"

static const uint8_t s_adv_fast_min_interval = 32;                /**< The fast advertising min interval (in units of 0.625 ms). */
static const uint8_t s_adv_fast_max_interval = 48;                /**< The fast advertising max interval (in units of 0.625 ms). */
static const uint8_t s_max_mtu_defualt = 247;                     /**< Defualt length of maximal MTU acceptable for device. */
static const uint8_t s_max_mps_default = 247;                     /**< Defualt length of maximal packet size acceptable for device. */
static const uint8_t s_max_nb_lecb_default = 10;                  /**< Defualt length of maximal number of LE Credit based connection. */
static const uint8_t s_max_tx_octel_default = 251;                /**< Default maximum transmitted number of payload octets. */
static const uint16_t s_max_tx_time_default = 2120;               /**< Defualt maximum packet transmission time. */

static uint8_t s_slave_adv_data_set[] =                     /**< Advertising data. */
{
     // Complete Name
    0x0a,
    BLE_GAP_AD_TYPE_COMPLETE_NAME,
    'C', 'o', 'm','1','2','3','4','5','6',
    
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

static void slave_adv_params_init(void);
static void slave_adv_start(void);
static void slave_hide_services(uint8_t conn_idx);
static void slave_ble_evt_handler(const ble_evt_t *p_evt);
static void slave_services_init(void);

const multi_dev_func_t com_slave_func = 
{
    .adv_params_init = slave_adv_params_init,
    .ble_evt_handler = slave_ble_evt_handler,
    .adv_start = slave_adv_start,
    .hide_services = slave_hide_services,
    .services_init = slave_services_init,
};

void common_slave_adv_data_set(uint8_t *p_data)
{
    sdk_err_t   error_code;
    for(uint8_t i=0; i<6; i++)
    {
        s_slave_adv_data_set[5+i] = p_data[i];
    }
    error_code = ble_gap_adv_data_set(COMMON_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_slave_adv_data_set, sizeof(s_slave_adv_data_set));
    APP_ERROR_CHECK(error_code);
}

void common_slave_adv_data_update(uint8_t *p_data)
{
    for(uint8_t i=0; i<6; i++)
    {
        s_slave_adv_data_set[5+i] = p_data[i];
    }
}


static void slave_adv_params_init(void)
{
    sdk_err_t   error_code;

    s_gap_adv_param.adv_intv_max = s_adv_fast_max_interval;
    s_gap_adv_param.adv_intv_min = s_adv_fast_min_interval;
    s_gap_adv_param.adv_mode     = BLE_GAP_ADV_TYPE_ADV_IND;
    s_gap_adv_param.chnl_map     = BLE_GAP_ADV_CHANNEL_37_38_39;
    s_gap_adv_param.disc_mode    = BLE_GAP_DISC_MODE_GEN_DISCOVERABLE;
    s_gap_adv_param.filter_pol   = BLE_GAP_ADV_ALLOW_SCAN_ANY_CON_ANY;

    error_code = ble_gap_adv_param_set(COMMON_SLAVE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_gap_adv_param);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(COMMON_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_slave_adv_data_set, sizeof(s_slave_adv_data_set));
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(COMMON_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_SCAN_RSP, s_slave_adv_rsp_data_set, sizeof(s_slave_adv_rsp_data_set));
    APP_ERROR_CHECK(error_code);

    s_gap_adv_time_param.duration    = 0;
    s_gap_adv_time_param.max_adv_evt = 0;

    error_code = ble_gap_l2cap_params_set(s_max_mtu_defualt, s_max_mps_default, s_max_nb_lecb_default);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_data_length_set(s_max_tx_octel_default, s_max_tx_time_default);
    APP_ERROR_CHECK(error_code);

    ble_gap_pref_phy_set(BLE_GAP_PHY_ANY, BLE_GAP_PHY_ANY);
}


static void slave_adv_start(void)
{
    sdk_err_t error_code;
    uint8_t* addr = multi_slave_get_addr(COMMON_SLAVE_ADV_INDEX);
    ble_gap_bdaddr_t adv_addr = 
    {
        .addr_type = BLE_GAP_ADDR_TYPE_RANDOM_STATIC,
    };
    memcpy(adv_addr.gap_addr.addr, addr, 6);
    adv_addr.gap_addr.addr[5] |= 0xC0;

    error_code = ble_gap_addr_set(&adv_addr);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_start(COMMON_SLAVE_ADV_INDEX, &s_gap_adv_time_param);
    APP_ERROR_CHECK(error_code);
}

static void slave_hide_services(uint8_t conn_idx)
{
    ble_gatts_service_hide_set(conn_idx, gus_service_start_handle_get());
}



/*------------------------------------------------------------------------------*/

static app_timer_id_t    s_check_auth_timer_id;
static app_timer_id_t    s_led_blink_timer_id;
static bool auth = false;
static bool shor_open_lock = false;

extern void bsp_led_off(void);
static void slave_ble_evt_handler(const ble_evt_t *p_evt)
{
    switch(p_evt->evt_id)
    {
        case BLE_GAPC_EVT_DISCONNECTED:
            slave_adv_start();
            app_timer_stop(s_led_blink_timer_id);
            bsp_led_off();
            auth = false;
        //需要检查是否短期开锁，如果是，需要判断长期用户是否连上，没有需要闭锁
            if(shor_open_lock)
            {
                 if(multi_slave_get_state(HID_SLAVE_ADV_INDEX) != CONN_STATE)
                 {
                     lock_open(false);
                 }
            }
            break;
        case BLE_GAPC_EVT_CONNECTED:
            printf("connect device, start auth timer cout");
            app_timer_start(s_check_auth_timer_id, 5000, NULL);
        break;
        
        default:break;
    }
}

extern void cal_share_key(uint8_t *p_serve_key, uint8_t *secret_cli);
extern void curve_25519_gen_public_key(uint8_t *p_cli_to_srv_key);
extern void aes_decode(uint8_t *p_key, uint8_t *p_ecode, uint8_t ecode_len, uint8_t *p_out, uint16_t *p_out_len);
extern void hid_slave_adv_data_set(uint8_t *p_data);
extern void bsp_led_toggle(void);

uint8_t cli_to_srv[32];
uint8_t srv_to_cli[32];
uint8_t share_key[32];
uint8_t key_data[256];
uint16_t len_s;

uint32_t password = 123456;

static const uint8_t s_auth_info[128] = 
{
    185,98,214,22,193,31,190,184,209,136,116,6,253,198,26,86,134,34,62,47,41,224,225,57,172,189,205,208,52,
    133,43,200,5,106,114,67,145,197,182,123,242,118,70,246,45,165,37,221,219,105,157,35,229,20,48,235,104,
    102,17,36,156,4,68,175,250,59,227,107,126,18,32,249,103,135,84,146,245,244,138,96,132,212,83,228,170,174,
    150,113,21,3,94,137,239,159,91,93,87,166,237,1,186,100,202,196,131,233,192,220,216,44,7,69,27,199,252,167,
    161,194,9,181,236,97,11,217,108,147,28,64
};

static const uint8_t SHORT_USERAUTH[] = {49,119,115,173,215,177,8,112,101,219,170,222,105,195,41,4};

#if 0
static const uint8_t LONG_USER_AUTH[] = {119,30,117,187,149,182,105,189,103,144,72,210,92,55,229,143,};

static const uint8_t s_long_auth[] = 
{
    119,30,117,187,149,182,105,189,103,144,72,210,92,55,229,143,
};

static const uint8_t s_short_auth[] = 
{
    49,119,115,173,215,177,8,112,101,219,170,222,105,195,41,4
};
#endif

static void slave_service_process_event(gus_evt_t *p_evt)
{
    uint8_t conn_idx = multi_slave_get_conn_idx(COMMON_SLAVE_ADV_INDEX);
    uint8_t send_data[34];
    switch (p_evt->evt_type)
    {
        case GUS_EVT_RX_DATA_RECEIVED:
            if(p_evt->p_data[0] == 0x01 && p_evt->p_data[1] == 0x02)//共享秘钥交换
            {
                memcpy(srv_to_cli, &p_evt->p_data[2], 32);
                curve_25519_gen_public_key(cli_to_srv);
                send_data[0] =0x01;
                send_data[1] = 0x02;
                memcpy(&send_data[2], cli_to_srv, 32);
                gus_tx_data_send(conn_idx, send_data, 34);
                cal_share_key(srv_to_cli, share_key);
                for(uint8_t i=0; i<32; i++)
                {
                    printf("%x",share_key[i]);
                }
            }
            else if(p_evt->p_data[0] == 0x03 && p_evt->p_data[1] == 0x04)//进行身份认证
            {
                aes_decode(share_key, &p_evt->p_data[2], p_evt->length-2, key_data, &len_s);
                uint8_t len = key_data[0];
                
                if(len == 128)
                {
                    if(memcmp(&key_data[1], s_auth_info, 128) == 0)
                    {
                        auth = true;
                        app_timer_start(s_led_blink_timer_id, 200, NULL);
                    }
                }
                app_timer_stop(s_check_auth_timer_id);
                if(auth)
                {
                    send_data[0] =0x03;
                    send_data[1] = 0x04;
                    gus_tx_data_send(conn_idx, send_data, 2);
                    printf("stop check timer, auth successful");
                }
                else
                {
                    multi_slave_disconnect(COMMON_SLAVE_ADV_INDEX);
                    printf("auth fail, disconnect device");
                }
            }
            else if(p_evt->p_data[0] == 0x05 && p_evt->p_data[1] == 0x06)//第一次绑定要开启HID广播
            {
                send_data[0] =0x05;
                send_data[1] = 0x06;
                gus_tx_data_send(conn_idx, send_data, 2);
                printf("start ADV");
                common_slave_adv_data_set(&p_evt->p_data[2]);
                hid_slave_adv_data_set(&p_evt->p_data[2]);
                multi_slave_start_adv(HID_SLAVE_ADV_INDEX);//开启HID广播
            }
            else if(p_evt->p_data[0] == 0x07 && p_evt->p_data[1] == 0x08)//接收绑定密码
            {
                //解密获取配对码
                aes_decode(share_key, &p_evt->p_data[2], p_evt->length-2, key_data, &len_s);
                uint8_t len = key_data[0];
                if(len == 6)
                {
                    password = key_data[1]*100000 + key_data[2]*10000 + key_data[3]*1000 + key_data[4]*100 + key_data[5]*10 + key_data[6];
                    send_data[0] =0x07;
                    send_data[1] = 0x08;
                    gus_tx_data_send(conn_idx, send_data, 2);
                    printf("receive password %d", password);  
                }
                else
                {
                    printf("receive password error");
                }
            }
            else if(p_evt->p_data[0] == 0x09 && p_evt->p_data[1] == 0x0a)//开锁信息认证
            {
                aes_decode(get_share_key(), &p_evt->p_data[2], p_evt->length-2, key_data, &len_s);
                if(key_data[0] == 0x02 && key_data[1] == 0x02)//短期秘钥
                {
                    char user_name[6] = {0};
                    for(uint8_t i=0; i<6; i++)
                    {
                        user_name[i] = key_data[2+i];
                    }
                    send_data[0] =0x0b;
                    send_data[1] = 0x0c;
                    send_data[2] = 0x00;
                    if((memcmp(user_name, get_user_id(), 6) == 0) && (memcmp(&key_data[8], SHORT_USERAUTH, 10) == 0))
                    {
                        printf("verify ok, open\r\n");
                        shor_open_lock = true;
                        lock_open(true);//需要开启RSSI 检测
                        send_data[2] = 0x01;
                    }
                    gus_tx_data_send(conn_idx, send_data, 3);
                }
                else if(key_data[0] == 0x01 && key_data[1] == 0x01)//长期用户绑定
                {
                    //需要判断是否已被连接，如果连接需要提示其他用户关闭手机蓝牙，断开连接
                    //
                    printf("long term user check\r\n");
                    send_data[0] =0x0d;
                    send_data[1] = 0x0e;
                    send_data[2] = 0x00;
                    if(bond_info_check() == false)//已经绑定了最大个数，需要删除
                    {
                        send_data[2] = 0x01;
                        printf("bond_info_check\r\n");
                    }
                    else
                    {
                        if(multi_slave_get_state(HID_SLAVE_ADV_INDEX) == CONN_STATE)//处于连接状态
                        {
                            send_data[2] = 0x02;
                            printf("multi_slave_get_state\r\n");
                        }
                    }
                    gus_tx_data_send(conn_idx, send_data, 3);
                }
            }
       
            break;
        
        default:
            break;
    }
}

static void check_auth_timeout_handler(void* p_arg)
{
    multi_slave_disconnect(COMMON_SLAVE_ADV_INDEX);
    printf("disconnect device");
}

static void led_blink_timeout_handler(void* p_arg)
{
    bsp_led_toggle();
}

static void slave_services_init(void)
{
    sdk_err_t   error_code;
    gus_init_t gus_init;

    gus_init.evt_handler = slave_service_process_event;

    error_code = gus_service_init(&gus_init);
    APP_ERROR_CHECK(error_code);
    
    error_code = app_timer_create(&s_check_auth_timer_id, ATIMER_ONE_SHOT, check_auth_timeout_handler);
    APP_ERROR_CHECK(error_code);
    error_code = app_timer_create(&s_led_blink_timer_id, ATIMER_REPEAT, led_blink_timeout_handler);
    APP_ERROR_CHECK(error_code);
}

#include "app_key.h"
#include "board_SK.h"
static uint8_t s_contiue_count = 0;
static bool s_reset_flag = false;

void app_key_evt_handler(uint8_t key_id, app_key_click_type_t key_click_type)
{
    printf("click\r\n");
    if(key_id == BSP_KEY_OK_ID && key_click_type == APP_KEY_LONG_CLICK && auth)
    {
        uint8_t conn_idx = multi_slave_get_conn_idx(COMMON_SLAVE_ADV_INDEX);
        uint8_t send_data[34];
        send_data[0] =0x09;
        send_data[1] = 0x0a;
        gus_tx_data_send(conn_idx, send_data, 2);
    }
    else if(key_id == BSP_KEY_LEFT_ID)
    {
        if(key_click_type == APP_KEY_CONTINUE_CLICK)
        {
            s_contiue_count ++;
            
            if(s_contiue_count >= 20 && s_reset_flag == false)
            {
                s_reset_flag = true;
                printf("reset device \r\n");
                clear_nvds_info();
                delay_ms(1000);
                hal_nvic_system_reset();
            }
        }
        else if(key_click_type == APP_KEY_CONTINUE_RELEASE)
        {
            s_contiue_count = 0;
            s_reset_flag = false;
        }
    }
    
}

