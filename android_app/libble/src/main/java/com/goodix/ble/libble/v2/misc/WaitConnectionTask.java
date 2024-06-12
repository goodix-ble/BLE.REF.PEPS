package com.goodix.ble.libble.v2.misc;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskParameter;

public class WaitConnectionTask extends Task implements IEventListener<Integer> {
    @TaskParameter
    private GBRemoteDevice device;
    private int timeout = 30_000;

    public WaitConnectionTask setDevice(GBRemoteDevice device) {
        this.device = device;
        return this;
    }

    public WaitConnectionTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    protected int doWork() {
        if (device == null) {
            finishedWithError("Device is null.");
            return 0;
        }

        device.evtStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);

        if (device.isConnected()) {
            finishedWithDone();
        }
        return timeout;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        if (device != null) {
            device.evtStateChanged().clear(this);
        }
    }

    @Override
    public void onEvent(Object o, int i, Integer state) {
        if (state == GBRemoteDevice.STATE_CONNECTED) {
            finishedWithDone();
        }
    }
}
