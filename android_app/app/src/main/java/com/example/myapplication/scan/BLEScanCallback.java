package com.example.myapplication.scan;


import android.bluetooth.BluetoothDevice;

public interface  BLEScanCallback {

    void onScanTimeOut(boolean found);

    void onScanResult(BluetoothDevice device, int rssi);

    void onScanError();
}
