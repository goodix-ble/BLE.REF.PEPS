package com.goodix.ble.libble.chain;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;

public class GattCallbackInterceptor extends BluetoothGattCallback {
    private BluetoothGattCallback originGattCallback;

    public final void setOriginGattCallback(BluetoothGattCallback originGattCallback) {
        // avoid nesting
        if (originGattCallback == this) return;

        this.originGattCallback = originGattCallback;
    }

    /**
     * replace the the callback in GATT
     */
    public final void interceptGattCallback(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }

        try {
            Field callbackField = gatt.getClass().getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            Object o = callbackField.get(gatt);
            // avoid nesting
            if (o != this) {
                originGattCallback = (BluetoothGattCallback) o;
                callbackField.set(gatt, this);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static BluetoothGattCallback getGattCallback(BluetoothGatt gatt) {
        if (gatt == null) {
            return null;
        }

        try {
            Field callbackField = gatt.getClass().getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            Object o = callbackField.get(gatt);
            return (BluetoothGattCallback) o;
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (originGattCallback != null) {
            originGattCallback.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (originGattCallback != null) {
            originGattCallback.onServicesDiscovered(gatt, status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (originGattCallback != null) {
            originGattCallback.onCharacteristicRead(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (originGattCallback != null) {
            originGattCallback.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (originGattCallback != null) {
            originGattCallback.onCharacteristicChanged(gatt, characteristic);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        if (originGattCallback != null) {
            originGattCallback.onDescriptorRead(gatt, descriptor, status);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        if (originGattCallback != null) {
            originGattCallback.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        if (originGattCallback != null) {
            originGattCallback.onReliableWriteCompleted(gatt, status);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (originGattCallback != null) {
            originGattCallback.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (originGattCallback != null) {
            originGattCallback.onMtuChanged(gatt, mtu, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        if (originGattCallback != null) {
            originGattCallback.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        super.onPhyRead(gatt, txPhy, rxPhy, status);
        if (originGattCallback != null) {
            originGattCallback.onPhyRead(gatt, txPhy, rxPhy, status);
        }
    }

}
