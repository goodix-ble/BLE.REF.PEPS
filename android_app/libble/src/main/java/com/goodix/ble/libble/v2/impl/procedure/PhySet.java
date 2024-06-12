package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import com.goodix.ble.libble.v2.gb.pojo.GBPhy;
import com.goodix.ble.libble.v2.impl.BleGattX;

public class PhySet extends BleBaseProcedure {

    private CB cb;
    private int tx;
    private int rx;
    private int opt;

    public void setPhy(int tx, int rx, int opt) {
        this.tx = tx;
        this.rx = rx;
        this.opt = opt;
    }

    @Override
    protected int doWork2() {
        if (!gattX.isConnected()) {
            finishedWithError("Failed to set PHY. The connection is not established.");
            return 0;
        }

        BluetoothGatt gatt = gattX.getGatt();

        if (gatt == null) {
            finishedWithError("Abort setting preferred PHY for null gatt.");
            return 0;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            finishedWithError("Setting preferred PHY requires API level 26.");
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        // 发起连接
        gatt.setPreferredPhy(convertToPhyMask(tx), convertToPhyMask(rx), convertToPhyOpt(opt));
        return (GATT_TIMEOUT);
    }

    @Override
    protected void onCleanup() {
        if (cb != null && gattX != null) {
            gattX.remove(cb);
        }
        super.onCleanup();
    }

    private int convertToPhyMask(int gbPhy) {
        int ret = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if ((gbPhy & GBPhy.LE_1M) != 0) ret |= BluetoothDevice.PHY_LE_1M;
            if ((gbPhy & GBPhy.LE_2M) != 0) ret |= BluetoothDevice.PHY_LE_2M;
            if ((gbPhy & GBPhy.LE_CODED) != 0) ret |= BluetoothDevice.PHY_LE_CODED;
        }
        return ret;
    }

    private int convertToPhyOpt(int gbPhy) {
        int ret = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if ((gbPhy & GBPhy.LE_OPT_S2) != 0) ret = BluetoothDevice.PHY_OPTION_S2;
            if ((gbPhy & GBPhy.LE_OPT_S8) != 0) ret = BluetoothDevice.PHY_OPTION_S8;
        }
        return ret;
    }

    class CB extends BluetoothGattCallback {

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                finishedWithDone();
            } else {
                finishedWithError("Failed to set preferred PHY: " + BleGattX.gattStatusToString(status));
            }
        }
    }
}
