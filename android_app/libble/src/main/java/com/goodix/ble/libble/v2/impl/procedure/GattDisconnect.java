package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedureConnect;

public class GattDisconnect extends BleBaseProcedure {

    private CB cb;
    private boolean clearCache = false;

    public void setClearCache(boolean clearCache) {
        this.clearCache = clearCache;
    }

    @Override
    protected boolean acquireLock() {
        // 对于断开连接这个规程，需要判断一下是否正处于建立连接的状态，如果是，就要尝试终止当前的规程。
        // 再等待处理结果
        Object procedure = remoteDevice.getLocker().getOwner();
        if (procedure instanceof GBProcedureConnect) {
            ((GBProcedureConnect) procedure).abort();
        }

        return super.acquireLock();
    }

    @Override
    protected int doWork2() {
        remoteDevice.expectConnection = false; // 在逻辑上已经希望连接断开了

        // 已经断开连接了，就直接成功
        if (gattX.getConnectionState() == GBRemoteDevice.STATE_DISCONNECTED) {
            // 如果设备是因为异常断开，再启动断链规程时，也要尝试释放资源
            releaseGattResource();
            finishedWithDone();
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        boolean connected = gattX.isConnected();

        // 请求断开连接
        if (gattX.tryDisconnect()) {
            // 如果连接还未成功的时候，请求断开并不会触发回调
            if (!connected) {
                releaseGattResource();
            }
            return (GATT_TIMEOUT);
        } else {
            finishedWithError("Failed to disconnect.");
        }
        return 0;
    }

    private void releaseGattResource() {
        if (clearCache) {
            gattX.tryRefreshDeviceCache();
        }
        gattX.tryCloseGatt(); // 是否应该考虑加个延时
        startTimer(1, 300);
    }

    @Override
    protected void onCleanup() {
        if (cb != null && gattX != null) {
            gattX.remove(cb);
        }
        super.onCleanup();
    }

    @Override
    protected void onTimeout(int id) {
        super.onTimeout(id);
        if (id == 1) {
            finishedWithDone();
        }
    }

    class CB extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                releaseGattResource();
            }
        }
    }
}
