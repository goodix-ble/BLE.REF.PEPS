

#include "fmna_storage_platform.h"

static fmna_storage_flash_read_t    s_flash_read;
static fmna_storage_flash_write_t   s_flash_write;
static fmna_storage_flash_erase_t   s_flash_erase;

fmna_ret_code_t fmna_storage_platform_init( fmna_storage_flash_init_t   flash_init,
                                            fmna_storage_flash_read_t   flash_read,
                                            fmna_storage_flash_write_t  flash_write,
                                            fmna_storage_flash_erase_t  flash_erase)
{
    if (NULL == flash_init ||
        NULL == flash_read ||
        NULL == flash_write ||
        NULL == flash_erase)
    {
        return FMNA_ERROR_INVALID_STORAGE_PARAM;
    }

    s_flash_read   = flash_read;
    s_flash_write  = flash_write;
    s_flash_erase  = flash_erase;


    return flash_init() ? FMNA_SUCCESS : FMNA_ERROR_INTERNAL;
}

uint32_t fmna_storage_platform_flash_read(const uint32_t addr, uint8_t *buf, const uint32_t size)
{
    if (s_flash_read)
    {
        return s_flash_read(addr, buf, size);
    }

    return FMNA_ERROR_INVALID_STORAGE_PARAM;
}
uint32_t fmna_storage_platform_flash_write(const uint32_t addr, const uint8_t *buf, const uint32_t size)
{
    if (s_flash_write)
    {
        return s_flash_write(addr, buf, size);
    }

    return FMNA_ERROR_INVALID_STORAGE_PARAM;
}

bool fmna_storage_platform_flash_erase(const uint32_t addr, const uint32_t size)
{
    if (s_flash_erase)
    {
        return s_flash_erase(addr, size);
    }

    return FMNA_ERROR_INVALID_STORAGE_PARAM;
}



uint16_t fmna_storage_platform_key_value_set(uint16_t key, uint16_t len, uint8_t *p_data)
{
    return nvds_put(key, len, p_data);
}

uint16_t fmna_storage_platform_key_value_get(uint16_t key, uint16_t* p_len, uint8_t *p_data)
{
    return  nvds_get(key, p_len, p_data);
}

uint16_t fmna_storage_platform_key_value_delete(uint16_t key)
{
    return nvds_del(key);
}

