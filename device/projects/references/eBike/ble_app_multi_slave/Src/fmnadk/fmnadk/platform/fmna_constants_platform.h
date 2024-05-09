

#ifndef fmna_constants_platform_h
#define fmna_constants_platform_h

#include "gr_includes.h"

#include "ble_prf_types.h"
#include "ble_prf_utils.h"

#include "app_timer.h"

#include "fmna_application.h"

#include "fmna_adv_platform.h"
#include "fmna_log_platform.h"
#include "fmna_battery_platform.h"
#include "fmna_connection_platform.h"
#include "fmna_gatt_fmns_platform.h"
#include "fmna_malloc_platform.h"
#include "fmna_motion_detection_platform.h"
#include "fmna_nfc_platform.h"
#include "fmna_peer_manager_platform.h"
#include "fmna_queue_platform.h"
#include "fmna_sound_platform.h"
#include "fmna_storage_platform.h"
#include "fmna_timer_platform.h"



extern fmna_startup_info_cfg_t g_fmna_info_cfg;
extern fmna_uarp_cfg_t         g_fmna_uarp_cfg;

extern bool     g_is_enable_debug;
extern bool     g_is_enable_nfc;
extern uint16_t g_accessory_pid;

#define FMNA_BLE_ADV_INDEX                      0
#define FMNA_BLE_DATA_LENGTH                    251
#define FMNA_BLE_MTU_SIZE                       247
#define FMNA_BLE_GATT_HEADER_LEN                3
#define FMNA_BLE_GATT_PAYLOAD_LEN(mtu)          ((mtu) - FMNA_BLE_GATT_HEADER_LEN) 

#define FMNA_BLE_MAX_SUPPORTED_CONNECTIONS      2
#define FMNA_BLE_CONN_HANDLE_INVALID            BLE_GAP_INVALID_CONN_INDEX
#define FMNA_BLE_CONN_HANDLE_ALL                0xfe

#define FMNA_BLE_SEC_LEVEL                      BLE_SEC_MODE1_LEVEL2
#define FMNA_BLE_SEC_IO_CAP                     BLE_SEC_IO_NO_INPUT_NO_OUTPUT
#define FMNA_BLE_SEC_OOB                        false
#define FMNA_BLE_SEC_AUTH                       (BLE_SEC_AUTH_BOND)
#define FMNA_BLE_SEC_KEY_SIZE                   16
#define FMNA_BLE_SEC_IKEY_DIST                  BLE_SEC_KDIST_ALL
#define FMNA_BLE_SEC_RKEY_DIST                  BLE_SEC_KDIST_ALL
#define FMNA_BLE_PRIVACY_RENEW_DURATION         150

#define FMNA_STORAGE_FLASH_SECTOR_SIZE          0x1000




#endif /* fmna_constants_platform_h */
