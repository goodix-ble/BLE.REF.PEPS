
/**
 *****************************************************************************************
 *
 * @file fmna_application.h
 *
 * @brief Find My Netwoek Accessory Application API.
 *
 *****************************************************************************************
 * @attention
  #####Copyright (c) 2019 GOODIX
  All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
  * Neither the name of GOODIX nor the names of its contributors may be used
    to endorse or promote products derived from this software without
    specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDERS AND CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************************
 */
#ifndef __FMNA_APPLICATION_H__
#define __FMNA_APPLICATION_H__


#include "gr_includes.h"

/**
 * @defgroup FMNA_APPLICATION_MACRO Defines
 * @{
 */
/**
* NVDS Tag 0x4011-0x4019  Reserved for FMNA Core
*/

#define FMNA_MODEL_NAME_LEN                         64       /**< Maximum length of module name string. */
#define FMNA_MANUFACTURER_NAME_LEN                  64       /**< Maximum length of manufacturer name string. */
#define FMNA_HW_REV_STR_LEN                         128      /**< Maximum length of hardware revision string. */
#define FMNA_SERIAL_NUMBER_LEN                      16       /**< Length of serial number. */
#define FMNA_PRODUCT_DATA_LEN                       8        /**< Maximum length of product data. */
#define FMNA_ACC_CATEGORY_LEN                       8        /**< Maximum length of accessory category data. */
#define FNMA_UARP_4CC_TAG_LEN                       4        /**< Length of uarp 4cc tag. */
#define FNMA_QUEUE_ITEM_SIZE                        10       /**< Size of queue item. */

#define FMNA_SUCCESS                                0x0000     /**< No error. */
#define FMNA_ERROR_INTERNAL                         0x0001     /**< Internal Error. */
#define FMNA_ERROR_INVALID_STATE                    0x0002     /**< Invalid state, operation disallowed in this state. */
#define FMNA_ERROR_INVALID_LENGTH                   0x0003     /**< Invalid Length. */
#define FMNA_ERROR_INVALID_DATA                     0x0004     /**< Invalid Data. */
#define FMNA_ERROR_INVALID_PARAM                    0x0005     /**< Invalid Data. */
#define FMNA_ERROR_NULL                             0x0006     /**< Null Pointer. */

#define FMNA_ERROR_INVALID_LOG_PARAM                0x0008     /**< Invalid log module param. */
#define FMNA_ERROR_INVALID_QUEUE_PARAM              0x0009     /**< Invalid queue module param. */
#define FMNA_ERROR_INVALID_STORAGE_PARAM            0x000a     /**< Invalid storage module param. */
#define FMNA_ERROR_INVALID_MALLOC_PARAM             0x000b     /**< Invalid memory malloc module param. */
#define FMNA_ERROR_INVALID_SPEAKER_PARAM            0x000c     /**< Invalid speaker module param. */
#define FMNA_ERROR_INVALID_MOTION_DET_PARAM         0x000d     /**< Invalid motion detect module param. */
#define FMNA_ERROR_INVALID_NFC_PARAM                0x000e     /**< Invalid NFC module param. */
#define FMNA_ERROR_INVALID_BATTERY_PARAM            0x000f     /**< Invalid battery module param. */
#define fmna_ret_code_t                             uint16_t   /**< FMNA error code type. */
/** @} */

/**
 * @defgroup FMNA_APPLICATION_ENUM Enumerations
 * @{
 */
/**@brief Battery level. */
typedef enum 
{
    FMNA_BAT_FULL = 0,
    FMNA_BAT_MEDIUM,
    FMNA_BAT_LOW,
    FMNA_BAT_CRITICALLY_LOW
} fmna_bat_state_level_t;

/**@brief Speaker On Type. */
typedef enum 
{
    FMNA_SPEAKER_ON_POWER_ON = 0,
    FMNA_SPEAKER_ON_PAIRED,
    FMNA_SPEAKER_ON_UNPAIR,
    FMNA_SPEAKER_ON_START_FOUND,
    FMNA_SPEAKER_ON_FACTORY_RESET
} fmna_speaker_on_type_t;

