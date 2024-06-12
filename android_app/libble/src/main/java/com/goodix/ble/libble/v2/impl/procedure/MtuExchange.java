package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import com.goodix.ble.libble.v2.impl.BleGattX;

public class MtuExchange extends BleBaseProcedure {

    private CB cb;
    private int mtu;

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    @Override
    protected int doWork2() {
        if (mtu < 23) {
            finishedWithError("MTU is less than 23: " + mtu);
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to exchange MTU. The connection is not established.");
            return 0;
        }

        BluetoothGatt gatt = gattX.getGatt();

        if (gatt == null) {
            finishedWithError("Abort requesting MTU for null gatt.");
            return 0;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            finishedWithError("Requesting MTU requires API level 21.");
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        // 发起连接
        if (gatt.requestMtu(mtu)) {
            return (GATT_TIMEOUT);
        } else {
            finishedWithError("Failed to request MTU.");
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
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (MtuExchange.this.mtu == mtu) {
                    finishedWithDone();
                } else {
                    finishedWithError("Failed to change MTU to " + MtuExchange.this.mtu + " , now it's " + mtu);
                }
            } else {
                if (gattX != null && MtuExchange.this.mtu == mtu) {
                    finishedWithDone();
                } else {
                    finishedWithError("Failed to request MTU " + MtuExchange.this.mtu + " : " + BleGattX.gattStatusToString(status));
                }
            }
        }
    }
}
