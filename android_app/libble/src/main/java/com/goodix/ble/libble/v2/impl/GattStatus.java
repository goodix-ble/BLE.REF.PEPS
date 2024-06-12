package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothGatt;

/**
 * gatt status:
 * http://androidxref.com/9.0.0_r3/xref/system/bt/stack/include/gatt_api.h#29
 * line 29 to line 76
 * <p>
 * gatt disconnect reason:
 * http://androidxref.com/9.0.0_r3/xref/system/bt/stack/include/gatt_api.h#116
 * line 116 to line 129
 */
public class GattStatus {
    public static String gattDisconnectReasonToString(final int error) {
        switch (error) {
            case BluetoothGatt.GATT_SUCCESS:
                return "GATT CONN UNKNOWN";
            case 0x01:
                return "GATT CONN L2C FAILURE";
            case 0x08:
                return "GATT CONN TIMEOUT";
            case 0x13:
                return "GATT CONN TERMINATE PEER USER";
            case 0x16:
                return "GATT CONN TERMINATE LOCAL HOST";
            case 0x3E:
                return "GATT CONN FAIL ESTABLISH";
            case 0x22:
                return "GATT CONN LMP TIMEOUT";
            case 0x0100:
                return "GATT CONN CANCEL ";
            default:
                return gattStatusToString(error);
        }
    }

    public static String gattStatusToString(final int code) {
        switch (code) {
            case 0x00:
                return "GATT SUCCESS"; // GATT_ENCRYPED_MITM too
            case 0x01:
                return "GATT INVALID HANDLE";
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                return "GATT READ NOT PERMITTED";
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                return "GATT WRITE NOT PERMITTED";
            case 0x04:
                return "GATT INVALID PDU";
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                return "GATT INSUFFICIENT AUTHENTICATION";
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                return "GATT REQUEST NOT SUPPORTED";
            case BluetoothGatt.GATT_INVALID_OFFSET:
                return "GATT INVALID OFFSET";
            case 0x08:
                return "GATT INSUF AUTHORIZATION";
            case 0x09:
                return "GATT PREPARE Q FULL";
            case 0x0a:
                return "GATT NOT FOUND";
            case 0x0b:
                return "GATT NOT LONG";
            case 0x0c:
                return "GATT INSUF KEY SIZE";
            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                return "GATT INVALID ATTRIBUTE LENGTH";
            case 0x0e:
                return "GATT ERR UNLIKELY";
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                return "GATT INSUFFICIENT ENCRYPTION";
            case 0x10:
                return "GATT UNSUPPORT GRP TYPE";
            case 0x11:
                return "GATT INSUF RESOURCE";
            case 0x80:
                return "GATT NO RESOURCES";
            case 0x81:
                return "GATT INTERNAL ERROR";
            case 0x82:
                return "GATT WRONG STATE";
            case 0x83:
                return "GATT DB FULL";
            case 0x84:
                return "GATT BUSY";
            case 0x85:
                return "GATT ERROR";
            case 0x86:
                return "GATT CMD STARTED";
            case 0x87:
                return "GATT ILLEGAL PARAMETER";
            case 0x88:
                return "GATT PENDING";
            case 0x89:
                return "GATT AUTH FAIL";
            case 0x8a:
                return "GATT MORE";
            case 0x8b:
                return "GATT INVALID CFG";
            case 0x8c:
                return "GATT SERVICE STARTED";
            case 0x8d:
                return "GATT ENCRYPED NO MITM";
            case 0x8e:
                return "GATT NOT ENCRYPTED";
            case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                return "GATT CONNECTION CONGESTED";
            case 0x90:
                return "GATT DUP REG";      /* 0x90 */
            case 0x91:
                return "GATT ALREADY OPEN"; /* 0x91 */
            case 0x92:
                return "GATT CANCEL";       /* 0x92 */
            /* 0xE0 ~ 0xFC reserved for future use */
            case 0xFD:
                /* Client Characteristic Configuration Descriptor Improperly Configured */
                return "GATT CCC CFG ERR";
            case 0xFE:
                /* Procedure Already in progress */
                return "GATT PRC IN PROGRESS";
            case 0xFF:
                /* Attribute value out of range */
                return "GATT OUT OF RANGE";
            case BluetoothGatt.GATT_FAILURE:
                /* A GATT operation failed, errors other than the above */
                return "GATT FAILURE";
            default:
                return "UNKNOWN (" + code + ")";
        }
    }
}
