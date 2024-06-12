package com.example.myapplication.scan;

import android.bluetooth.BluetoothDevice;

import com.example.myapplication.R;


public class ScanBluetoothDevice {
	public final BluetoothDevice device;
	public String name;
	public int rssi;
	public String macAddr;
    public int imageId;

	public ScanBluetoothDevice(BluetoothDevice device, int rssi) {
		this.device = device;
		this.name = device.getName();
		this.rssi = rssi;
		this.macAddr = device.getAddress();
        this.imageId = R.mipmap.ic_ble_device;
	}


	public void updateDevice(int rssi) {
		this.rssi = rssi;
	}
}
