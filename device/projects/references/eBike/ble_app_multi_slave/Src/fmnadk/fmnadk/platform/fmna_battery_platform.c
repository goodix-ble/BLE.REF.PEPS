
#include "fmna_battery_platform.h"

#include "fmna_nfc.h"


static fmna_bat_level_get_t s_bat_level_get;


fmna_ret_code_t fmna_battery_platform_init(fmna_bat_level_get_t bat_level_get)
{
    s_bat_level_get = bat_level_get;

    return FMNA_SUCCESS;
}

fmna_bat_state_level_t fmna_battery_platform_get_battery_level(void)
{
    fmna_bat_state_level_t bat_state_level = FMNA_BAT_FULL;

    if (s_bat_level_get)
    {
        bat_state_level = s_bat_level_get();
    }
    else
    {
        FMNA_LOG_INFO("No [BATTERY DETECT] module, [ACTION]: get battery level value");
    }

    #if FMNA_NFC_ENABLE
    // Set the NFC URL battery status key.
    fmna_nfc_set_url_key(URL_KEY_BATT_STATUS, &bat_state_level);
    #endif

    return bat_state_level;
}

