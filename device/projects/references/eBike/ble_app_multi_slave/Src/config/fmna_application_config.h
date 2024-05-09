

#ifndef __FMNA_APPLICATION_CONFIG_H__
#define __FMNA_APPLICATION_CONFIG_H__

#include "custom_config.h"
/**Note !!!! NVDS Tag 0x4011-0x4019  Reserved for FMNA Core */


#define FMNA_MODEL_NAME                         "Goodix Tag Demo"
#define FMNA_MFR_NAME                           "Goodix Technology Co., Ltd."
#define FMNA_HARDWARE_REV                       "GR5332_SK_BASIC"

#define FMNA_ACCESSORY_PRODUCT_DATA             {0xf8,0x6a,0xe4,0x3a,0x97,0x20,0xf5,0xff}
#define FMNA_ACCESSORY_CATEGORY                 1

// If enable NFC feature
#define FMNA_ACCESSORY_PID                      0xCAFE

#define FMNA_FW_REV_MAJOR_NUMBER                0
#define FMNA_FW_REV_MINOR_NUMBER                9
#define FMNA_FW_REV_REVISION_NUMBER             0

#if defined(SOC_GR5332)
    #define FMNA_SOFTWARE_AUTH_UUID_SAVE_ADDR   0x0027A000
    #define FMNA_UARP_DFU_FW_SAVE_ADDR          0x00250000
    #define FMNA_UARP_DFU_INFO_SAVE_ADDR        0x00203000
#endif

#if defined(SOC_GR5515) && (CHIP_TYPE <= 5)
    #define FMNA_SOFTWARE_AUTH_UUID_SAVE_ADDR   0x010FA000
    #define FMNA_UARP_DFU_FW_SAVE_ADDR          0x0105D000
    #define FMNA_UARP_DFU_INFO_SAVE_ADDR        0x01003000
#endif

#if defined(SOC_GR5515) && (CHIP_TYPE > 5)
    #define FMNA_SOFTWARE_AUTH_UUID_SAVE_ADDR   0x0107A000
    #define FMNA_UARP_DFU_FW_SAVE_ADDR          0x0105D000
    #define FMNA_UARP_DFU_INFO_SAVE_ADDR        0x01003000
#endif

#define FMNA_UARP_DFU_4CC_TAG                   { 'S', 'M', 'P', 'L' }

#define FMNA_MULTI_SLAVE_ENABLE                 1
#define FMNA_CUSTOM_BOARD_ENABLE                0

#endif


