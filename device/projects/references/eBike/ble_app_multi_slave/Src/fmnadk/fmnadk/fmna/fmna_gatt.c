

#include "fmna_gatt.h"
#include "fmna_util.h"
#include "fmna_config_control_point.h"
#include "fmna_connection.h"
#include "fmna_pairing_control_point.h"
#include "fmna_nonowner_control_point.h"
#include "fmna_state_machine.h"
#include "fmna_paired_owner_control_point.h"


#if FMNA_UARP_ENABLE
#include "fmna_uarp_control_point.h"
#endif

#if FMNA_DEBUG_ENABLE
#include "fmna_debug_control_point.h"
#endif // DEBUG

#define FMNA_OPCODE_LENGTH                  2
#define START_PACKET_DATA_LENGTH            (FMNA_BLE_MTU_SIZE - FRAGMENTATION_HEADER_LENGTH - FMNA_OPCODE_LENGTH)
#define CONTINUE_PACKET_DATA_LENGTH         (FMNA_BLE_MTU_SIZE - FRAGMENTATION_HEADER_LENGTH)

uint16_t m_gatt_mtu = FMNA_BLE_MTU_SIZE - FMNA_BLE_GATT_HEADER_LEN;

typedef struct {
    uint8_t header;
    union {
        struct {
            FMNA_Service_Opcode_t opcode;
            uint8_t               data[START_PACKET_DATA_LENGTH];
        } __attribute__((packed)) single_packet_data;
        uint8_t continuation_packet_data[CONTINUE_PACKET_DATA_LENGTH];
    } __attribute__((packed)) data;
} __attribute__((packed)) tx_buffer_t;

tx_buffer_t tx_buffer;


fmna_service_extended_packet_t fmna_service_current_extended_packet_tx;

/// Ensure this GATT message has valid payload after fragmented flag header byte.
static fmna_ret_code_t fmna_gatt_verify_rx_length(uint16_t length) {
    if (length >= (FRAGMENTED_FLAG_LENGTH + sizeof(FMNA_Service_Opcode_t))) {
        return FMNA_SUCCESS;
    } else {
        return FMNA_ERROR_INVALID_LENGTH;
    }
}

FMNA_Response_Status_t fmna_gatt_verify_control_point_opcode_and_length(FMNA_Service_Opcode_t opcode, uint16_t length, fmna_service_length_check_manager_t *managers, uint8_t num_managers) {
    for (uint8_t i = 0; i < num_managers; i++) {
        if (managers[i].opcode == opcode) {
            if (managers[i].length == length) {
                return RESPONSE_STATUS_SUCCESS;
            } else {
                return RESPONSE_STATUS_INVALID_LENGTH;
            }
        }
    }
    return RESPONSE_STATUS_INVALID_COMMAND;
}

