#include "fmna_adv_platform.h"
#include "fmna_log_platform.h" 
#include "fmna_adv.h"
#include "fmna_connection.h"
#include "fmna_util.h"
#include "utility.h"



#define HI_U16(a) (uint8_t)(((uint16_t)(a) >> 8) & 0xFF)
#define LO_U16(a) (uint8_t)((uint16_t)(a) & 0xFF)

#define PAIRING_ADV     1
#define NEARBY_ADV      2
#define SEPARATED_ADV   3

#define FMNA_PAIRING_ADV_FAST_INTERVAL          0x30          /**< Fast advertising interval 30ms (in units of 0.625 ms.) */
#define FMNA_PAIRING_ADV_FAST_DURATION          300           /**< The advertising duration of fast advertising 3s (in units of 10 milliseconds.) */
#define FMNA_PAIRING_ADV_SLOW_INTERVAL          0x30          /**< Slow advertising interval 30ms (in units of 0.625 ms.) */
#define FMNA_PAIRING_ADV_SLOW_DURATION          0             /**< The advertising duration of slow advertising forever (in units of 10 milliseconds.) */

#define FMNA_SEPARATED_ADV_FAST_INTERVAL        0x30          /**< Fast advertising interval 30ms (in units of 0.625 ms.) */
#define FMNA_SEPARATED_ADV_FAST_DURATION        12000         /**< The advertising duration of fast advertising 2 minutes (in units of 10 milliseconds.) */
#define FMNA_SEPARATED_ADV_SLOW_INTERVAL        0xC80         /**< Slow advertising interval 2 seconds (in units of 0.625 ms.) */
#define FMNA_SEPARATED_ADV_SLOW_DURATION        0             /**< The advertising duration of slow advertising forever (in units of 10 milliseconds.) */

#define FMNA_NEARBY_ADV_FAST_INTERVAL           0x30          /**< Fast advertising interval 30ms (in units of 0.625 ms.) */
#define FMNA_NEARBY_ADV_FAST_DURATION           300           /**< The advertising duration of fast advertising 3s (in units of 10 milliseconds.) */
#define FMNA_NEARBY_ADV_SLOW_INTERVAL           0xC80         /**< Slow advertising interval 2 seconds (in units of 0.625 ms.) */
#define FMNA_NEARBY_ADV_SLOW_DURATION           0             /**< The advertising duration of slow advertising forever (in units of 10 milliseconds.) */

uint16_t fmna_separated_adv_fast_intv        = FMNA_SEPARATED_ADV_FAST_INTERVAL;
uint16_t fmna_separated_adv_fast_duration    = FMNA_SEPARATED_ADV_FAST_DURATION;
uint16_t fmna_separated_adv_slow_intv        = FMNA_SEPARATED_ADV_SLOW_INTERVAL;
uint16_t fmna_separated_adv_slow_duration    = FMNA_SEPARATED_ADV_SLOW_DURATION;

uint16_t fmna_nearby_adv_fast_intv           = FMNA_NEARBY_ADV_FAST_INTERVAL;
uint16_t fmna_nearby_adv_fast_duration       = FMNA_NEARBY_ADV_FAST_DURATION;
uint16_t fmna_nearby_adv_slow_intv           = FMNA_NEARBY_ADV_SLOW_INTERVAL;
uint16_t fmna_nearby_adv_slow_duration       = FMNA_NEARBY_ADV_SLOW_DURATION;

uint16_t fmna_pairing_adv_fast_intv          = FMNA_PAIRING_ADV_FAST_INTERVAL;
uint16_t fmna_pairing_adv_fast_duration      = FMNA_PAIRING_ADV_FAST_DURATION;
uint16_t fmna_pairing_adv_slow_intv          = FMNA_PAIRING_ADV_SLOW_INTERVAL;
uint16_t fmna_pairing_adv_slow_duration      = FMNA_PAIRING_ADV_SLOW_DURATION;

static uint8_t  s_pairing_adv_data[31];
static uint8_t  s_pairing_adv_data_len;

static uint8_t  s_nearby_adv_data[31];
static uint8_t  s_nearby_adv_data_len;

static uint8_t  s_separated_adv_data[31];
static uint8_t  s_separated_adv_data_len;

