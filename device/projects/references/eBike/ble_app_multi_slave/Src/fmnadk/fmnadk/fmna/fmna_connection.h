

#ifndef fmna_connection_h
#define fmna_connection_h

#include "fmna_constants_platform.h"

// Multi status bits
typedef enum {
    FMNA_MULTI_STATUS_PERSISTENT_CONNECTION        = 0,
    FMNA_MULTI_STATUS_PLAYING_SOUND                = 2,
    FMNA_MULTI_STATUS_UPDATING_FIRMWARE            = 3,
    FMNA_MULTI_STATUS_ENCRYPTED                    = 5,
    FMNA_MULTI_STATUS_IS_MULTIPLE_OWNERS_CONNECTED = 6,
} FMNA_Multi_Status_t;

typedef struct {
    uint64_t conn_ts;         // Timestamp (in app_timer ticks) of connection establishment.
    uint32_t multi_status;    // Bitmask of multi status bits
    uint8_t conn_handle;
    uint16_t conn_intv;       // Connection interval in units of 1.25ms.
} fmna_active_conn_info_t;

// Connection info for all connections.
extern fmna_active_conn_info_t m_fmna_active_connections[FMNA_BLE_MAX_SUPPORTED_CONNECTIONS];

/// Initializes fmna_connection module and relevant structs.
void fmna_connection_init(void);

/// Set max allowed simultaneous connections, from iOS.
/// @details     Sets max connections, if valid, and potentially starts advertising Nearby again.
///              If max connections is set to 1, we disconnect all OTHER links currently
///              connected, and max connections response will be delayed until disconnects
///              are complete. Otherwise, max connections response is sent synchronously,
///
/// @param[in]   max_connections     Maximum allowed connections, depending on how many
///                               applicable devices user has on iCloud account.
///
/// @param[in]   conn_handle              Connection handle of connection setting max connections.
void fmna_connection_set_max_connections(uint8_t max_connections, uint16_t conn_handle);

/// Disconnects every link connected to us.
void fmna_connection_disconnect_all(void);

/// Disconnects current connection, e.g. most recently connected device.
void fmna_connection_disconnect_this(void);

/// Sets or clears multi status bit for valid connection.
/// @param[in]   conn_handle     Connection handle for connection to update info of.
/// @param[in]   status                 Multi status bit to update.
/// @param[in]   enable                 Boolean flag: enable or disable a multi status bit.
void fmna_connection_update_connection_info(uint16_t conn_handle, FMNA_Multi_Status_t status, bool enable);

void fmna_connection_update_connection_info_all(FMNA_Multi_Status_t status, bool enable);

/// Get number of currently active connections.
/// @return Number of currently active connections, 0 if none.
uint8_t fmna_connection_get_num_connections(void);

void fmna_connection_send_multi_status(uint16_t conn_handle);

/// Checks if connection has a valid handle.
/// @return True if this connection handle is for a valid connection, False otherwise.
bool fmna_connection_is_valid_connection(uint16_t conn_handle);

/// Checks whether a particular connection, or any connection, has a particular multi status bit enabled.
/// @param[in]   status      Multi status bit to check.
/// @param[in]   conn_handle     Connection handle for the connection to check, or FMNA_BLE_CONN_HANDLE_INVALID.
/// @return True is this connection, or any connection (if applicable) handle is a valid connection and has this multi status bit enabled, False otherwise.
bool fmna_connection_is_status_bit_enabled(uint16_t conn_handle, FMNA_Multi_Status_t status);


uint32_t fmna_connection_get_non_owner_timeout(void);

/// Find connection with particular multi status bit enabled.
/// @param[in]   status      Multi status bit to check.
/// @return      Connection handle of connection with multi status bit enabled,
///              or GAP_INVALID_CONN_INDEX if no connection has this multi status bit enabled.
uint16_t fmna_connection_get_conn_handle_with_multi_status_enabled(FMNA_Multi_Status_t status);

void fmna_connection_set_is_fmna_paired(bool is_paired);

// Function to determine on the app level, if app is paired 
// Function checks the global variable of app's pairing status, which is only set after successfully pairing complete
bool fmna_connection_is_fmna_paired(void);

bool fmna_connection_is_fmna_active(uint16_t conn_idx);

void fmna_connection_connected_handler(uint16_t conn_handle, uint16_t conn_interval);
void fmna_connection_conn_param_update_handler(uint16_t conn_handle, uint16_t conn_interval);
void fmna_connection_disconnected_handler(uint16_t conn_handle, uint8_t disconnect_reason);


/// Set the LTK to swap existing LTK with for connection.
void fmna_connection_set_active_ltk(uint8_t new_ltk[FMNA_BLE_SEC_KEY_SIZE]);
uint8_t *fmna_connection_get_active_ltk(void);

uint8_t fmna_connection_get_max_connections(void);
void fmna_connection_set_unpair_pending(bool enable);
bool fmna_connection_get_unpair_pending(void);
void fmna_connection_fmna_unpair(bool force_disconnect);




uint16_t fmna_gatt_platform_get_most_recent_conn_handle(void);


#endif /* fmna_connection_h */
