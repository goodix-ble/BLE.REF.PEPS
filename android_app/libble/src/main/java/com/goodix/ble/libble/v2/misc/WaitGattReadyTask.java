package com.goodix.ble.libble.v2.misc;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskParameter;

public class WaitGattReadyTask extends Task implements IEventListener<Boolean> {
    @TaskParameter
    GBRemoteDevice gatt;

    private int timeout = 30_000;

    public WaitGattReadyTask setGatt(GBRemoteDevice gatt) {
        this.gatt = gatt;
        return this;
    }

    public WaitGattReadyTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    protected int doWork() {

        gatt.evtReady()
                .subEvent(this).setExecutor(getExecutor()).register2(this);

        if (gatt.isReady()) {
            finishedWithDone();
            return 0;
        }

        return timeout;
    }

    @Override
    protected void onCleanup() {
        gatt.evtReady()
                .clear(this);
    }

    @Override
    public void onEvent(Object src, int evtType, Boolean evtData) {
        if (src == gatt && evtType == GBRemoteDevice.EVT_READY) {
            if (evtData) {
                finishedWithDone();
            } else {
                finishedWithError("Failed to setup GATT.");
            }
        }
    }
}
