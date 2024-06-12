package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureRead;
import com.goodix.ble.libble.v2.impl.BleCharacteristicX;
import com.goodix.ble.libble.v2.impl.BleGattX;
import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.IEventListener;

public class CharacteristicRead extends BleBaseProcedure implements GBGattProcedureRead, IEventListener<BleIntState> {
    private static final String TAG = "CharacteristicRead";

    private BleCharacteristicX targetCharacteristic;

    private InnerCB cb;
    private byte[] value;

    public void setTargetCharacteristic(BleCharacteristicX targetCharacteristic) {
        this.targetCharacteristic = targetCharacteristic;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    protected int doWork2() {
        if (targetCharacteristic == null) {
            finishedWithError("Target characteristic is null.");
            return 0;
        }

        BluetoothGattCharacteristic gattCharacteristic = targetCharacteristic.getGattCharacteristic();
        if (gattCharacteristic == null) {
            finishedWithError("Target characteristic is not discovered.");
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to read characteristic. The connection is not established.");
            return 0;
        }

        // 监听回调
        cb = new InnerCB();
        gattX.register(cb);

        if (gattX.tryReadCharacteristic(gattCharacteristic)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                gattX.evtBondStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);
            }
            return (COMMUNICATION_TIMEOUT);
        } else {
            finishedWithError("Failed to read characteristic.");
        }
        return 0;
    }

    @Override
    protected void onCleanup() {
        if (gattX != null) {
            gattX.evtBondStateChanged().clear(this);
            if (cb != null) {
                gattX.remove(cb);
            }
        }
        super.onCleanup();
    }

    @Override
    public void onEvent(Object src, int evtType, BleIntState evtData) {
        if (evtData.state == BluetoothDevice.BOND_BONDED) {
            final ILogger log = logger;
            if (log != null) {
                log.v(TAG, "Retry to read characteristic after bonded");
            }
            if (!gattX.tryReadCharacteristic(targetCharacteristic.getGattCharacteristic())) {
                finishedWithError("Failed to read characteristic after bonded.");
            }
        }
    }

    class InnerCB extends BluetoothGattCallback {
        /**
         * 遇到绑定问题的时候，需要等待绑定完成，再继续。
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                value = characteristic.getValue();
                finishedWithDone();
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                    || status == 8 /* GATT INSUF AUTHORIZATION */
                    || status == 137 /* GATT AUTH FAIL */) {
                //if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
                // This should never happen but it used to: http://stackoverflow.com/a/20093695/2115352
                //}
                // TODO: 2020/2/18 权限不足时，要根据情况判断是否需要重新请求
                finishedWithError("Insufficient Authentication");
            } else {
                String msg = "Error on reading characteristic <" + characteristic.getUuid() + ">: " + BleGattX.gattStatusToString(status);
                final ILogger log = logger;
                if (log != null) {
                    log.e(TAG, msg);
                }
                finishedWithError(msg);
            }
        }
    }
}
