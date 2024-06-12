package com.goodix.ble.libuihelper.ble.scanner;

import android.bluetooth.BluetoothDevice;

public class LeScannerReport {
    public static final int PDU_TYPE_ADV_IND = 0b0000;
    public static final int PDU_TYPE_ADV_NONCONN_IND = 0b0010;

    public long timestamp; // milliseconds
    public long timestampNano; // the nano part
    public int pduType;
    public final String address;
    public BluetoothDevice device;
    public int rssi;

    // ext info
    public boolean extended; // true - extended; false - legacy
    public int advertisingSetId; // 0xFF if no set id was is present.
    public int periodicAdvertisingInterval; // in units of 1.25ms. Valid range is 6 (7.5ms) to 65536 (81918.75ms). 0x00 means periodic advertising interval is not present.

    private byte[] payload;
    public final AdvDataParser advData = new AdvDataParser();

    public LeScannerReport(String address) {
        this.address = address;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
        advData.setAdvPayload(payload);
    }
}
