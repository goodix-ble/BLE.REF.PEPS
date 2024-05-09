

#ifndef fmna_connection_platform_h
#define fmna_connection_platform_h

#include "fmna_application.h"

#define USED_FMNA_LTK_FOR_LTK_EN       1
#define USED_FMNA_LTK_FOR_LTK_DIS      0

#define LINK_NUMS_SLAVES               2
///// Disconnect the link.
///// @details     Disconnects the link with BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION disconnect reason.
///// @param[in]   conn_handle     Connection handle for connection to disconnect.
fmna_ret_code_t fmna_connection_platform_disconnect(uint8_t conn_handle);

/// This function sets up all the necessary GAP (Generic Access Profile) parameters of the
/// device, including the device name, appearance, and the preferred connection parameters.
void fmna_connection_platform_gap_params_init(void);

void fmna_sec_info_request_handler(uint8_t conn_idx);

void fmna_connection_platform_log_token(void * auth_token, uint16_t token_size, uint8_t isCrash);

void fmna_connection_platform_get_serial_number(uint8_t * pSN, uint8_t length);

void fmna_connection_update_mfi_token_storage(void *p_data, uint16_t data_size);
bool fmna_connection_mfi_token_stored(void);

void fmna_connection_platform_connected_handler(uint8_t conn_idx, const ble_gap_evt_connected_t *p_param);
void fmna_connection_platform_disconnect_handler(uint8_t conn_idx, uint8_t reason);
void fmna_connection_platform_connection_update_handler(uint8_t conn_idx, const ble_gap_evt_conn_param_updated_t *p_conn_param_update_info);
void fmna_connection_platform_connection_update_req_handler(uint8_t conn_idx, const ble_gap_evt_conn_param_update_req_t *p_conn_param_update_req);
void fmna_connection_platform_sec_rcv_enc_req_handler(uint8_t conn_idx, const ble_sec_evt_enc_req_t *p_enc_req);
void fmna_connection_platform_sec_rcv_enc_ind_handler(uint8_t conn_idx, uint16_t enc_ind, ble_sec_evt_enc_ind_t auth);


#endif /* fmna_connection_platform_h */
