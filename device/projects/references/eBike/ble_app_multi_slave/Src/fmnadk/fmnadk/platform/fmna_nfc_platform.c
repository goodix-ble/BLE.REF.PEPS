
#include "fmna_nfc_platform.h"
#include "fmna_log_platform.h"

static fmna_nfc_info_update_t s_nfc_info_update;

fmna_ret_code_t fmna_nfc_platform_init(fmna_nfc_info_update_t info_update)
{
    s_nfc_info_update = info_update;

    return FMNA_SUCCESS;
}

void fmna_nfc_platform_info_update(uint8_t *p_data, uint16_t length)
{
    if (NULL == p_data || 0 == length)
    {
        return;
    }

    if (s_nfc_info_update)
    {
        s_nfc_info_update(p_data, length);
    }
    else
    {
        FMNA_LOG_INFO("No [NFC] module, [ACTION]: update info [%s]", p_data);
    }

    return;
}