/**@brief Battery level. */
typedef enum 
{
    FMNA_UARP_DFU_FILE_INFO_RECV = 0,
    FMNA_UARP_DFU_FILE_PROGRAM,
    FMNA_UARP_DFU_INFO_UPDEATE,
    FMNA_UARP_ERROR_OCCUR,
} fmna_uarp_notify_type_t;
/** @} */

/**
 * @defgroup FMNA_APPLICATION_STRUCT Structures
 * @{
 */
/**@brief FMNA UARP Config Information. */
typedef struct
{
    fmna_uarp_notify_type_t notify_type;
    uint32_t                param;          /**< File Size for FMNA_UARP_DFU_FILE_INFO_RECV, Percentage Progress for FMNA_UARP_DFU_FILE_PROGRAM, Error path for FMNA_UARP_ERROR_OCCUR. */
} __attribute__((packed)) fmna_uarp_ntf_evt_t;
/** @} */

/**
 * @defgroup FMNA_APPLICATION_TYPEDEF Typedefs
 * @{
 */
/**@brief Memory malloc function type.*/
typedef void* (*fmna_malloc_t)(size_t size);

/**@brief Memory realloc function type.*/
typedef void* (*fmna_realloc_t)(void *ptr, size_t size);

/**@brief Memory free function type.*/
typedef void  (*fmna_free_t)(void *ptr);

/**@brief Storage flash init function type.*/
typedef bool (*fmna_storage_flash_init_t)(void);

/**@brief Storage flash read function type, retval is numbers of read.*/
typedef uint32_t (*fmna_storage_flash_read_t)(const uint32_t addr, uint8_t *buf, const uint32_t size);

/**@brief Storage flash write function type, retval is numbers of write.*/
typedef uint32_t (*fmna_storage_flash_write_t)(const uint32_t addr, const uint8_t *buf, const uint32_t size);

/**@brief Storage flash erase function type.*/
typedef bool (*fmna_storage_flash_erase_t)(const uint32_t addr, const uint32_t size);

/**@brief Queue send function type.*/
typedef bool (*fmna_queue_send_t)(void const *data, uint16_t size, bool in_isr);

/**@brief Queue receive function type.*/
typedef bool (*fmna_queue_receive_t)(void const *buffer, uint16_t size);

/**@brief Log output function type.*/
typedef void (*fmna_log_output_t)(uint8_t *p_data, uint16_t length);

/**@brief Speaker on function type.*/
typedef void (*fmna_speaker_on_t)(fmna_speaker_on_type_t on_type);

/**@brief Speaker off function type.*/
typedef void (*fmna_speaker_off_t)(void);

/**@brief Motion detect start/power on function type.*/
typedef void (*fmna_motion_detect_start_t)(void);

/**@brief Motion detect stop/power off function type.*/
typedef void (*fmna_motion_detect_stop_t)(void);

/**@brief Motion sensor motion state get function type.*/
typedef bool (*fmna_motion_is_detected_t)(void);

/**@brief Battery level get function type.*/
typedef fmna_bat_state_level_t (*fmna_bat_level_get_t)(void);

/**@brief NFC init function type.*/
typedef void (*fmna_nfc_init_t)(void);

/**@brief NFC info update function type.*/
typedef void (*fmna_nfc_info_update_t)(uint8_t *p_data, uint16_t length);

/**@brief UARP progress notification function type.*/
typedef void (*fmna_uarp_pro_notify_t)(fmna_uarp_ntf_evt_t *p_uarp_ntf_evt);
/** @} */

/**
 * @defgroup FMNA_APPLICATION_STRUCT Structures
 * @{
 */
/**@brief FMNA UARP Config Information. */
typedef struct
{
    uint8_t   _4cc_tag[FNMA_UARP_4CC_TAG_LEN];    /**< UARP 4cc TAG. */
    uint32_t  dfu_info_save_addr;                 /**< DFU infomation save address. */
    uint32_t  dfu_fw_save_addr;                   /**< DFU firmware save address. */
} __attribute__((packed)) fmna_uarp_cfg_t;

