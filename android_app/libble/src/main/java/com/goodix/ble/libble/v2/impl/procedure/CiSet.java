package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import androidx.annotation.Keep;

import com.goodix.ble.libble.v2.gb.pojo.GBCI;
import com.goodix.ble.libble.v2.impl.BleGattX;

public class CiSet extends BleBaseProcedure {

    private CB cb;
    private int priority;

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    protected int doWork2() {
        if (priority > GBCI.PRIORITY_LOW) {
            finishedWithError("Invalid connection priority: " + priority);
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to set CI. The connection is not established.");
            return 0;
        }

        BluetoothGatt gatt = gattX.getGatt();

        if (gatt == null) {
            finishedWithError("Abort setting connection priority for null gatt.");
            return 0;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            finishedWithError("Setting connection priority requires API level 21.");
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        switch (priority) {
            case GBCI.PRIORITY_DEFAULT:
                priority = BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
                break;
            case GBCI.PRIORITY_HIGH:
                priority = BluetoothGatt.CONNECTION_PRIORITY_HIGH;
                break;
            case GBCI.PRIORITY_LOW:
                priority = BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
                break;
        }

        // 发起连接
        if (gatt.requestConnectionPriority(priority)) {
            return (GATT_TIMEOUT);
        } else {
            finishedWithError("Failed to set connection priority.");
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

        @SuppressWarnings("unused")
        @Keep
        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout,
                                        int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                finishedWithDone();
            } else if (status == 0x3b) { // HCI_ERR_UNACCEPT_CONN_INTERVAL
                finishedWithError("Connection parameters update failed with status: " +
                        "UNACCEPT CONN INTERVAL (0x3b) (interval: " + (interval * 1.25) + "ms, " +
                        "latency: " + latency + ", timeout: " + (timeout * 10) + "ms)");
            } else {
                finishedWithError("Connection parameters update failed with " +
                        "status " + BleGattX.gattStatusToString(status) + " (interval: " + (interval * 1.25) + "ms, " +
                        "latency: " + latency + ", timeout: " + (timeout * 10) + "ms)");
            }
        }
    }
}
