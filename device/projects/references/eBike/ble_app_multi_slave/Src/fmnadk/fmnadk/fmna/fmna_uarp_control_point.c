/*
 *      Copyright (C) 2020 Apple Inc. All Rights Reserved.
 *
 *      Find My Network ADK is licensed under Apple Inc.’s MFi Sample Code License Agreement,
 *      which is contained in the License.txt file distributed with the Find My Network ADK,
 *      and only to those who accept that license.
 */

#define APP_LOG_TAG "[UARP]"


#include "fmna_gatt.h"

#include "fmna_state_machine.h"
#include "fmna_connection.h"
#include "fmna_crypto.h"
#include "fmna_version.h"
#include "fmna_uarp_control_point.h"

#include "CoreUARPAppAccessory.h"

#define UARP_MAX_LEN kUARPAppPayloadWindowSize



//MARK: RX Packet Definitions

// General buffer to hold the incoming Pairing characteristic Write data
typedef struct {
    uint16_t len;
    uint8_t data[UARP_MAX_LEN];
} uarp_buffer_t;

static uarp_buffer_t uarp_rx_buffer = {0};

// Only allow one controller
static uint16_t uarp_conn_handle = FMNA_BLE_CONN_HANDLE_INVALID;

static struct UARPAppAccessory uarpAccessory = {0};
static struct UARPAppController uarpController = {0};
static struct UARPVersion activeFW = {0};
static struct UARPVersion stagedFW = {0};
static struct UARPAppCallbacks uarpCBs = {0};
// Hold info for sending messages

static char dummySerial[FMNA_SERIAL_NUMBER_LEN + 1];



static void fmna_uarp_dispatch_send_packet_complete_handler(void *p_event_data, uint16_t event_size) {
    uint32_t ret_code;
    ret_code = UARPAppSendMessageComplete( &uarpAccessory,
                                             &uarpController);
    FMNA_ERROR_CHECK(ret_code);

    UARPAppApplyStagedAssetsPendingHandle();
}

void fmna_uarp_packet_sent(void) {
    fmna_queue_platform_evt_put(NULL, 0, fmna_uarp_dispatch_send_packet_complete_handler);
}

static void fmna_uarp_dispatch_rx_handler(void *p_event_data, uint16_t event_size) {
    // Call into UARP handler
    uint32_t ret_val;
    ret_val = UARPAppRecvMessage(&uarpAccessory,
                                    &uarpController,
                                    uarp_rx_buffer.data,
                                    uarp_rx_buffer.len);
    FMNA_ERROR_CHECK(ret_val);

    memset(&uarp_rx_buffer, 0 , sizeof(uarp_rx_buffer));
}

void fmna_uarp_control_point_handle_rx(void) {
    fmna_queue_platform_evt_put(NULL, 0, fmna_uarp_dispatch_rx_handler);
}


uint32_t fmna_uarp_send_msg( void *pAccessoryDelegate, void *pControllerDelegate, uint8_t *pBuffer, uint32_t length ) {
    fmna_gatt_send_indication(uarp_conn_handle, FMNA_SERVICE_OPCODE_INTERNAL_UARP, pBuffer, length);
    return kUARPStatusSuccess;
}

fmna_ret_code_t fmna_uarp_connect(uint16_t conn_handle) {
    if (uarp_conn_handle == FMNA_BLE_CONN_HANDLE_INVALID) {
        uarp_conn_handle = conn_handle;
        memset(&uarp_rx_buffer, 0 , sizeof(uarp_rx_buffer));
        UARPAppControllerAdd(&uarpAccessory, &uarpController);
    }
    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_uarp_disconnect(uint16_t conn_handle) {
    FMNA_LOG_INFO("UARP Controller disconnect 0x%x", conn_handle);
    if (conn_handle == uarp_conn_handle) {
        FMNA_LOG_INFO("Removing controller for handle 0x%x", conn_handle);
        uarp_conn_handle = FMNA_BLE_CONN_HANDLE_INVALID;
        memset(&uarp_rx_buffer, 0 , sizeof(uarp_rx_buffer));
        UARPAppControllerRemove(&uarpAccessory, &uarpController);
    }
    else {
        FMNA_LOG_INFO("Not Removing controller, different handle uarp_handle %x conn_handle 0x%x", uarp_conn_handle, conn_handle);
    }
    return FMNA_SUCCESS;
}

void fmna_uarp_control_point_init(fcnUARPAppEventHandler evt_handler) {
    uint32_t ret_code  = 0;
    // The FW version code needs to verfied
    activeFW.build = 0;
    activeFW.major = g_fmna_info_cfg.fw_rev_major_number;
    activeFW.minor = g_fmna_info_cfg.fw_rev_minor_number;
    activeFW.release = g_fmna_info_cfg.fw_rev_revision_number;
    uarpCBs.fSendMessage = fmna_uarp_send_msg;
    uarpCBs.fUarpEventHandler = evt_handler;

    memcpy(dummySerial, fmna_crypto_get_serial_number_raw(), FMNA_SERIAL_NUMBER_LEN);
    dummySerial[FMNA_SERIAL_NUMBER_LEN] = 0;

    ret_code = UARPAppInit( &uarpAccessory,
                              g_fmna_info_cfg.manufaturer_name_str,
                              g_fmna_info_cfg.model_name_str,
                              (const char *)dummySerial,
                              g_fmna_info_cfg.hardware_rev_str,
                              &activeFW,
                              &stagedFW,
                              &uarpCBs );
    FMNA_ERROR_CHECK(ret_code);
}

fmna_ret_code_t fmna_uarp_control_point_append_to_rx_buffer(uint8_t const *data, uint16_t length) {
    if ((uarp_rx_buffer.len + length) > UARP_MAX_LEN) {
        memset(&uarp_rx_buffer, 0 , sizeof(uarp_rx_buffer));
        return FMNA_ERROR_INVALID_LENGTH;
    }

    memcpy(&(uarp_rx_buffer.data[uarp_rx_buffer.len]), data, length);
    uarp_rx_buffer.len += length;

    return FMNA_SUCCESS;
}

fmna_ret_code_t fmna_uarp_authorized_rx_handler(uint16_t conn_handle, uint8_t const * data, uint16_t length){
    fmna_ret_code_t ret_code = FMNA_SUCCESS;

    if (fmna_connection_is_fmna_paired() != true) {
        return FMNA_ERROR_INVALID_STATE;
    }

    if (uarp_conn_handle != conn_handle) {
        // drop the packet not the correct connection handle
        return FMNA_ERROR_INVALID_STATE;
    }

    ret_code = fmna_uarp_control_point_append_to_rx_buffer(&data[FRAGMENTED_FLAG_LENGTH], length - FRAGMENTED_FLAG_LENGTH);
    if (ret_code != FMNA_SUCCESS) {
        // TODO send an error?
        return ret_code;
    }

    if (data[FRAGMENTED_FLAG_INDEX] & FRAGMENTED_FLAG_FINAL) {
        // packet reassembled so give to UARP
        // Delay allowing gatt writes until the packet is processes so the rx buffer is not overwritten
        fmna_uarp_control_point_handle_rx();
    }

    return ret_code;
}