/**@brief FMNA Advertising Interval And Duration Customize Information. */
typedef struct
{
    uint16_t    pairing_adv_fast_intv;                              /**< Pairing fast advertiding interval. */
    uint16_t    pairing_adv_fast_duration;                          /**< Pairing fast advertiding duration. */
    uint16_t    pairing_adv_slow_intv;                              /**< Pairing slow advertiding interval. */
    uint16_t    pairing_adv_slow_duration;                          /**< Pairing slow advertiding interval. */

    uint16_t    nearby_adv_fast_intv;                               /**< Nearby fast advertiding interval. */
    uint16_t    nearby_adv_fast_duration;                           /**< Nearby fast advertiding duration. */
    uint16_t    nearby_adv_slow_intv;                               /**< Nearby slow advertiding interval. */
    uint16_t    nearby_adv_slow_duration;                           /**< Nearby slow advertiding interval. */

    uint16_t    separated_adv_fast_intv;                            /**< Separated fast advertiding interval. */
    uint16_t    separated_adv_fast_duration;                        /**< Separated fast advertiding duration. */
    uint16_t    separated_adv_slow_intv;                            /**< Separated slow advertiding interval. */
    uint16_t    separated_adv_slow_duration;                        /**< Separated slow advertiding interval. */

} __attribute__((packed)) fmna_adv_intv_dur_customize_t;

/**@brief FMNA Config Information. */
typedef struct
{
    char        model_name_str[FMNA_MODEL_NAME_LEN];                /**< Module name string. */
    char        manufaturer_name_str[FMNA_MANUFACTURER_NAME_LEN];   /**< Manufacturer name string. */
    char        hardware_rev_str[FMNA_HW_REV_STR_LEN];              /**< Hardware revision string. */
    uint8_t     serial_number[FMNA_SERIAL_NUMBER_LEN];              /**< Serial number. */
    uint8_t     product_data[FMNA_PRODUCT_DATA_LEN];                /**< Accessory product data. */
    uint8_t     accessory_category;                                 /**< Accessory category. */
    uint32_t    fw_rev_major_number;                                /**< Firmware revision major number. */
    uint32_t    fw_rev_minor_number;                                /**< Firmware revision minor number. */
    uint32_t    fw_rev_revision_number;                             /**< Firmware revision number. */
    uint32_t    software_auth_uuid_save_addr;                       /**< Software auth token ande uuid save address. */
} __attribute__((packed)) fmna_startup_info_cfg_t;

/**@brief FMNA Application Startup Callback Configuration Information. */
typedef struct
{
    fmna_log_output_t           log_output;             /**< Log output function. */
    fmna_queue_send_t           queue_send;             /**< Queue send function. */
    fmna_queue_receive_t        queue_receive;          /**< Queue receive function. */
    fmna_malloc_t               mem_malloc;             /**< Memory malloc function. */
    fmna_realloc_t              mem_realloc;            /**< Memory realloc function. */
    fmna_free_t                 mem_free;               /**< Memory free function. */
    fmna_storage_flash_init_t   flash_init;             /**< Storage flash init function. */
    fmna_storage_flash_read_t   flash_read;             /**< Storage flash read function. */
    fmna_storage_flash_write_t  flash_write;            /**< Storage flash write function. */
    fmna_storage_flash_erase_t  flash_erase;            /**< Storage flash erase function. */
    fmna_speaker_on_t           speaker_on;             /**< Speaker on function. */
    fmna_speaker_off_t          speaker_off;            /**< Speaker off function. */
    fmna_bat_level_get_t        bat_level_get;          /**< Battery level get function. */
    fmna_motion_detect_start_t  motion_detect_start;    /**< Motion sensor detect start/power on function. */
    fmna_motion_detect_stop_t   motion_detect_stop;     /**< Motion sensor detect stop/power off function. */
    fmna_motion_is_detected_t   is_motion_detected;     /**< Motion sensor is detected motion function. */
} __attribute__((packed)) fmna_startup_cb_cfg_t;

/** @} */


