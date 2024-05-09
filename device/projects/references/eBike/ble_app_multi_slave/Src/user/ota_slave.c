#include "multi_slave_config.h"
#include "otas.h"
#include "app_error.h"

static const uint8_t s_adv_fast_min_interval = 32;                /**< The fast advertising min interval (in units of 0.625 ms). */
static const uint8_t s_adv_fast_max_interval = 48;                /**< The fast advertising max interval (in units of 0.625 ms). */
static const uint8_t s_max_mtu_defualt = 247;                     /**< Defualt length of maximal MTU acceptable for device. */
static const uint8_t s_max_mps_default = 247;                     /**< Defualt length of maximal packet size acceptable for device. */
static const uint8_t s_max_nb_lecb_default = 10;                  /**< Defualt length of maximal number of LE Credit based connection. */
static const uint8_t s_max_tx_octel_default = 251;                /**< Default maximum transmitted number of payload octets. */
static const uint16_t s_max_tx_time_default = 2120;               /**< Defualt maximum packet transmission time. */

static const uint8_t s_slave_adv_data_set[] =                     /**< Advertising data. */
{
     // Complete Name
    0x04,
    BLE_GAP_AD_TYPE_COMPLETE_NAME,
    'O', 'T', 'A',
    
    0x11, // Length of this data
    BLE_GAP_AD_TYPE_COMPLETE_LIST_128_BIT_UUID,
    BLE_UUID_OTA_SERVICE,
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

const multi_dev_func_t ota_slave_func = 
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

    error_code = ble_gap_adv_param_set(OTA_SLAVE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_gap_adv_param);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(OTA_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_slave_adv_data_set, sizeof(s_slave_adv_data_set));
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(OTA_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_SCAN_RSP, s_slave_adv_rsp_data_set, sizeof(s_slave_adv_rsp_data_set));
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
    ble_gap_bdaddr_t adv_addr = 
    {
        .addr_type = BLE_GAP_ADDR_TYPE_RANDOM_STATIC,
        .gap_addr.addr = OTA_SLAVE_ADDR,
    };
    error_code = ble_gap_addr_set(&adv_addr);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_start(OTA_SLAVE_ADV_INDEX, &s_gap_adv_time_param);
    APP_ERROR_CHECK(error_code);
}

static void slave_hide_services(uint8_t conn_idx)
{
    ble_gatts_service_hide_set(conn_idx, otas_service_start_handle_get());
}

static void slave_ble_evt_handler(const ble_evt_t *p_evt)
{
    switch(p_evt->evt_id)
    {
        case BLE_GAPC_EVT_DISCONNECTED:
            slave_adv_start();
            break;
        
        default:break;
    }
}


static void slave_service_process_event(otas_evt_t *p_evt)
{
    switch (p_evt->evt_type)
    {
        
        default:
            break;
    }
}

static void slave_services_init(void)
{
    sdk_err_t   error_code;
    otas_init_t otas_init;

    otas_init.evt_handler = slave_service_process_event;

    error_code = otas_service_init(&otas_init);
    APP_ERROR_CHECK(error_code);
}