static ble_gap_adv_time_param_t s_fmna_adv_time_param;
static ble_gap_adv_param_t      s_fmna_adv_param;
static uint8_t                  s_fmna_adv_type;
static bool                     s_is_fmna_fast_adv;

bool             g_fmna_active_enable = true;
ble_gap_bdaddr_t g_fmna_adv_addr;
int              g_fmna_adv_tx_power = 4;

static uint8_t  s_is_adv_exc_cplt;


void fmna_adv_platform_start_handler(uint8_t inst_idx, uint8_t status)
{
    FMNA_ERROR_CHECK(status);
}

void fmna_adv_platform_stop_handler(uint8_t inst_idx, uint8_t status, uint8_t reason)
{
    FMNA_ERROR_CHECK(status);

    if (BLE_GAP_STOPPED_REASON_TIMEOUT == reason && BLE_SUCCESS == status)
    {
        if (s_is_fmna_fast_adv)
        {
            fmna_adv_platform_start_slow_adv();
        }
        else
        {
            FMNA_LOG_INFO("No advertising, enter idle state");
        }
    }
    else if (BLE_GAP_STOPPED_REASON_ON_USER == reason && BLE_SUCCESS == status)
    {
        s_is_adv_exc_cplt = 1;
    }
}

void fmna_adv_platform_get_default_bt_addr(uint8_t default_bt_addr[FMNA_BLE_MAC_ADDR_BLEN])
{
    // Read hardcoded address from factory register
    fmna_ret_code_t ret_code;
    ble_gap_bdaddr_t dev_addr;

    ret_code = ble_gap_addr_get(&dev_addr);
    FMNA_ERROR_CHECK(ret_code);

    memcpy(default_bt_addr, dev_addr.gap_addr.addr, FMNA_BLE_MAC_ADDR_BLEN);
    reverse_array(default_bt_addr, 0, FMNA_BLE_MAC_ADDR_BLEN-1);
}

void fmna_adv_platform_set_random_static_bt_addr(uint8_t new_bt_mac[FMNA_BLE_MAC_ADDR_BLEN])
{
    fmna_ret_code_t     ret_code;

    memcpy(g_fmna_adv_addr.gap_addr.addr, new_bt_mac, FMNA_BLE_MAC_ADDR_BLEN);

    // Print the current public key 6 bytes Bluetooth Address
    FMNA_LOG_INFO("Current fmna address: %02x:%02x:%02x:%02x:%02x:%02x", g_fmna_adv_addr.gap_addr.addr[0],
                                                                         g_fmna_adv_addr.gap_addr.addr[1],
                                                                         g_fmna_adv_addr.gap_addr.addr[2],
                                                                         g_fmna_adv_addr.gap_addr.addr[3],
                                                                         g_fmna_adv_addr.gap_addr.addr[4],
                                                                         g_fmna_adv_addr.gap_addr.addr[5]);

    reverse_array(g_fmna_adv_addr.gap_addr.addr, 0, FMNA_BLE_MAC_ADDR_BLEN-1);

    g_fmna_adv_addr.addr_type = BLE_GAP_ADDR_TYPE_RANDOM_STATIC;

    ret_code = ble_gap_addr_set(&g_fmna_adv_addr);
    FMNA_ERROR_CHECK(ret_code);
}



