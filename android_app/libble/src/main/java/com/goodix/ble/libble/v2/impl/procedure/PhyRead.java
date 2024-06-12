package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import com.goodix.ble.libble.v2.impl.BleGattX;

public class PhyRead extends BleBaseProcedure {

    private CB cb;

    @Override
    protected int doWork2() {
        if (!gattX.isConnected()) {
            finishedWithError("Failed to read PHY. The connection is not established.");
            return 0;
        }

        BluetoothGatt gatt = gattX.getGatt();

        if (gatt == null) {
            finishedWithError("Abort Reading PHY for null gatt.");
            return 0;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            finishedWithError("Reading PHY requires API level 26.");
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        // 发起连接
        gatt.readPhy();
        return (COMMUNICATION_TIMEOUT);
    }

    @Override
    protected void onCleanup() {
        if (cb != null && gattX != null) {
            gattX.remove(cb);
        }
        super.onCleanup();
    }

    class CB extends BluetoothGattCallback {

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                finishedWithDone();
            } else {
                finishedWithError("Failed to read PHY: " + BleGattX.gattStatusToString(status));
            }
        }
    }
}
