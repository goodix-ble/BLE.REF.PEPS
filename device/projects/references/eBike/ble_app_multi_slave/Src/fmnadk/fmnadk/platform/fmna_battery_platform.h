

#ifndef fmna_battery_platform_h
#define fmna_battery_platform_h

#include "fmna_application.h"

fmna_ret_code_t fmna_battery_platform_init(fmna_bat_level_get_t bat_level_get);
fmna_bat_state_level_t fmna_battery_platform_get_battery_level(void);

#endif /* fmna_battery_platform_h */
