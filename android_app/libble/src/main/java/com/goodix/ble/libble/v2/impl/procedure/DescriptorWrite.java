package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureWrite;
import com.goodix.ble.libble.v2.impl.BleDescriptorX;
import com.goodix.ble.libble.v2.impl.BleGattX;
import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.IEventListener;

public class DescriptorWrite extends BleBaseProcedure implements GBGattProcedureWrite, IEventListener<BleIntState> {
    private static final String TAG = "DescriptorWrite";

    private BleDescriptorX targetDescriptor;
    private byte[] value;

    private InnerCB cb;

    public void setTargetDescriptor(BleDescriptorX targetCharacteristic) {
        this.targetDescriptor = targetCharacteristic;
    }

    @Override
    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    protected int doWork2() {
        if (targetDescriptor == null) {
            finishedWithError("Target descriptor is null.");
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to write descriptor. The connection is not established.");
            return 0;
        }

        BluetoothGattDescriptor gattDescriptor = targetDescriptor.getGattDescriptor();
        if (gattDescriptor == null) {
            finishedWithError("Target descriptor is not discovered.");
            return 0;
        }

        if (value == null) {
            finishedWithError("Value is null.");
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to write descriptor. The connection is not established.");
            return 0;
        }

        // 监听回调
        cb = new InnerCB();
        gattX.register(cb);

        gattDescriptor.setValue(value);
        if (gattX.tryWriteDescriptor(gattDescriptor)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                gattX.evtBondStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);
            }
            return (COMMUNICATION_TIMEOUT);
        } else {
            finishedWithError("Failed to write descriptor.");
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
                log.v(TAG, "Retry to write descriptor after bonded");
            }
            if (!gattX.tryWriteDescriptor(targetDescriptor.getGattDescriptor())) {
                finishedWithError("Failed to write descriptor after bonded.");
            }
        }
    }

    class InnerCB extends BluetoothGattCallback {
        /**
         * 遇到绑定问题的时候，需要等待绑定完成，再继续。
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
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
                String msg = "Error on writing descriptor <" + descriptor.getUuid() + ">: " + BleGattX.gattStatusToString(status);
                final ILogger log = logger;
                if (log != null) {
                    log.e(TAG, msg);
                }
                finishedWithError(msg);
            }
        }
    }
}
