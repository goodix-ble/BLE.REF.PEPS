#include "long_term_user.h"
#include "gr55xx_sys.h"
#include "custom_config.h"
#include "app_error.h"
#include "app_log.h"
#include "findmy_slave.h"
#include "fmna_application_config.h"

#define HID_SLAVE_NAME_NVDS  NV_TAG_APP(0x01)
#define SHARE_KEY_NVDS       NV_TAG_APP(0x02)
#define USER_ID_NVDS         NV_TAG_APP(0x03)


static uint8_t s_share_key[32] = {0};
static uint8_t s_name[6] = {'1','2','3','4','5','6'};
static uint8_t s_user_id[6] = {0};
static bool s_bonded = false;

extern void common_slave_adv_data_update(uint8_t *p_data);
extern void hid_slave_adv_data_update(uint8_t *p_data);

bool get_bonded_flag(void)
{
    return s_bonded;
}

uint8_t *get_share_key(void)
{
    return s_share_key;
}

uint8_t *get_user_id(void)
{
    return s_user_id;
}

static void log_nvds_info(uint8_t *p_info, uint8_t len)
{
    for(uint8_t i=0; i<len; i++)
    {
        printf("0x%x,",p_info[i]);
    }
    printf("\r\n");
}

void nvds_info_init(void)
{
    uint8_t get_data[8];
    uint16_t len = 8;
    s_bonded = false;
    nvds_get(HID_SLAVE_NAME_NVDS, &len, get_data);
    if(get_data[0] == 0x55 && get_data[1] == 0x55)//已经配对绑定
    {
        s_bonded = true;
        memcpy(s_name, &get_data[2], 6);
        common_slave_adv_data_update(s_name);
        hid_slave_adv_data_update(s_name);
        len = 32;
        nvds_get(SHARE_KEY_NVDS, &len, s_share_key);
        len = 6;
        nvds_get(USER_ID_NVDS, &len, s_user_id);
        
        printf("nvds_info\r\n");
        log_nvds_info(s_name,6);
        log_nvds_info(s_user_id,6);
        log_nvds_info(s_share_key, 32);
    }
}

void hid_name_save(uint8_t *name, uint8_t len)
{
    uint8_t save_data[8] = {0x55, 0x55};
    memcpy(&save_data[2], name, len);
    memcpy(s_name, name, 6);
    nvds_put(HID_SLAVE_NAME_NVDS, 8, save_data);
}

void save_share_key(uint8_t *p_key, uint8_t len)
{
    memcpy(s_share_key, p_key, 32);
    nvds_put(SHARE_KEY_NVDS, len, p_key);
}

void save_user_id(uint8_t *p_user_id, uint8_t len)
{
    memcpy(s_user_id, p_user_id, 6);
    nvds_put(USER_ID_NVDS, len, p_user_id);
}

void clear_nvds_info(void)
{
    uint8_t save_data[8] = {0x00, 0x00};
    memset(save_data, 0, 8);
    nvds_put(HID_SLAVE_NAME_NVDS, 8, save_data);
    ble_gap_bond_devs_clear();
}

bool bond_info_check(void)
{
    sdk_err_t error_code;
    ble_gap_white_list_t whitelist;
    error_code = ble_gap_whitelist_get(&whitelist);
    APP_ERROR_CHECK(error_code);
    uint8_t bonded_num = whitelist.num;
    if(bonded_num < CFG_MAX_BOND_DEVS)
    {
        return true;
    }
    return false;
}



