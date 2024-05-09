



#include "findmy_slave.h"

#include "app_error.h"
#include "app_log.h"



static fmna_startup_info_cfg_t s_fmna_info_cfg;


static fmna_startup_cb_cfg_t s_fmna_cb_cfg = 
{
    .log_output             = findmy_log_output,
    .queue_send             = findmy_queue_send,
    .queue_receive          = findmy_queue_receive,
    .mem_malloc             = findmy_mem_malloc,
    .mem_realloc            = findmy_mem_realloc,
    .mem_free               = findmy_mem_free,
    .flash_init             = findmy_flash_init,
    .flash_read             = findmy_flash_read,
    .flash_write            = findmy_flash_write,
    .flash_erase            = findmy_flash_erase,

#if FMNA_CUSTOM_BOARD_ENABLE
    .bat_level_get          = custom_battery_det_value_get,
    .speaker_on             = custom_speaker_on,
    .speaker_off            = custom_speaker_off,
    .motion_detect_start    = custom_motion_det_start,
    .motion_detect_stop     = custom_motion_det_stop,
    .is_motion_detected     = custom_motion_det_is_motion_detected,
#endif
};

static fmna_uarp_cfg_t s_fmna_uarp_cfg =
{
    ._4cc_tag               = FMNA_UARP_DFU_4CC_TAG,
    .dfu_info_save_addr     = FMNA_UARP_DFU_INFO_SAVE_ADDR,
    .dfu_fw_save_addr       = FMNA_UARP_DFU_FW_SAVE_ADDR,
};

static void fmna_uarp_pro_notify(fmna_uarp_ntf_evt_t *p_uarp_ntf_evt)
{
    if (NULL == p_uarp_ntf_evt)
    {
        return;
    }

    switch (p_uarp_ntf_evt->notify_type)
    {
        case FMNA_UARP_DFU_FILE_INFO_RECV:
            APP_LOG_INFO("UARP - [FMNA_UARP_DFU_FILE_INFO_RECV], size[%d]", p_uarp_ntf_evt->param);
            break;
        case FMNA_UARP_DFU_FILE_PROGRAM:
            APP_LOG_INFO("UARP - [FMNA_UARP_DFU_FILE_PROGRAM], pro[%d%%]", p_uarp_ntf_evt->param);
            break;
        case FMNA_UARP_DFU_INFO_UPDEATE:
            APP_LOG_INFO("UARP - [FMNA_UARP_DFU_INFO_UPDEATE]");
            break;
        case FMNA_UARP_ERROR_OCCUR:
            APP_LOG_INFO("UARP - [FMNA_UARP_ERROR_OCCUR], path[%d]", p_uarp_ntf_evt->param);
            break;
        default:
            break;
    }
}


static void findmy_product_info_load(void)
{
    // TODO 
    // Load Special Serial Number
    uint8_t uid[FMNA_SERIAL_NUMBER_LEN] = {0};
    uint8_t index   = 0;
    sys_device_uid_get(uid);

    for (uint8_t i = 0; i < FMNA_SERIAL_NUMBER_LEN; i++)
    {
        if ((uid[i] & 0xf) >= 0 && (uid[i] & 0xf) <= 9)
        {
            s_fmna_info_cfg.serial_number[index++] = '0' + (uid[i] & 0xf);
        }
        else
        {
             s_fmna_info_cfg.serial_number[index++] = 'A' + (uid[i] & 0xf);
        }
    }

    // TODO 
    // Load Special Product Data (From MFi Product Plan)
    uint8_t product_data[FMNA_PRODUCT_DATA_LEN] = FMNA_ACCESSORY_PRODUCT_DATA;
    memcpy(s_fmna_info_cfg.product_data, product_data, FMNA_PRODUCT_DATA_LEN);

    // TODO 
    // Load Special Product Category (From MFi Product Plan)
    s_fmna_info_cfg.accessory_category = FMNA_ACCESSORY_CATEGORY;

    // TODO
    // Load Special Model Name (Apple Device UI Display)
    uint8_t model_name[FMNA_MODEL_NAME_LEN] = FMNA_MODEL_NAME;
    memcpy(s_fmna_info_cfg.model_name_str, model_name, FMNA_MODEL_NAME_LEN);

    // TODO
    // Load Special Manufacture Name (Apple Device UI Display)
    uint8_t manufacture_name[FMNA_MANUFACTURER_NAME_LEN] = FMNA_MFR_NAME;
    memcpy(s_fmna_info_cfg.manufaturer_name_str, manufacture_name, FMNA_MANUFACTURER_NAME_LEN);

    // TODO
    // Load Special Hardware Revision (Apple UARP Need)
    uint8_t hardware_revision[FMNA_HW_REV_STR_LEN] = FMNA_HARDWARE_REV;
    memcpy(s_fmna_info_cfg.hardware_rev_str, hardware_revision, FMNA_HW_REV_STR_LEN);

    // Load Special Firmware Revision (Apple Device UI Display and Apple UARP Need)
    s_fmna_info_cfg.fw_rev_major_number    = FMNA_FW_REV_MAJOR_NUMBER;
    s_fmna_info_cfg.fw_rev_minor_number    = FMNA_FW_REV_MINOR_NUMBER;
    s_fmna_info_cfg.fw_rev_revision_number = FMNA_FW_REV_REVISION_NUMBER;

    // Load Software Auth Token and UUID Save Address
    s_fmna_info_cfg.software_auth_uuid_save_addr   = FMNA_SOFTWARE_AUTH_UUID_SAVE_ADDR;
}

void findmy_slave_init(void)
{
    uint16_t ret;

    // Load Product Information
    findmy_product_info_load();

    // Enable debug characteristc
    ret = fmna_application_debug_enable();
    APP_ERROR_CHECK(ret);

    // Enable NFC feature
    ret = fmna_application_nfc_enable(NULL, FMNA_ACCESSORY_PID);
    APP_ERROR_CHECK(ret);

    // Enable UARP
    ret = fmna_application_uarp_enable(&s_fmna_uarp_cfg, fmna_uarp_pro_notify);
    APP_ERROR_CHECK(ret);

    // If needed
    //fmna_application_adv_intv_dur_customize(&s_fmna_adv_intv_dur);

    //Startup fmna application
    ret = fmna_application_startup(&s_fmna_info_cfg, &s_fmna_cb_cfg);
    APP_ERROR_CHECK(ret);
}










