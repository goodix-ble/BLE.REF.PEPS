package com.goodix.ble.libble.v2.impl.procedure;

import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libble.v2.impl.BleGattX;
import com.goodix.ble.libble.v2.impl.BleRemoteDevice;
import com.goodix.ble.libcomx.logger.Logger;
import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.util.AccessLock;

public abstract class BleBaseProcedure extends Task implements GBProcedure, AccessLock.CB {
    protected static final int GATT_TIMEOUT = 30_000 + 1000; // connectGatt() 连接设备会在30秒后上报GATT_ERROR，表示连接超时
    protected static final int COMMUNICATION_TIMEOUT = 4000 * 3; // 等待3个最长的CI间隔

    protected BleRemoteDevice remoteDevice;
    protected BleGattX gattX;
    protected int timeout = GATT_TIMEOUT;

    public void setRemoteDevice(BleRemoteDevice remoteDevice) {
        this.remoteDevice = remoteDevice;
    }

    @Override
    public GBProcedure setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public void startProcedure() {
        start(null, null);
    }

    @Override
    final protected int doWork() {
        BleRemoteDevice remoteDevice = this.remoteDevice;
        if (remoteDevice == null) {
            finishedWithError("Target remote device is null.");
            return 0;
        }

        gattX = remoteDevice.getGatt();

        if (!acquireLock()) {
            doWork2();
        }

        return timeout;
    }

    @Override
    protected void onCleanup() {
        if (printVerboseLog) {
            Logger.v(logger, getName(), "Release lock.");
        }
        remoteDevice.getLocker().releaseLock(this);
    }

    @Override
    public void onLockAcquired(AccessLock lock) {
        if (printVerboseLog) {
            Logger.v(logger, getName(), "Acquired lock.");
        }
        refreshTaskTimeout();
        // 确保是在指定线程下运行的
        getExecutor().execute(this::doWork2);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean acquireLock() {
        if (!remoteDevice.getLocker().acquireLock(this)) {
            if (printVerboseLog) {
                Logger.v(logger, getName(), "Wait lock.");
            }
        }
        return true;
    }

    protected abstract int doWork2();
}