void fmna_adv_platform_start_fast_adv(void)
{
    fmna_ret_code_t ret_code;

    if (PAIRING_ADV == s_fmna_adv_type)
    {
        FMNA_LOG_INFO("Pairing fast advertising start(%0.2fms, %dms)", fmna_pairing_adv_fast_intv * 0.625, fmna_pairing_adv_fast_duration *10);
        s_fmna_adv_param.adv_intv_max  = fmna_pairing_adv_fast_intv;
        s_fmna_adv_param.adv_intv_min  = fmna_pairing_adv_fast_intv;
        s_fmna_adv_time_param.duration = fmna_pairing_adv_fast_duration;
    }
    else if (NEARBY_ADV == s_fmna_adv_type)
    {
        FMNA_LOG_INFO("Nearby fast advertising start(%0.2fms, %dms)", fmna_nearby_adv_fast_intv * 0.625, fmna_nearby_adv_fast_duration *10);
        s_fmna_adv_param.adv_intv_max  = fmna_nearby_adv_fast_intv;
        s_fmna_adv_param.adv_intv_min  = fmna_nearby_adv_fast_intv;
        s_fmna_adv_time_param.duration = fmna_nearby_adv_fast_duration;
    }
    else if (SEPARATED_ADV == s_fmna_adv_type)
    {
        FMNA_LOG_INFO("Separated fast advertising start(%0.2fms, %dms)", fmna_separated_adv_fast_intv * 0.625, fmna_separated_adv_fast_duration *10);
        s_fmna_adv_param.adv_intv_max  = fmna_separated_adv_fast_intv;
        s_fmna_adv_param.adv_intv_min  = fmna_separated_adv_fast_intv;
        s_fmna_adv_time_param.duration = fmna_separated_adv_fast_duration;
    }
    else
    {
        return;
    }

    if (!g_fmna_active_enable)
    {
        FMNA_LOG_WARNING("Now FMNA active is disable");
        return;
    }

    s_is_fmna_fast_adv = 1;

    ret_code = ble_gap_adv_param_set(FMNA_BLE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_fmna_adv_param);
    FMNA_ERROR_CHECK(ret_code);

    ret_code = ble_gap_adv_start(FMNA_BLE_ADV_INDEX, &s_fmna_adv_time_param);
    FMNA_ERROR_CHECK(ret_code);
}

void fmna_adv_platform_start_slow_adv(void)
{
    fmna_ret_code_t ret_code;

    if (PAIRING_ADV == s_fmna_adv_type)
    {
        FMNA_LOG_INFO("Pairing slow advertising start(%0.2fms, %dms)", fmna_pairing_adv_slow_intv * 0.625, fmna_pairing_adv_slow_duration *10);
        s_fmna_adv_param.adv_intv_max  = fmna_pairing_adv_slow_intv;
        s_fmna_adv_param.adv_intv_min  = fmna_pairing_adv_slow_intv;
        s_fmna_adv_time_param.duration = fmna_pairing_adv_slow_duration;
    }
    else if (NEARBY_ADV == s_fmna_adv_type)
    {
        FMNA_LOG_INFO("Nearby slow advertising start(%0.2fms, %dms)", fmna_nearby_adv_slow_intv * 0.625, fmna_nearby_adv_slow_duration *10);
        s_fmna_adv_param.adv_intv_max  = fmna_nearby_adv_slow_intv;
        s_fmna_adv_param.adv_intv_min  = fmna_nearby_adv_slow_intv;
        s_fmna_adv_time_param.duration = fmna_nearby_adv_slow_duration;
    }
    else if (SEPARATED_ADV == s_fmna_adv_type)
    {
        FMNA_LOG_INFO("Separated slow advertising start(%0.2fms, %dms)", fmna_separated_adv_slow_intv * 0.625, fmna_separated_adv_slow_duration *10);
        s_fmna_adv_param.adv_intv_max  = fmna_separated_adv_slow_intv;
        s_fmna_adv_param.adv_intv_min  = fmna_separated_adv_slow_intv;
        s_fmna_adv_time_param.duration = fmna_separated_adv_slow_duration;
    }
    else
    {
        return;
    }

    s_is_fmna_fast_adv = 0;

    if (!g_fmna_active_enable)
    {
        FMNA_LOG_WARNING("Now FMNA active is disable");
        return;
    }

    ret_code = ble_gap_adv_param_set(FMNA_BLE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_fmna_adv_param);
    FMNA_ERROR_CHECK(ret_code);

    ret_code = ble_gap_adv_start(FMNA_BLE_ADV_INDEX, &s_fmna_adv_time_param);
    FMNA_ERROR_CHECK(ret_code);
}

void fmna_adv_platform_stop_adv(void)
{
    extern void ble_task_force_schedule(void);
    s_is_adv_exc_cplt = 0;

    if (!ble_gap_adv_stop(FMNA_BLE_ADV_INDEX))
    {
        while(!s_is_adv_exc_cplt)
        {
            ble_task_force_schedule();
        }
    }
}

