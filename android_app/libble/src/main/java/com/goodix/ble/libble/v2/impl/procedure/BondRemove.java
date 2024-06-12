package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;

import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libcomx.event.IEventListener;

public class BondRemove extends BleBaseProcedure implements IEventListener<BleIntState> {

    private boolean needRemoveReceiver;

    @Override
    protected int doWork2() {
        BluetoothDevice device = remoteDevice.getBluetoothDevice();

        if (device == null) {
            finishedWithError("Abort removing bond for null device.");
            return 0;
        }

        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            finishedWithError("Device is not bonded: " + device.getName());
            return 0;
        }

        remoteDevice.expectConnection = false; // 在逻辑上已经希望连接断开了

        needRemoveReceiver = false;
        if (remoteDevice.isDisconnected()) {
            needRemoveReceiver = true;
            gattX.setupReceiver();
        }
        gattX.evtBondStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);

        // 发起连接
        if (gattX.tryRemoveBond()) {
            return (COMMUNICATION_TIMEOUT);
        } else {
            finishedWithError("Failed to remove bond.");
        }
        return 0;
    }

    @Override
    protected void onCleanup() {
        if (gattX != null) {
            gattX.evtBondStateChanged().clear(this);

            // 是由该规程创建的监听就由该规程去移除
            if (needRemoveReceiver && remoteDevice.isDisconnected()) {
                needRemoveReceiver = false;
                gattX.cleanReceiver();
            }
        }
        super.onCleanup();
    }

    @Override
    public void onEvent(Object src, int evtType, BleIntState evtData) {
        switch (evtData.state) {
            case BluetoothDevice.BOND_NONE:
                finishedWithDone();
                break;
            case BluetoothDevice.BOND_BONDED:
                finishedWithError("Bond has been created.");
        }
    }
}
