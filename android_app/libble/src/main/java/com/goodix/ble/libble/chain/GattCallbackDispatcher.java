package com.goodix.ble.libble.chain;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

public class GattCallbackDispatcher extends BluetoothGattCallback {
    private CopyOnWriteArrayList<BluetoothGattCallback> gattCallbackList = new CopyOnWriteArrayList<>();

    public void register(BluetoothGattCallback callback) {
        gattCallbackList.addIfAbsent(callback);
    }

    public void remove(BluetoothGattCallback callback) {
        gattCallbackList.remove(callback);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onServicesDiscovered(gatt, status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onCharacteristicRead(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onDescriptorRead(gatt, descriptor, status);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onReliableWriteCompleted(gatt, status);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onMtuChanged(gatt, mtu, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyRead(gatt, txPhy, rxPhy, status);
        for (BluetoothGattCallback callback : gattCallbackList) {
            callback.onPhyRead(gatt, txPhy, rxPhy, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Keep
    public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout,
                                    int status) {
        for (BluetoothGattCallback callback : gattCallbackList) {
            try {
                for (Method method : callback.getClass().getDeclaredMethods()) {
                    if ("onConnectionUpdated".equals(method.getName())) {
                        method.invoke(callback, gatt, interval, latency, timeout, status);
                        break;
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
