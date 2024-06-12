package com.goodix.ble.libuihelper.ble.scanner;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libcomx.util.HexReader;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class AdvDataParser {
    public static final int TYPE_FLAGS = 0x01;

    public static final int TYPE_INCOMPLETE_LIST_OF_16BIT_SERVICE_CLASS_UUIDS = 0x02;// Incomplete List of 16-bit Service Class UUIDs
    public static final int TYPE_COMPLETE_LIST_OF_16BIT_SERVICE_CLASS_UUIDS = 0x03;// Complete List of 16-bit Service Class UUIDs
    public static final int TYPE_INCOMPLETE_LIST_OF_32BIT_SERVICE_CLASS_UUIDS = 0x04;// Incomplete List of 32-bit Service Class UUIDs
    public static final int TYPE_COMPLETE_LIST_OF_32BIT_SERVICE_CLASS_UUIDS = 0x05;// Complete List of 32-bit Service Class UUIDs
    public static final int TYPE_INCOMPLETE_LIST_OF_128BIT_SERVICE_CLASS_UUIDS = 0x06;// Incomplete List of 128-bit Service Class UUIDs
    public static final int TYPE_COMPLETE_LIST_OF_128BIT_SERVICE_CLASS_UUIDS = 0x07;// Complete List of 128-bit Service Class UUIDs

    public static final int TYPE_SHORTENED_LOCAL_NAME = 0x08;// Shortened Local Name
    public static final int TYPE_COMPLETE_LOCAL_NAME = 0x09;// Complete Local Name

    public static final int TYPE_TX_POWER_LEVEL = 0x0A;// Tx Power Level
    public static final int TYPE_CLASS_OF_DEVICE = 0x0D;// Class of Device
    public static final int TYPE_SIMPLE_PAIRING_HASH_C = 0x0E;// Simple Pairing Hash C
    public static final int TYPE_SIMPLE_PAIRING_HASH_C192 = 0x0E;// Simple Pairing Hash C-192
    public static final int TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F;// Simple Pairing Randomizer R
    public static final int TYPE_SIMPLE_PAIRING_RANDOMIZER_R192 = 0x0F;// Simple Pairing Randomizer R-192
    public static final int TYPE_DEVICE_ID = 0x10;// Device ID
    public static final int TYPE_SECURITY_MANAGER_TK_VALUE = 0x10;// Security Manager TK Value
    public static final int TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS = 0x11;// Security Manager Out of Band Flags
    public static final int TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;// Slave Connection Interval Range
    public static final int TYPE_LIST_OF_16BIT_SERVICE_SOLICITATION_UUIDS = 0x14;// List of 16-bit Service Solicitation UUIDs
    public static final int TYPE_LIST_OF_128BIT_SERVICE_SOLICITATION_UUIDS = 0x15;// List of 128-bit Service Solicitation UUIDs
    public static final int TYPE_SERVICE_DATA = 0x16;// Service Data
    public static final int TYPE_SERVICE_DATA_16BIT_UUID = 0x16;// Service Data - 16-bit UUID
    public static final int TYPE_PUBLIC_TARGET_ADDRESS = 0x17;// Public Target Address
    public static final int TYPE_RANDOM_TARGET_ADDRESS = 0x18;// Random Target Address
    public static final int TYPE_APPEARANCE = 0x19;// Appearance
    public static final int TYPE_ADVERTISING_INTERVAL = 0x1A;// Advertising Interval
    public static final int TYPE_LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B;// LE Bluetooth Device Address
    public static final int TYPE_LE_ROLE = 0x1C;// LE Role
    public static final int TYPE_SIMPLE_PAIRING_HASH_C256 = 0x1D;// Simple Pairing Hash C-256
    public static final int TYPE_SIMPLE_PAIRING_RANDOMIZER_R256 = 0x1E;// Simple Pairing Randomizer R-256
    public static final int TYPE_LIST_OF_32BIT_SERVICE_SOLICITATION_UUIDS = 0x1F;// List of 32-bit Service Solicitation UUIDs
    public static final int TYPE_SERVICE_DATA_32BIT_UUID = 0x20;// Service Data - 32-bit UUID
    public static final int TYPE_SERVICE_DATA_128BIT_UUID = 0x21;// Service Data - 128-bit UUID
    public static final int TYPE_LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE = 0x22;// LE Secure Connections Confirmation Value
    public static final int TYPE_LE_SECURE_CONNECTIONS_RANDOM_VALUE = 0x23;// LE Secure Connections Random Value
    public static final int TYPE_URI = 0x24;// URI
    public static final int TYPE_INDOOR_POSITIONING = 0x25;// Indoor Positioning
    public static final int TYPE_TRANSPORT_DISCOVERY_DATA = 0x26;// Transport Discovery Data
    public static final int TYPE_LE_SUPPORTED_FEATURES = 0x27;// LE Supported Features
    public static final int TYPE_CHANNEL_MAP_UPDATE_INDICATION = 0x28;// Channel Map Update Indication
    public static final int TYPE_PB_ADV = 0x29;// PB-ADV
    public static final int TYPE_MESH_MESSAGE = 0x2A;// Mesh Message
    public static final int TYPE_MESH_BEACON = 0x2B;// Mesh Beacon
    public static final int TYPE_BIG_INFO = 0x2C;// BIGInfo
    public static final int TYPE_BROADCAST_CODE = 0x2D;// Broadcast_Code
    public static final int TYPE_3D_INFORMATION_DATA = 0x3D;// 3D Information Data
    public static final int TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;// Manufacturer Specific Data

    public static boolean seekData(final HexReader payloadReader, final int type) {
        if (payloadReader == null) {
            return false;
        }

        byte[] payload = payloadReader.getBuffer();

        for (int pos = 0; pos < payload.length; pos++) {
            int len = payload[pos] & 0xFF;
            if (len > 0) {
                int t = payload[pos + 1] & 0xFF;
                if (t == type) {
                    payloadReader.setRange(pos + 2, len - 1);
                }
                return true;
            }
            pos += len;
        }

        return false;
    }

    public byte[] advPayload;
    private HexReader payloadReader;
    private String localName;
    private int flags;
    private List<UUID> serviceUuids = null;

    public void setAdvPayload(byte[] advPayload) {
        this.advPayload = advPayload;
    }

    public String getLocalName() {
        parsePayload(advPayload);
        return localName;
    }

    public int getFlags() {
        parsePayload(advPayload);
        return flags;
    }

    public List<UUID> getServiceUuids() {
        parsePayload(advPayload);
        return serviceUuids;
    }

    private void parsePayload(byte[] payload) {
        if (payloadReader != null && payloadReader.getBuffer() == payload) {
            return;
        }

        flags = 0;
        localName = null;
        if (serviceUuids != null) serviceUuids.clear();

        if (payload == null) return;

        if (payloadReader == null) {
            payloadReader = new HexReader(payload);
        } else {
            payloadReader.setBuffer(payload);
        }

        for (int pos = 0; pos < payload.length; pos++) {

            int len = 0xFF & payload[pos];
            if (len > 0) {
                int valueLen = len - 1;
                int valueType = 0xFF & payload[pos + 1];
                payloadReader.setRange(pos + 2, valueLen);
                switch (valueType) {
                    case TYPE_FLAGS: // Flags
                        flags = payloadReader.get(1);
                        break;
                    case TYPE_INCOMPLETE_LIST_OF_16BIT_SERVICE_CLASS_UUIDS:
                    case TYPE_COMPLETE_LIST_OF_16BIT_SERVICE_CLASS_UUIDS:
                        if (serviceUuids == null) serviceUuids = new ArrayList<>();
                        for (int i = 0; i < valueLen; i += 2) {
                            serviceUuids.add(BleUuid.from(payloadReader.get(2)));
                        }
                        break;
                    case TYPE_INCOMPLETE_LIST_OF_32BIT_SERVICE_CLASS_UUIDS:
                    case TYPE_COMPLETE_LIST_OF_32BIT_SERVICE_CLASS_UUIDS:
                        if (serviceUuids == null) serviceUuids = new ArrayList<>();
                        for (int i = 0; i < valueLen; i += 4) {
                            serviceUuids.add(BleUuid.from(payloadReader.get(4)));
                        }
                        break;
                    case TYPE_INCOMPLETE_LIST_OF_128BIT_SERVICE_CLASS_UUIDS:
                    case TYPE_COMPLETE_LIST_OF_128BIT_SERVICE_CLASS_UUIDS:
                        if (serviceUuids == null) serviceUuids = new ArrayList<>();
                        for (int i = 0; i < valueLen; i += 2) {
                            long lsb = payloadReader.getLong(8, false);
                            long msb = payloadReader.getLong(8, false);
                            serviceUuids.add(new UUID(msb, lsb));
                        }
                        break;
                    case TYPE_SHORTENED_LOCAL_NAME: // Shortened Local Name
                    case TYPE_COMPLETE_LOCAL_NAME: // Complete Local Name
                        localName = payloadReader.getString(Charset.defaultCharset(), valueLen);
                        break;
                }
            }

            pos += len;
        }
    }
}