void fmna_adv_platform_init_pairing(uint8_t *pairing_adv_service_data, size_t pairing_adv_service_data_size)
{
    fmna_ret_code_t ret_code;

    s_pairing_adv_data_len = 0;

    s_fmna_adv_param.adv_intv_max  = fmna_pairing_adv_slow_intv;
    s_fmna_adv_param.adv_intv_min  = fmna_pairing_adv_slow_intv;
    s_fmna_adv_param.adv_mode      = BLE_GAP_ADV_TYPE_ADV_IND;
    s_fmna_adv_param.chnl_map      = BLE_GAP_ADV_CHANNEL_37_38_39;
    s_fmna_adv_param.disc_mode     = BLE_GAP_DISC_MODE_NON_DISCOVERABLE;
    s_fmna_adv_param.filter_pol    = BLE_GAP_ADV_ALLOW_SCAN_ANY_CON_ANY;
    s_fmna_adv_param.max_tx_pwr    = g_fmna_adv_tx_power;

    ret_code = ble_gap_adv_param_set(FMNA_BLE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_fmna_adv_param);
    FMNA_ERROR_CHECK(ret_code);

    // gap adv data
    s_pairing_adv_data[s_pairing_adv_data_len++] = pairing_adv_service_data_size + 3;
    s_pairing_adv_data[s_pairing_adv_data_len++] = BLE_GAP_AD_TYPE_SERVICE_16_BIT_DATA;
    s_pairing_adv_data[s_pairing_adv_data_len++] = LO_U16(FINDMY_UUID_SERVICE);
    s_pairing_adv_data[s_pairing_adv_data_len++] = HI_U16(FINDMY_UUID_SERVICE);
    memcpy(s_pairing_adv_data + s_pairing_adv_data_len, pairing_adv_service_data, pairing_adv_service_data_size);
    s_pairing_adv_data_len += pairing_adv_service_data_size;

    ret_code = ble_gap_adv_data_set(FMNA_BLE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_pairing_adv_data, s_pairing_adv_data_len);
    FMNA_ERROR_CHECK(ret_code);

    s_fmna_adv_type = PAIRING_ADV;
}

void fmna_adv_platform_init_nearby(uint8_t *nearby_adv_manuf_data, size_t nearby_adv_manuf_data_size)
{
    fmna_ret_code_t ret_code;

    s_nearby_adv_data_len = 0;

    s_fmna_adv_param.adv_intv_max  = fmna_nearby_adv_slow_intv;
    s_fmna_adv_param.adv_intv_min  = fmna_nearby_adv_slow_intv;
    s_fmna_adv_param.adv_mode      = BLE_GAP_ADV_TYPE_ADV_IND;
    s_fmna_adv_param.chnl_map      = BLE_GAP_ADV_CHANNEL_37_38_39;
    s_fmna_adv_param.disc_mode     = BLE_GAP_DISC_MODE_NON_DISCOVERABLE;
    s_fmna_adv_param.filter_pol    = BLE_GAP_ADV_ALLOW_SCAN_ANY_CON_ANY;
    s_fmna_adv_param.max_tx_pwr    = g_fmna_adv_tx_power;

    ret_code = ble_gap_adv_param_set(FMNA_BLE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_fmna_adv_param);
    FMNA_ERROR_CHECK(ret_code);

    // gap adv data
    s_nearby_adv_data[s_nearby_adv_data_len++] = nearby_adv_manuf_data_size + 3;
    s_nearby_adv_data[s_nearby_adv_data_len++] = BLE_GAP_AD_TYPE_MANU_SPECIFIC_DATA;
    s_nearby_adv_data[s_nearby_adv_data_len++] = LO_U16(FMNA_COMPANY_IDENTIFIER);
    s_nearby_adv_data[s_nearby_adv_data_len++] = HI_U16(FMNA_COMPANY_IDENTIFIER);
    memcpy(s_nearby_adv_data + s_nearby_adv_data_len, nearby_adv_manuf_data, nearby_adv_manuf_data_size);
    s_nearby_adv_data_len += nearby_adv_manuf_data_size;

    ret_code = ble_gap_adv_data_set(FMNA_BLE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_nearby_adv_data, s_nearby_adv_data_len);
    FMNA_ERROR_CHECK(ret_code);

    s_fmna_adv_type = NEARBY_ADV;
}


