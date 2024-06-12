package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.SystemClock;

import com.goodix.ble.libble.v2.gb.procedure.GBProcedureConnect;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.task.ITaskResult;

public class GattConnect extends BleBaseProcedure implements GBProcedureConnect {
    private static final long CONNECTION_TIMEOUT = 20_000;

    // 配置
    private int preferredPhy = 0;
    private int retryMax = 0; // 最大重试次数
    private int retryInterval = 0; // 重试前的间隔时间
    private boolean backgroundMode = false;

    private CB cb;
    private long startTime; // 记录连接开始的时间，以便判断133错误的原因
    private int retryCnt;

    public void setPreferredPhy(int preferredPhy) {
        this.preferredPhy = preferredPhy;
    }

    @Override
    public GBProcedureConnect setRetry(int retryMax, int retryInterval) {
        this.retryMax = retryMax;
        this.retryInterval = retryInterval;
        return this;
    }

    @Override
    public GBProcedureConnect setBackgroundMode(boolean enable) {
        backgroundMode = enable;
        return this;
    }

    @Override
    protected int doWork2() {
        remoteDevice.expectConnection = true; // 在逻辑上希望已经连接了

        // 如果已经连接
        if (gattX.isConnected()) {
            finishedWithDone();
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        // 发起连接
        // BluetoothDevice.PHY_LE_1M_MASK == 1
        retryCnt = 0;
        if (gattX.tryConnect(preferredPhy != 0 ? preferredPhy : 1, backgroundMode)) {
            startTime = SystemClock.elapsedRealtime();
            return (GATT_TIMEOUT);
        } else {
            finishedWithError("Failed to start connecting.");
        }
        return 0;
    }

    @Override
    protected void onCleanup() {
        // 如果是终止的情况，还需取消连接
        if (getResult().getCode() == ITaskResult.CODE_ABORT) {
            if (gattX != null) {
                gattX.tryDisconnect();
                gattX.tryCloseGatt();
            }
        }

        if (cb != null && gattX != null) {
            gattX.remove(cb);
        }

        super.onCleanup();
    }

    @Override
    protected void onTimeout(int id) {
        super.onTimeout(id);
        if (id == 1) {
            retryCnt++;
            final ILogger log = logger;
            if (log != null) {
                log.d(getName(), "Retry connecting... #" + retryCnt);
            }
            if (remoteDevice.getGatt().tryConnect(preferredPhy != 0 ? preferredPhy : 1, backgroundMode)) {
                startTime = SystemClock.elapsedRealtime();
                refreshTaskTimeout();
            } else {
                finishedWithError("Failed to retry connecting.");
            }
        }
    }

    class CB extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    startTime = 0;
                    finishedWithDone();
                } else {
                    finishedWithError("Disconnect successfully?");
                }
            } else {
                long delta = SystemClock.elapsedRealtime() - startTime;

                // 如果是超时错误，就不再重试了。如果是其他错误，就考虑重试一下。
                if (startTime > 0 && delta > CONNECTION_TIMEOUT) {
                    finishedWithError("GATT Timeout.");
                } else {
                    if (retryCnt < retryMax) {
                        if (retryInterval > 0) {
                            final ILogger log = logger;
                            if (log != null) {
                                log.d(getName(), "wait " + retryInterval + "ms to retry.");
                            }
                            startTimer(1, retryInterval);
                            // 重连前先释放资源
                            if (gattX != null) {
                                gattX.tryCloseGatt();
                            }
                        } else {
                            // 没有延时就直接重新连接
                            onTimeout(1);
                        }
                    } else {
                        if (!backgroundMode && gattX != null && newState != BluetoothGatt.STATE_CONNECTED) {
                            gattX.tryCloseGatt();
                        }
                        finishedWithError("Failed to connect device after " + retryCnt + " retry(s). Last status: " + status);
                    }
                }
            }
        }
    }
}
