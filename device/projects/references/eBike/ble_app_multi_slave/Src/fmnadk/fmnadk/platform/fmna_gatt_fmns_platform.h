

#ifndef fmna_gatt_platform_h
#define fmna_gatt_platform_h

#include "fmna_gatt.h"
#include "fmna_application.h"
#include "fmna_constants.h"

void fmna_gatt_platform_mtu_update_handler(uint16_t status, uint16_t mtu);
void fmna_gatt_platform_services_init(void);
fmna_ret_code_t fmna_gatt_platform_send_indication(uint16_t conn_handle, FMNA_Service_Opcode_t *opcode, uint8_t *p_data, uint16_t length);
uint8_t fmna_gatt_platform_get_next_command_response_index(void);
void fmna_application_gatt_service_hide(uint8_t conidx);

#endif /* fmna_gatt_platform_h */
