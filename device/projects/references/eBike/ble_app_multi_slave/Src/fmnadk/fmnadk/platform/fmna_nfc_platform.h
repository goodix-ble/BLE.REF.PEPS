

#ifndef fmna_nfc_platform_h
#define fmna_nfc_platform_h

#include "fmna_application.h"

/// Initialize platform NFC peripheral. 
fmna_ret_code_t fmna_nfc_platform_init(fmna_nfc_info_update_t info_update);

void fmna_nfc_platform_info_update(uint8_t *p_data, uint16_t length);
#endif /* fmna_nfc_platform_h */