void fmna_adv_platform_init_separated(uint8_t *separated_adv_manuf_data, size_t separated_adv_manuf_data_size)
{
    fmna_ret_code_t ret_code;

    s_separated_adv_data_len = 0;
    s_fmna_adv_param.adv_intv_max  = fmna_separated_adv_slow_intv;
    s_fmna_adv_param.adv_intv_min  = fmna_separated_adv_slow_intv;
    s_fmna_adv_param.adv_mode      = BLE_GAP_ADV_TYPE_ADV_IND;
    s_fmna_adv_param.chnl_map      = BLE_GAP_ADV_CHANNEL_37_38_39;
    s_fmna_adv_param.disc_mode     = BLE_GAP_DISC_MODE_NON_DISCOVERABLE;
    s_fmna_adv_param.filter_pol    = BLE_GAP_ADV_ALLOW_SCAN_ANY_CON_ANY;
    s_fmna_adv_param.max_tx_pwr    = g_fmna_adv_tx_power;

    ret_code = ble_gap_adv_param_set(FMNA_BLE_ADV_INDEX, BLE_GAP_OWN_ADDR_STATIC, &s_fmna_adv_param);
    FMNA_ERROR_CHECK(ret_code);

    // gap adv data
    s_separated_adv_data[s_separated_adv_data_len++] = separated_adv_manuf_data_size + 3;
    s_separated_adv_data[s_separated_adv_data_len++] = BLE_GAP_AD_TYPE_MANU_SPECIFIC_DATA;
    s_separated_adv_data[s_separated_adv_data_len++] = LO_U16(FMNA_COMPANY_IDENTIFIER);
    s_separated_adv_data[s_separated_adv_data_len++] = HI_U16(FMNA_COMPANY_IDENTIFIER);
    memcpy(s_separated_adv_data + s_separated_adv_data_len, separated_adv_manuf_data, separated_adv_manuf_data_size);
    s_separated_adv_data_len += separated_adv_manuf_data_size;

    ret_code = ble_gap_adv_data_set(FMNA_BLE_ADV_INDEX, BLE_GAP_ADV_DATA_TYPE_DATA, s_separated_adv_data, s_separated_adv_data_len);
    FMNA_ERROR_CHECK(ret_code);

    s_fmna_adv_type = SEPARATED_ADV;
}


void fmna_application_adv_intv_dur_customize(fmna_adv_intv_dur_customize_t *p_intv_dur_customize)
{
    if (NULL == p_intv_dur_customize)
    {
        return;
    }

    fmna_separated_adv_fast_intv        = p_intv_dur_customize->separated_adv_fast_intv;
    fmna_separated_adv_fast_duration    = p_intv_dur_customize->separated_adv_fast_duration;
    fmna_separated_adv_slow_intv        = p_intv_dur_customize->separated_adv_slow_intv;
    fmna_separated_adv_slow_duration    = p_intv_dur_customize->separated_adv_slow_duration;

    fmna_nearby_adv_fast_intv           = p_intv_dur_customize->nearby_adv_fast_intv;
    fmna_nearby_adv_fast_duration       = p_intv_dur_customize->nearby_adv_fast_duration;
    fmna_nearby_adv_slow_intv           = p_intv_dur_customize->nearby_adv_slow_intv;
    fmna_nearby_adv_slow_duration       = p_intv_dur_customize->nearby_adv_slow_duration;

    fmna_pairing_adv_fast_intv          = p_intv_dur_customize->pairing_adv_fast_intv;
    fmna_pairing_adv_fast_duration      = p_intv_dur_customize->pairing_adv_fast_duration;
    fmna_pairing_adv_slow_intv          = p_intv_dur_customize->pairing_adv_slow_intv;
    fmna_pairing_adv_slow_duration      = p_intv_dur_customize->pairing_adv_slow_duration;
}


fmna_ret_code_t fmna_application_pair_adv_restart(uint16_t duration)
{
    if (fmna_connection_is_fmna_paired())
    {
        return FMNA_ERROR_INVALID_STATE;
    }

    fmna_pairing_adv_slow_duration = duration;

    fmna_adv_init_pairing();
    fmna_adv_start_fast_adv();

    return FMNA_SUCCESS;
}
