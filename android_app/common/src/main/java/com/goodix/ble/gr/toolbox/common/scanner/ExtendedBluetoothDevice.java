package com.goodix.ble.gr.toolbox.common.scanner;

import android.bluetooth.BluetoothDevice;

import com.goodix.ble.gr.toolbox.common.R;

public class ExtendedBluetoothDevice {
    static final int NO_RSSI = -1000;
    public final BluetoothDevice device;
    public String name;
    public int rssi;
    public String macAddr;
    public boolean isBonded;
    public int imageId;
    public byte[] manuData;

    public ExtendedBluetoothDevice(final BluetoothDevice device) {
        this.device = device;
        this.name = device.getName();
        this.rssi = NO_RSSI;
        this.macAddr = device.getAddress();
        this.isBonded = true;
        this.imageId = R.mipmap.ic_device_other;
    }
}
