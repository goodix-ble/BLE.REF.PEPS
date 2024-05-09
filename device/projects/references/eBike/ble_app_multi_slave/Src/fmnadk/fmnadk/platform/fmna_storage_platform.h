
#ifndef fmna_storage_platform_h
#define fmna_storage_platform_h

#include "fmna_application.h"

#define FMNA_PAIRED_STATE_NV_TAG                        0x4001
#define FMNA_PUBLIC_KEY_P_NV_TAG                        0x4002
#define FMNA_SYMMETRIC_KEY_SKN_NV_TAG                   0x4003
#define FMNA_SYMMETRIC_KEY_SKS_NV_TAG                   0x4004
#define FMNA_SERVER_SHARED_SECRET_NV_TAG                0x4005
#define FMNA_CURRENT_PRIMARY_KEY_NV_TAG                 0x4006
#define FMNA_CURRENT_SECONDARY_KEY_NV_TAG               0x4007
#define FMNA_ICLOUD_IDENTIFIER_NV_TAG                   0x4008
#define FMNA_PRODUCT_DATA_NV_TAG                        0x4009

#define FMNA_PAIRED_STATE_NV_TOKEN                      0xa5

fmna_ret_code_t fmna_storage_platform_init( fmna_storage_flash_init_t   flash_init,
                                            fmna_storage_flash_read_t   flash_read,
                                            fmna_storage_flash_write_t  flash_write,
                                            fmna_storage_flash_erase_t  flash_erase);
uint32_t fmna_storage_platform_flash_read(const uint32_t addr, uint8_t *buf, const uint32_t size);
uint32_t fmna_storage_platform_flash_write(const uint32_t addr, const uint8_t *buf, const uint32_t size);
bool     fmna_storage_platform_flash_erase(const uint32_t addr, const uint32_t size);

fmna_ret_code_t fmna_storage_platform_key_value_set(uint16_t key, uint16_t len, uint8_t *p_data);
fmna_ret_code_t fmna_storage_platform_key_value_get(uint16_t key, uint16_t* p_len, uint8_t *p_data);
fmna_ret_code_t fmna_storage_platform_key_value_delete(uint16_t key);

#endif

