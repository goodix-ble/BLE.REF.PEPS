package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;

import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libcomx.event.IEventListener;

public class BondCreate extends BleBaseProcedure implements IEventListener<BleIntState> {

    @Override
    protected int doWork2() {
        BluetoothDevice device = remoteDevice.getBluetoothDevice();

        if (device == null) {
            finishedWithError("Abort creating bond for null device.");
            return 0;
        }

        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            finishedWithDone(); //finishedWithError("Device already bonded: " + device.getName());
            return 0;
        }

        gattX.evtBondStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);

        // 发起连接
        if (gattX.tryCreateBond()) {
            return (COMMUNICATION_TIMEOUT);
        } else {
            finishedWithError("Failed to create bond.");
        }
        return 0;
    }

    @Override
    protected void onCleanup() {
        if (gattX != null) {
            gattX.evtBondStateChanged().clear(this);
        }
        super.onCleanup();
    }

    @Override
    public void onEvent(Object src, int evtType, BleIntState evtData) {
        switch (evtData.state) {
            case BluetoothDevice.BOND_NONE:
                if (evtData.prvState == BluetoothDevice.BOND_BONDING) {
                    finishedWithError("Failed to create bond.");
                } else if (evtData.prvState == BluetoothDevice.BOND_BONDED) {
                    finishedWithError("Bond has been removed.");
                }
                break;
//            case BluetoothDevice.BOND_BONDING:
//                //mCallbacks.onBondingRequired(device);
//                return;
            case BluetoothDevice.BOND_BONDED:
                finishedWithDone();
        }
    }
}