/**
 * @defgroup FMNA_APPLICATION_FUNCTION Functions
 * @{
 */
/**
 *****************************************************************************************
 * @brief FMNA Application startup.
 *
 * @param[in] p_info_cfg: Pointer to information configuration.
 * @param[in] p_cb_cfg:   Pointer to callback configuration.
 *
 * @return Result of startup.
 *****************************************************************************************
 */
fmna_ret_code_t fmna_application_startup(fmna_startup_info_cfg_t *p_info_cfg, fmna_startup_cb_cfg_t *p_cb_cfg);

/**
 *****************************************************************************************
 * @brief FMNA Application DEBUG enable shall be available only when the accessory is in development/certificaiton, shall not be enabled in shipping firmware.
 *
 *
 * @return Result of DEBUG enable.
 *****************************************************************************************
 */
fmna_ret_code_t fmna_application_debug_enable(void);

/**
 *****************************************************************************************
 * @brief FMNA Application NFC enable if need NFC feature.
 *
 * @param[in] info_update:   Pointer to NFC info update function.
 * @param[in] accessory_pid: Accessory PID.
 *
 * @return Result of NFC enable.
 *****************************************************************************************
 */
fmna_ret_code_t fmna_application_nfc_enable(fmna_nfc_info_update_t info_update, uint16_t accessory_pid);

/**
 *****************************************************************************************
 * @brief FMNA Application UARP enable if need UARP feature.
 *
 * @param[in] p_uarp_cfg:      Pointer to uarp information configuration.
 * @param[in] uarp_pro_notify: UARP process notify callback.
 *
 * @return Result of uarp enable.
 *****************************************************************************************
 */
fmna_ret_code_t fmna_application_uarp_enable(fmna_uarp_cfg_t *p_uarp_cfg, fmna_uarp_pro_notify_t uarp_pro_notify);

/**
 *****************************************************************************************
 * @brief FMNA Application BLE event handle.
 *
 * @param[in] p_evt: Pointer to BLE event.
 *****************************************************************************************
 */
void fmna_application_ble_evt_handle(const ble_evt_t *p_evt);

/**
 *****************************************************************************************
 * @brief FMNA application task, should be called in while loop.
 *****************************************************************************************
 */
void fmna_application_task(void);

/**
 *****************************************************************************************
 * @brief FMNA active set.
 *****************************************************************************************
 */
void fmna_application_active_set(bool is_enable);

/**
 *****************************************************************************************
 * @brief FMNA serial number report enable set.
 *****************************************************************************************
 */
void fmna_application_serial_num_report_set(bool is_enable);

/**
 *****************************************************************************************
 * @brief FMNA factory reset.
 *****************************************************************************************
 */
void fmna_application_factory_reset(void);

/**
 *****************************************************************************************
 * @brief FMNA Application restart paired advertising.
 *
 * @param[in] duration: Paired advertising duration (in unit of 10ms). 0 means that advertising continues until the host disables it
 *
 * @return Result of adv start state.
 *****************************************************************************************
 */
fmna_ret_code_t fmna_application_pair_adv_restart(uint16_t duration);

/**
 *****************************************************************************************
 * @brief FMNA Application advertising interval and duration customize if need.
 *
 * @note Default Param:
 * PAIRING FAST ADV INTERVAL: 0x30  DURATION: 300
 * PAIRING SLOW ADV INTERVAL: 0x30  DURATION: 0
 * 
 * SEPARATED FAST ADV INTERVAL: 0x30  DURATION: 12000
 * SEPARATED SLOW ADV INTERVAL: 0xC80 DURATION: 0
 * 
 * NEARBY FAST ADV INTERVAL: 0x30  DURATION: 300
 * NEARBY SLOW ADV INTERVAL: 0xC80 DURATION: 0
 * 
 * @param[in] p_intv_dur_customize:      Pointer to advertising interval and duration customize info.
 *****************************************************************************************
 */
void fmna_application_adv_intv_dur_customize(fmna_adv_intv_dur_customize_t *p_intv_dur_customize);

/** @} */


#endif

