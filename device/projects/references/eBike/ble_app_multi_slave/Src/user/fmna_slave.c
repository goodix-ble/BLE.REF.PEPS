#include "multi_slave_config.h"
#include "dis.h"
#include "hrs.h"
#include "bas.h"
#include "utility.h"
#include "app_error.h"
#include "multi_slave_manage.h"

static const uint8_t s_adv_fast_min_interval = 32;                /**< The fast advertising min interval (in units of 0.625 ms). */
static const uint8_t s_adv_fast_max_interval = 48;                /**< The fast advertising max interval (in units of 0.625 ms). */

static const uint8_t s_adv_data_set[] =                      /**< Advertising data. */
{
    // Complete Name
    0x0b,
    BLE_GAP_AD_TYPE_COMPLETE_NAME,
    'G', 'o', 'o', 'd', 'i', 'x', '_', 'H', 'R', 'M',

    // Device appearance
    0x03,
    BLE_GAP_AD_TYPE_APPEARANCE,
    LO_U16(BLE_APPEARANCE_GENERIC_HEART_RATE_SENSOR),
    HI_U16(BLE_APPEARANCE_GENERIC_HEART_RATE_SENSOR),

    // Device Services uuid
    0x07,
    BLE_GAP_AD_TYPE_COMPLETE_LIST_16_BIT_UUID,
    LO_U16(BLE_ATT_SVC_HEART_RATE),
    HI_U16(BLE_ATT_SVC_HEART_RATE),
    LO_U16(BLE_ATT_SVC_DEVICE_INFO),
    HI_U16(BLE_ATT_SVC_DEVICE_INFO),
    LO_U16(BLE_ATT_SVC_BATTERY_SERVICE),
    HI_U16(BLE_ATT_SVC_BATTERY_SERVICE),
};

static const uint8_t s_adv_rsp_data_set[] =
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

static ble_gap_adv_param_t      s_gap_adv_param;                 /**< Advertising parameters for legay advertising. */
static ble_gap_adv_time_param_t s_gap_adv_time_param;            /**< Advertising time parameter. */


static void slave_adv_params_init(void);
static void slave_adv_start(void);
static void slave_hide_services(uint8_t conn_idx);
static void slave_ble_evt_handler(const ble_evt_t *p_evt);
static void slave_services_init(void);

const multi_dev_func_t fmna_slave_func = 
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

    error_code = ble_gap_adv_param_set(FMNA_SLAVE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_gap_adv_param);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(FMNA_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_adv_data_set, sizeof(s_adv_data_set));
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_data_set(FMNA_SLAVE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_SCAN_RSP, s_adv_rsp_data_set, sizeof(s_adv_rsp_data_set));
    APP_ERROR_CHECK(error_code);

    s_gap_adv_time_param.duration    = 0;
    s_gap_adv_time_param.max_adv_evt = 0;

    ble_gap_pref_phy_set(BLE_GAP_PHY_ANY, BLE_GAP_PHY_ANY);
}


static void slave_adv_start(void)
{
    sdk_err_t error_code;
    uint8_t* addr = multi_slave_get_addr(FMNA_SLAVE_ADV_INDEX);
    ble_gap_bdaddr_t adv_addr = 
    {
        .addr_type = BLE_GAP_ADDR_TYPE_RANDOM_STATIC,
    };
    memcpy(adv_addr.gap_addr.addr, addr, 6);
    adv_addr.gap_addr.addr[5] |= 0xC0;

    error_code = ble_gap_addr_set(&adv_addr);
    APP_ERROR_CHECK(error_code);

    error_code = ble_gap_adv_start(FMNA_SLAVE_ADV_INDEX, &s_gap_adv_time_param);
    APP_ERROR_CHECK(error_code);
}

static void slave_hide_services(uint8_t conn_idx)
{
    ble_gatts_service_hide_set(conn_idx, bas_service_start_handle_get());
    ble_gatts_service_hide_set(conn_idx, hrs_service_start_handle_get());
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

static void slave_services_init(void)
{
    sdk_err_t  error_code;
    bas_init_t bas_env_init[1];
    hrs_init_t hrs_init;

    /*------------------------------------------------------------------*/
    bas_env_init[0].char_mask   = BAS_CHAR_MANDATORY | BAS_CHAR_LVL_NTF_SUP;
    bas_env_init[0].batt_lvl    = 0;
    bas_env_init[0].evt_handler = NULL;
    error_code = bas_service_init(bas_env_init, 1);
    APP_ERROR_CHECK(error_code);

    /*------------------------------------------------------------------*/
    hrs_init.sensor_loc                      = HRS_SENS_LOC_FINGER;
    hrs_init.char_mask                       = HRS_CHAR_MANDATORY |
                                               HRS_CHAR_BODY_SENSOR_LOC_SUP |
                                               HRS_CHAR_ENGY_EXP_SUP;
    hrs_init.evt_handler                     = NULL;
    hrs_init.is_sensor_contact_supported     = true;
    error_code = hrs_service_init(&hrs_init);
    APP_ERROR_CHECK(error_code);
}

