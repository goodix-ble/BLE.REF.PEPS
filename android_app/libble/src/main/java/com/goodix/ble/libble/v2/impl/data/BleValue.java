package com.goodix.ble.libble.v2.impl.data;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BleValue {
    @NonNull
    private BluetoothGatt gatt;
    @NonNull
    private BluetoothGattCharacteristic characteristic;
    @Nullable
    private byte[] data;
    private int status;

    public BleValue(@NonNull final BluetoothGatt gatt,
                    @NonNull BluetoothGattCharacteristic characteristic) {
        this.gatt = gatt;
        this.characteristic = characteristic;
        // byte[] value = characteristic.getValue();
        // this.data = Arrays.copyOf(value, value.length);
        this.data = characteristic.getValue();
    }

    public BleValue(@NonNull final BluetoothGatt gatt,
                    @NonNull BluetoothGattCharacteristic characteristic,
                    final int status) {
        this.gatt = gatt;
        this.characteristic = characteristic;
        this.data = characteristic.getValue();
        this.status = status;
    }

    public BleValue(@NonNull final BluetoothGatt gatt,
                    @NonNull final BluetoothGattCharacteristic characteristic,
                    @Nullable final byte[] data,
                    final int status) {
        this.gatt = gatt;
        this.characteristic = characteristic;
        this.data = data;
        this.status = status;
    }

    @NonNull
    public BluetoothGatt getGatt() {
        return gatt;
    }

    @NonNull
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    @Nullable
    public byte[] getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }
}
