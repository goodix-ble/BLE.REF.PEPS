



#include "fmna_util.h"
#include "fmna_version.h"

// FW version bit offsets as described in spec.
#define FW_VERSION_MAJOR_NUMBER_OFFSET  16
#define FW_VERSION_MINOR_NUMBER_OFFSET  8

static uint32_t m_fw_version = 0;
static bool     s_version_init = false;


uint32_t fmna_version_get_fw_version(void)
{
    if (!s_version_init)
    {
        m_fw_version |= g_fmna_info_cfg.fw_rev_revision_number;
        m_fw_version |= BF_VAL(g_fmna_info_cfg.fw_rev_major_number, 16, FW_VERSION_MAJOR_NUMBER_OFFSET);
        m_fw_version |= BF_VAL(g_fmna_info_cfg.fw_rev_minor_number, 8,  FW_VERSION_MINOR_NUMBER_OFFSET);
        s_version_init = true;
    }
    return m_fw_version;
}
