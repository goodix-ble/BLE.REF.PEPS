package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.goodix.ble.libble.v2.gb.procedure.GBProcedureRssiRead;
import com.goodix.ble.libble.v2.impl.BleGattX;

public class RssiRead extends BleBaseProcedure implements GBProcedureRssiRead {

    private CB cb;
    private int rssi;

    @Override
    public int getRssi() {
        return rssi;
    }

    @Override
    protected int doWork2() {
        if (!gattX.isConnected()) {
            finishedWithError("Failed to read RSSI. The connection is not established.");
            return 0;
        }

        BluetoothGatt gatt = gattX.getGatt();

        if (gatt == null) {
            finishedWithError("Abort reading RSSI for null gatt.");
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        // 发起连接
        if (gatt.readRemoteRssi()) {
            return (COMMUNICATION_TIMEOUT);
        } else {
            finishedWithError("Failed to read RSSI.");
        }
        return 0;
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
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                RssiRead.this.rssi = rssi;
                finishedWithDone();
            } else {
                finishedWithError("Failed to read RSSI: " + BleGattX.gattStatusToString(status));
            }
        }
    }
}
