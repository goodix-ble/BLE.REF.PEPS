package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.v2.gb.gatt.GBGattCharacteristic;
import com.goodix.ble.libble.v2.gb.gatt.GBGattService;
import com.goodix.ble.libble.v2.impl.BleCharacteristicX;
import com.goodix.ble.libble.v2.impl.BleGattX;
import com.goodix.ble.libble.v2.impl.BleServiceX;
import com.goodix.ble.libcomx.logger.Logger;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.TaskError;
import com.goodix.ble.libcomx.task.TaskQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行发现服务的相关规程
 * 并且将发现的服务转换并保存到GBGattService的容器中
 * 对某些版本SCC不会自动启动的情况进行兼容
 */
public class GattDiscover extends BleBaseProcedure {
    private static final String TAG = "GattDiscover";

    private CB cb;

    @Override
    protected int doWork2() {
        BluetoothGatt gatt = gattX.getGatt();

        if (gatt == null) {
            finishedWithError("Abort discovering service for null gatt.");
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to start discovering service. The connection is not established.");
            return 0;
        }

        // 监听回调
        cb = new CB();
        gattX.register(cb);

        // 发起连接
        if (gatt.discoverServices()) {
            return (GATT_TIMEOUT);
        } else {
            finishedWithError("Failed to start discovering service.");
        }
        return 0;
    }

    @Override
    protected void onCleanup() {
        if (cb != null && gattX != null) {
            gattX.remove(cb);
        }
        super.onCleanup();
    }

    class CB extends BluetoothGattCallback {
        /**
         * 枚举被发现的服务，将原生的服务处理为GBGattService类型的服务集合
         * 处理过程中，需要考虑3种情况：不存在的添加，存在的更新，不存在的移除（同时移除里面的RemoteDevice引用）
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (taskState != STATE_RUNNING) {
                Logger.w(logger, TAG, "Discovering is not running.");
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                ArrayList<String> errors = new ArrayList<>(16);
                // 将全部的服务添加到临时MAP中
                remoteDevice.onDiscovered(gatt, errors);

                // 拆解为2步，先判断是否完整地发现了所有的服务，再进行Notification的初始化
                if (!errors.isEmpty()) {
                    StringBuilder sb = new StringBuilder(errors.size() * 128);
                    sb.append("Failed to discovery all services: ");
                    for (String error : errors) {
                        sb.append(error).append(" ");
                    }
                    finishedWithError(sb.toString());

                } else {
                    // 查看是否需要使能notify
                    ArrayList<BleServiceX> serviceList = remoteDevice.getServiceList();
                    TaskQueue queue = new TaskQueue();
                    queue.setName("EnableDefinedChar");
                    queue.setAbortOnException(true); // 如果使能Notification的过程中有错误，也要暴露出来。
                    // 使能SCC，在以下这些版本中，需要手动去使能
                    // https://android-review.googlesource.com/c/platform/system/bt/+/239970
                    if (remoteDevice.getBluetoothDevice().getBondState() == BluetoothDevice.BOND_BONDED
                            && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                            || Build.VERSION.SDK_INT == Build.VERSION_CODES.O
                            || Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1
                            || Build.VERSION.SDK_INT == Build.VERSION_CODES.P)) {
                        Logger.v(logger, TAG, "Enable SCC for bond device.");
                        List<GBGattService> gas = remoteDevice.getService(BleUuid.GENERIC_ATTRIBUTE_SERVICE);
                        for (GBGattService service : gas) {
                            Logger.v(logger, TAG, "Found GENERIC_ATTRIBUTE_SERVICE");
                            List<GBGattCharacteristic> scc = service.getCharacteristic(BleUuid.SERVICE_CHANGED_CHARACTERISTIC);
                            for (GBGattCharacteristic characteristic : scc) {
                                Logger.v(logger, TAG, "Found SERVICE_CHANGED_CHARACTERISTIC");
                                queue.addTask(characteristic.setEnableIndicate(true));
                            }
                        }
                    }
                    // 使能预先定义的特性的notify
                    for (BleServiceX serviceX : serviceList) {
                        if (!serviceX.isDiscovered()) continue;

                        for (BleCharacteristicX x : serviceX.getCharacteristicList()) {
                            if (!x.isDiscovered()) continue;

                            if (x.needEnableIndicate) {
                                Logger.v(logger, TAG, "Try to enable indicate after discovery for: " + x.getUuid());
                                queue.addTask(x.setEnableIndicate(true));
                            }
                            if (x.needEnableNotify) {
                                Logger.v(logger, TAG, "Try to enable notify after discovery for: " + x.getUuid());
                                queue.addTask(x.setEnableNotify(true));
                            }
                        }
                    }
                    // 执行规程
                    queue.setExecutor(getExecutor()).evtFinished().register2((src, evtType, evtData) -> {
                        TaskError err = evtData.getError();
                        if (err == null) {
                            remoteDevice.setDiscovered(true);
                            finishedWithDone();
                        } else {
                            finished(ITaskResult.CODE_ERROR, new TaskError(GattDiscover.this, "Failed to setup predefined characteristic : " + err.getRawMessage(), err));
                        }
                    });
                    remoteDevice.getLocker().releaseLock(GattDiscover.this); // 释放锁，以便队列执行
                    queue.setLogger(logger);
                    queue.start(null, null);
                }
            } else {
                finishedWithError("Error on discovering service: " + BleGattX.gattStatusToString(status));
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                finishedWithError("Connection has been terminated.");
            }
        }
    }
}
