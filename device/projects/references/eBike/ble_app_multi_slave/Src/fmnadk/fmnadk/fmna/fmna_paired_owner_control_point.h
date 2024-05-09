
#ifndef fmna_paired_owner_control_point_h
#define fmna_paired_owner_control_point_h

#include "fmna_constants_platform.h"

#include "fmna_gatt.h"

extern bool g_serial_num_report_enable;

/// Function for handling the different Paired Owner opcodes.
/// @param data Buffer of data of Paired Owner opcode and possible operands
void fmna_paired_owner_rx_handler(uint16_t conn_handle, uint8_t const * data, uint16_t length);
void fmna_serial_num_report_ctrl(uint8_t report_ctrl);
uint8_t fmna_serial_num_report_en_query(void);

#endif /* fmna_paired_owner_control_point_h */


