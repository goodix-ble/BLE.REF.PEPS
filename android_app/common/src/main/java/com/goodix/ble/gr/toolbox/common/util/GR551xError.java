package com.goodix.ble.gr.toolbox.common.util;

import android.util.SparseArray;

public class GR551xError {
    private static SparseArray<CharSequence> attErrorMap = new SparseArray<>(256);
    private static SparseArray<CharSequence> sdkErrorMap = new SparseArray<>(256);

    public static CharSequence getAttError(int errorCode) {
        CharSequence errorMsg = attErrorMap.get(errorCode);
        if (errorMsg == null) {
            errorMsg = "UNKNOWN 0x" + Integer.toHexString(errorCode);
        }
        return errorMsg;
    }

    public static CharSequence getSdkError(int errorCode) {
        CharSequence errorMsg = sdkErrorMap.get(errorCode);
        if (errorMsg == null) {
            errorMsg = "UNKNOWN 0x" + Integer.toHexString(errorCode);
        }
        return errorMsg;
    }

    static {
        // 对应文件 components\sdk\ble_error.h
        // 查找模式： #define [A-Z0-9_]* *(0x[0-9A-Z]{2}) */\*\*< ([\u0020-\u007E]*) \*/
        // 查找模式： #define [A-Z0-9_]* *(0x[0-9A-Z]{2}) */\*\*< ([\x20-\x7E]*) \*/
        // 替换字串： ap[$1] = "$2";
        // 使用替换字串： map.put($1, "$2");
        attErrorMap.put(0x00, "Operation is Successful.");
        //@brief ATT Specific Error. */
        attErrorMap.put(0x01, "The given attribute handle was not valid on this server.");
        attErrorMap.put(0x02, "The attribute cannot be read.");
        attErrorMap.put(0x03, "The attribute cannot be written.");
        attErrorMap.put(0x04, "The attribute PDU was invalid.");
        attErrorMap.put(0x05, "The attribute requires authentication before it can be read or written.");
        attErrorMap.put(0x06, "Attribute server does not support the request received from the client.");
        attErrorMap.put(0x07, "Offset specified was past the end of the attribute.");
        attErrorMap.put(0x08, "The attribute requires authorization before it can be read or written.");
        attErrorMap.put(0x09, "Too many prepare writes have been queued.");
        attErrorMap.put(0x0A, "No attribute found within the given attribute handle range.");
        attErrorMap.put(0x0B, "The attribute cannot be read using the Read Blob Request.");
        attErrorMap.put(0x0C, "The Encryption Key Size used for encrypting this link is insufficient.");
        attErrorMap.put(0x0D, "The attribute value length is invalid for the operation.");
        attErrorMap.put(0x0E, "The attribute request has encountered an unlikely error, so the request could not be completed as requested.");
        attErrorMap.put(0x0F, "The attribute requires encryption before it can be read or written.");
        attErrorMap.put(0x10, "The attribute type is not a supported grouping attribute as defined by a higher layer specification.");
        attErrorMap.put(0x11, "Insufficient resources to complete the request.");

        //#if defined(GR551xx_D0)
        attErrorMap.put(0x12, "The server requests the client to rediscover the database.");
        attErrorMap.put(0x13, "The attribute parameter value was not allowed.");
        //#endif

        //@brief L2CAP Specific Error. */
        attErrorMap.put(0x30, "Message cannot be sent because connection is lost (disconnected).");
        attErrorMap.put(0x31, "Invalid PDU length exceeds MTU.");
        attErrorMap.put(0x32, "Invalid PDU length exceeds MPS.");
        attErrorMap.put(0x33, "Invalid Channel ID.");
        attErrorMap.put(0x34, "Invalid PDU.");
        attErrorMap.put(0x35, "Connection refused because no resources available.");
        attErrorMap.put(0x36, "Connection refused because of insufficient authentication.");
        attErrorMap.put(0x37, "Connection refused because of insufficient authorization.");
        attErrorMap.put(0x38, "Connection refused because of insufficient encryption key size.");
        attErrorMap.put(0x39, "Connection refused because of insufficient encryption.");
        attErrorMap.put(0x3A, "Connection refused because LE_PSM is not supported.");
        attErrorMap.put(0x3B, "No more credit.");
        attErrorMap.put(0x3C, "Command not understood by peer device.");
        attErrorMap.put(0x3D, "Credit error: invalid number of credit received.");
        attErrorMap.put(0x3E, "Channel identifier already allocated.");

        //@brief GAP Specific Error. */
        attErrorMap.put(0x40, "Invalid parameters set.");
        attErrorMap.put(0x41, "Problem with protocol exchange, resulting in unexpected responses.");
        attErrorMap.put(0x42, "Request not supported by software configuration.");
        attErrorMap.put(0x43, "Request not allowed in current state.");
        attErrorMap.put(0x44, "Requested operation canceled.");
        attErrorMap.put(0x45, "Requested operation timeout.");
        attErrorMap.put(0x46, "Link connection is lost during operation.");
        attErrorMap.put(0x47, "Search algorithm finished, but no result found.");
        attErrorMap.put(0x48, "Request rejected by peer device.");
        attErrorMap.put(0x49, "Problem with privacy configuration.");
        attErrorMap.put(0x4A, "Duplicate or invalid advertising data.");
        attErrorMap.put(0x4B, "Insufficient resources.");
        attErrorMap.put(0x4C, "Unexpected error.");
        attErrorMap.put(0x4D, "Feature mismatch.");

        //@brief GATT Specific Error. */
        attErrorMap.put(0x50, "Problem with ATTC protocol response.");
        attErrorMap.put(0x51, "Error in service search.");
        attErrorMap.put(0x52, "Invalid write data.");
        attErrorMap.put(0x53, "Signed write error.");
        attErrorMap.put(0x54, "No attribute client defined.");
        attErrorMap.put(0x55, "No attribute server defined.");
        attErrorMap.put(0x56, "Permission set in service/attribute is invalid.");
        attErrorMap.put(0x57, "GATT browse no any more contents.");


        //@brief LL Specific Error. */
        attErrorMap.put(0x91, "Unknown HCI Command.");
        attErrorMap.put(0x92, "Unknown Connection Identifier.");
        attErrorMap.put(0x93, "Hardware Failure.");
        attErrorMap.put(0x94, "BT Page Timeout.");
        attErrorMap.put(0x95, "Authentication failure.");
        attErrorMap.put(0x96, "Pin code missing.");
        attErrorMap.put(0x97, "Memory capacity exceed.");
        attErrorMap.put(0x98, "Connection Timeout.");
        attErrorMap.put(0x99, "Connection limit Exceed.");
        attErrorMap.put(0x9A, "Synchronous Connection limit exceed.");
        attErrorMap.put(0x9B, "ACL Connection exits.");
        attErrorMap.put(0x9C, "Command Disallowed.");
        attErrorMap.put(0x9D, "Connection rejected due to limited resources.");
        attErrorMap.put(0x9E, "Connection rejected due to insecurity issues.");
        attErrorMap.put(0x9F, "Connection rejected due to unacceptable BD Addr.");
        attErrorMap.put(0xA0, "Connection rejected due to Accept connection timeout.");
        attErrorMap.put(0xA1, "Not Supported.");
        attErrorMap.put(0xA2, "Invalid parameters.");
        attErrorMap.put(0xA3, "Remote user terminate connection.");
        attErrorMap.put(0xA4, "Remote device lost connection due to low resources.");
        attErrorMap.put(0xA5, "Remote device lost  connection due to power failure.");
        attErrorMap.put(0xA6, "Connection terminated by local host.");
        attErrorMap.put(0xA7, "Repeated attempts.");
        attErrorMap.put(0xA8, "Pairing not Allowed.");
        attErrorMap.put(0xA9, "Unknown PDU Error.");
        attErrorMap.put(0xAA, "Unsupported remote feature.");
        attErrorMap.put(0xAB, "SCO Offset rejected.");
        attErrorMap.put(0xAC, "SCO Interval Rejected.");
        attErrorMap.put(0xAD, "SCO air mode Rejected.");
        attErrorMap.put(0xAE, "Invalid LMP parameters.");
        attErrorMap.put(0xAF, "Unspecified error.");
        attErrorMap.put(0xB0, "Unsupported LMP Parameter value.");
        attErrorMap.put(0xB1, "Role Change Not allowed.");
        attErrorMap.put(0xB2, "LMP Response timeout.");
        attErrorMap.put(0xB3, "LMP Collision.");
        attErrorMap.put(0xB4, "LMP PDU not allowed.");
        attErrorMap.put(0xB5, "Encryption mode not accepted.");
        attErrorMap.put(0xB6, "Link Key cannot be changed.");
        attErrorMap.put(0xB7, "Quality of Service not supported.");
        attErrorMap.put(0xB8, "Error, instant passed.");
        attErrorMap.put(0xB9, "Pairing with unit key not supported.");
        attErrorMap.put(0xBA, "Transaction collision.");
        attErrorMap.put(0xBC, "Quality of Service not supported.");
        attErrorMap.put(0xBD, "Quality of Service rejected.");
        attErrorMap.put(0xBE, "Channel class not supported.");
        attErrorMap.put(0xBF, "Insufficient security.");
        attErrorMap.put(0xC0, "Parameters out of mandatory range.");
        attErrorMap.put(0xC2, "Role switch pending.");
        attErrorMap.put(0xC4, "Reserved slot violation.");
        attErrorMap.put(0xC5, "Role Switch failed.");
        attErrorMap.put(0xC6, "Error: EIR too large.");
        attErrorMap.put(0xC7, "Simple pairing not supported by host.");
        attErrorMap.put(0xC8, "Host pairing is busy.");
        attErrorMap.put(0xCA, "Controller is busy.");
        attErrorMap.put(0xCB, "Unacceptable connection initialization.");
        attErrorMap.put(0xCC, "Advertising Timeout.");
        attErrorMap.put(0xCD, "Connection Terminated due to a MIC failure.");
        attErrorMap.put(0xCE, "Connection failed to be established.");

        sdkErrorMap.put(0x0000, "Successful.");
        sdkErrorMap.put(0x0001, "Invalid parameter supplied.");
        sdkErrorMap.put(0x0002, "Invalid pointer supplied.");
        sdkErrorMap.put(0x0003, "Invalid connection index supplied.");
        sdkErrorMap.put(0x0004, "Invalid handle supplied.");
        sdkErrorMap.put(0x0005, "Maximum SDK profile count exceeded.");
        sdkErrorMap.put(0x0006, "SDK is busy internally.");
        sdkErrorMap.put(0x0007, "Timer is insufficient.");
        sdkErrorMap.put(0x0008, "NVDS is not initiated.");
        sdkErrorMap.put(0x0009, "Item not found in list.");
        sdkErrorMap.put(0x000a, "Item already existed in list.");
        sdkErrorMap.put(0x000b, "List is full.");
        sdkErrorMap.put(0x000c, "Internal SDK error.");
        sdkErrorMap.put(0x000d, "The buffer's length is not enough.");
        sdkErrorMap.put(0x000e, "Invalid data length supplied.");
        sdkErrorMap.put(0x000f, "Operation is disallowed.");
        sdkErrorMap.put(0x0010, "Not enough resources for operation.");
        sdkErrorMap.put(0x0011, "Request not supported.");
        sdkErrorMap.put(0x0012, "Offset exceeds the current attribute value length.");
        sdkErrorMap.put(0x0013, "Invalid length of the attribute value.");
        sdkErrorMap.put(0x0014, "Permission set in service/attribute is invalid.");
        sdkErrorMap.put(0x0015, "Invalid advertising index supplied.");
        sdkErrorMap.put(0x0016, "Invalid advertising data type supplied.");
        sdkErrorMap.put(0x0017, "Invalid PSM number.");
        sdkErrorMap.put(0x0018, "The PSM number has been registered.");
        sdkErrorMap.put(0x0019, "The maximum PSM number limit is exceeded.");
        sdkErrorMap.put(0x001A, "Notification is NOT enabled.");
        sdkErrorMap.put(0x001B, "Indication is NOT enabled.");
        sdkErrorMap.put(0x001C, "Disconnection occurs.");
        sdkErrorMap.put(0x001D, "Invalid address supplied.");
        //#if defined(GR551xx_D0)
        sdkErrorMap.put(0x001E, "Cache feature is not enabled.");
        //#endif
        sdkErrorMap.put(0x001F, "Invalid advertising interval supplied.");
        sdkErrorMap.put(0x0020, "Invalid discovery mode supplied.");
        sdkErrorMap.put(0x0021, "Invalid advertising parameters supplied.");
        sdkErrorMap.put(0x0022, "Invalid peer address supplied.");
        sdkErrorMap.put(0x0023, "Legacy advertising data not set.");
        sdkErrorMap.put(0x0024, "Periodic advertising data not set.");
        sdkErrorMap.put(0x0025, "Extended scan response data not set.");
        sdkErrorMap.put(0x0026, "Invalid duration parameter_supplied.");
        sdkErrorMap.put(0x0027, "Invalid periodic synchronization index supplied.");
        sdkErrorMap.put(0x0028, "Invalid CID supplied.");
        sdkErrorMap.put(0x0029, "Invalid channel number supplied.");
        sdkErrorMap.put(0x002A, "Not enough credits.");

        sdkErrorMap.put(0x0080, "Application error.");
    }

}