fmna_ret_code_t fmna_gatt_config_char_write_handler(uint16_t conn_handle, uint16_t length, uint8_t const *data) {
    fmna_ret_code_t ret_code = FMNA_SUCCESS;

    ret_code = fmna_gatt_verify_rx_length(length);
    if (ret_code != FMNA_SUCCESS) {
        fmna_gatt_send_command_response(FMNA_SERVICE_OPCODE_COMMAND_RESPONSE, conn_handle, FMNA_SERVICE_OPCODE_NONE, RESPONSE_STATUS_INVALID_COMMAND);
        return FMNA_ERROR_INVALID_DATA;
    }
    
    // Check fragmentation
    if (data[FRAGMENTED_FLAG_INDEX] != FRAGMENTED_FLAG_FINAL) {
        fmna_gatt_send_command_response(FMNA_SERVICE_OPCODE_COMMAND_RESPONSE, conn_handle, *(FMNA_Service_Opcode_t *)&data[FRAGMENTED_FLAG_LENGTH], RESPONSE_STATUS_INVALID_LENGTH);
        return FMNA_ERROR_INVALID_LENGTH;
    }
    
    fmna_config_control_point_rx_handler(conn_handle, &data[FRAGMENTED_FLAG_LENGTH], length - FRAGMENTED_FLAG_LENGTH);
    
    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_gatt_nonown_char_write_handler(uint16_t conn_handle, uint16_t length, uint8_t const *data) {
    fmna_ret_code_t ret_code = FMNA_SUCCESS;

    //Check length
    ret_code = fmna_gatt_verify_rx_length(length);
    if (ret_code != FMNA_SUCCESS) {
        fmna_gatt_send_command_response(FMNA_SERVICE_NON_OWNER_OPCODE_COMMAND_RESPONSE, conn_handle, FMNA_SERVICE_OPCODE_NONE, RESPONSE_STATUS_INVALID_COMMAND);
        return FMNA_ERROR_INVALID_DATA;
    }
    
    // Check fragmentation
   if (data[FRAGMENTED_FLAG_INDEX] != FRAGMENTED_FLAG_FINAL) {
       fmna_gatt_send_command_response(FMNA_SERVICE_NON_OWNER_OPCODE_COMMAND_RESPONSE, conn_handle, *(FMNA_Service_Opcode_t *)&data[FRAGMENTED_FLAG_LENGTH], RESPONSE_STATUS_INVALID_LENGTH);
       return FMNA_ERROR_INVALID_LENGTH;
   }

    fmna_nonowner_rx_handler(conn_handle, &data[FRAGMENTED_FLAG_LENGTH], length - FRAGMENTED_FLAG_LENGTH);

    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_gatt_paired_owner_char_write_handler(uint16_t conn_handle, uint16_t length, uint8_t const *data)
{
    fmna_ret_code_t ret_code = FMNA_SUCCESS;

    //Check length
    ret_code = fmna_gatt_verify_rx_length(length);
    if (ret_code != FMNA_SUCCESS) {
        fmna_gatt_send_command_response(FMNA_SERVICE_PAIRED_OWNER_OPCODE_COMMAND_RESPONSE, conn_handle, FMNA_SERVICE_OPCODE_NONE, RESPONSE_STATUS_INVALID_COMMAND);
        return FMNA_ERROR_INVALID_LENGTH;
    }

    // Check fragmentation
    if (data[FRAGMENTED_FLAG_INDEX] != FRAGMENTED_FLAG_FINAL) {
       fmna_gatt_send_command_response(FMNA_SERVICE_PAIRED_OWNER_OPCODE_COMMAND_RESPONSE, conn_handle, *(FMNA_Service_Opcode_t *)&data[FRAGMENTED_FLAG_LENGTH], RESPONSE_STATUS_INVALID_LENGTH);
       return FMNA_ERROR_INVALID_DATA;
    }

    fmna_paired_owner_rx_handler(conn_handle, &data[FRAGMENTED_FLAG_LENGTH], length - FRAGMENTED_FLAG_LENGTH);

    return FMNA_SUCCESS;
}

#if FMNA_DEBUG_ENABLE
fmna_ret_code_t fmna_gatt_debug_char_write_handler(uint16_t conn_handle, uint16_t length, uint8_t const *data)
{
    fmna_ret_code_t ret_code = FMNA_SUCCESS;

    //Check length
    ret_code = fmna_gatt_verify_rx_length(length);
    if (ret_code != FMNA_SUCCESS) {
        fmna_gatt_send_command_response(FMNA_SERVICE_DEBUG_OPCODE_COMMAND_RESPONSE, conn_handle, FMNA_SERVICE_OPCODE_NONE, RESPONSE_STATUS_INVALID_STATE);
        return FMNA_ERROR_INVALID_LENGTH;
    }

    // Check fragmentation
   if (data[FRAGMENTED_FLAG_INDEX] != FRAGMENTED_FLAG_FINAL) {
       fmna_gatt_send_command_response(FMNA_SERVICE_DEBUG_OPCODE_COMMAND_RESPONSE, conn_handle, *(FMNA_Service_Opcode_t *)&data[FRAGMENTED_FLAG_LENGTH], RESPONSE_STATUS_INVALID_LENGTH);
       return FMNA_ERROR_INVALID_DATA;
   }

    fmna_debug_control_point_rx_handler(conn_handle, &data[FRAGMENTED_FLAG_LENGTH], length - FRAGMENTED_FLAG_LENGTH);

    return FMNA_SUCCESS;
}
#endif //DEBUG

fmna_ret_code_t fmna_gatt_pairing_char_authorized_write_handler(uint16_t conn_handle, uint16_t length, uint8_t const *data)
{
    fmna_ret_code_t ret_code = FMNA_SUCCESS;

    if (fmna_connection_is_fmna_paired() == true)
    {
        return FMNA_ERROR_INVALID_STATE;
    }
    ret_code = fmna_pairing_control_point_append_to_rx_buffer(&data[FRAGMENTED_FLAG_LENGTH], length - FRAGMENTED_FLAG_LENGTH);
    if (ret_code != FMNA_SUCCESS)
    {
        return ret_code;
    }

    if (data[FRAGMENTED_FLAG_INDEX] == FRAGMENTED_FLAG_FINAL)
    {
        ret_code = fmna_gatt_verify_rx_length(length);
        if (ret_code != FMNA_SUCCESS)
        {
            fmna_pairing_control_point_unpair();
            return ret_code;
        }
        fmna_pairing_control_point_handle_rx();
    }

    return ret_code;
}

static bool fmna_gatt_is_tx_allowed(uint16_t conn_handle, FMNA_Service_Opcode_t opcode)
{
    bool tx_allowed = true;

    switch (opcode & FMNA_SERVICE_OPCODE_BASE_MASK)
    {
        case FMNA_SERVICE_OPCODE_CONFIG_CONTROL_POINT_BASE:
            tx_allowed = fmna_config_control_point_is_tx_allowed(conn_handle, opcode);
            break;

        default:
            break;
    }

    return tx_allowed;
}

static void fmna_gatt_dispatch_send_packet_extension_indication_handler(void *p_event_data, uint16_t event_size) {
    fmna_gatt_send_indication_internal(fmna_gatt_get_most_recent_conn_handle(), FMNA_SERVICE_OPCODE_PACKET_EXTENSION, fmna_service_current_extended_packet_tx.data, fmna_service_current_extended_packet_tx.length);
}

void fmna_gatt_dispatch_send_packet_extension_indication(void)
{
    fmna_queue_platform_evt_put(NULL, 0, fmna_gatt_dispatch_send_packet_extension_indication_handler);
}

void fmna_gatt_send_indication(uint16_t conn_handle, FMNA_Service_Opcode_t opcode, void *data, uint16_t length)
{
    if (fmna_connection_get_num_connections() == 0)
    {
        return;
    }

    fmna_gatt_send_indication_internal (conn_handle, opcode, data, length);
}

void fmna_gatt_send_indication_internal (uint16_t conn_handle, FMNA_Service_Opcode_t opcode, void *data, uint16_t length) {
    uint8_t     headers_length;
    uint16_t    packet_length;
    bool        did_send_indication = false;
    
    fmna_ret_code_t  ret_code;

    headers_length = sizeof(tx_buffer.header);
    if (opcode != FMNA_SERVICE_OPCODE_PACKET_EXTENSION && opcode != FMNA_SERVICE_OPCODE_INTERNAL_UARP) {    // If the indication being sent is the first fragment, include the opcode in the "header"
        headers_length += sizeof(opcode);
    }
    
    memset(&tx_buffer, 0, sizeof(tx_buffer));
    
    // Fill header
    if ((headers_length + length) > m_gatt_mtu) {
        CLR_BIT(tx_buffer.header, FRAGMENTATION_BIT);
        packet_length = m_gatt_mtu;
    } else {
        SET_BIT(tx_buffer.header, FRAGMENTATION_BIT);   // Setting the fragmentation bit means last fragment of indication to be sent
        packet_length = headers_length + length;
    }
    
    // Fill data
    if (opcode != FMNA_SERVICE_OPCODE_PACKET_EXTENSION && opcode != FMNA_SERVICE_OPCODE_INTERNAL_UARP) {
        tx_buffer.data.single_packet_data.opcode = opcode;
        if ((headers_length + length) > m_gatt_mtu) {
            memcpy(tx_buffer.data.single_packet_data.data, data, m_gatt_mtu - headers_length);
        } else {
            memcpy(tx_buffer.data.single_packet_data.data, data, length);
        }
    } else {
        if ((headers_length + length) > m_gatt_mtu) {
            memcpy(tx_buffer.data.continuation_packet_data, data, m_gatt_mtu - headers_length);
        } else {
            memcpy(tx_buffer.data.continuation_packet_data, data, length);
        }
    }
    
    if (fmna_gatt_is_tx_allowed(conn_handle, opcode)) {
        // Send indication to the specific central.
        ret_code = fmna_gatt_platform_send_indication(conn_handle, &opcode, (uint8_t *)&tx_buffer, packet_length);
        if (FMNA_SUCCESS != ret_code) {
            FMNA_ERROR_CHECK(ret_code);
        }
        else {
            did_send_indication = true;
        }
    }

    if (did_send_indication) {
        // Check if there is still indication left to be sent
        if ((headers_length + length) > m_gatt_mtu) {
            fmna_service_current_extended_packet_tx.opcode = opcode;
            fmna_service_current_extended_packet_tx.data   = (uint8_t *)data + (m_gatt_mtu - headers_length);
            fmna_service_current_extended_packet_tx.length = length - (m_gatt_mtu - headers_length);
        }
        else
        {
            memset(&fmna_service_current_extended_packet_tx, 0, sizeof(fmna_service_current_extended_packet_tx));
        }
    }
}

control_point_command_response_data_t m_command_response_data[MAX_CONTROL_POINT_RSP];
uint8_t m_command_response_index = 0;

void fmna_gatt_send_command_response(FMNA_Service_Opcode_t command_response_opcode, uint16_t conn_handle, FMNA_Service_Opcode_t command_opcode, FMNA_Response_Status_t status)
{
    uint8_t index = fmna_gatt_platform_get_next_command_response_index();
    m_command_response_data[index].opcode = command_opcode;
    m_command_response_data[index].status = status;
    fmna_gatt_send_indication(conn_handle, command_response_opcode, &(m_command_response_data[index]), sizeof(control_point_command_response_data_t));
}

void fmna_gatt_services_init(void)
{
    fmna_gatt_platform_services_init();
}

uint16_t fmna_gatt_get_most_recent_conn_handle(void)
{
    return fmna_gatt_platform_get_most_recent_conn_handle();
}
