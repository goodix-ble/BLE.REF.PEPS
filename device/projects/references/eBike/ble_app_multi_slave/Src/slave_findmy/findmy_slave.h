


#ifndef __FINDMY_SLAVE_H__
#define __FINDMY_SLAVE_H__

#include "fmna_application.h"

#include "findmy_platform_port.h"

#include "fmna_application.h"
#include "fmna_application_config.h"

#if FMNA_CUSTOM_BOARD_ENABLE
#include "custom_log_uart.h"
#include "custom_button.h"
#include "custom_led.h"
#include "custom_nfc.h"
#include "custom_speaker.h"
#include "custom_battery_det.h"
#include "custom_motion_det.h"
#endif


void findmy_slave_init(void);


#endif









