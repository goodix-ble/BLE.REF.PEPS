package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.v2.impl.BleCharacteristicX;
import com.goodix.ble.libble.v2.impl.BleGattX;
import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.IEventListener;

public class NotificationEnable extends BleBaseProcedure implements IEventListener<BleIntState> {
    private static final String TAG = "NotificationEnable";

    private BleCharacteristicX targetCharacteristic;
    private boolean enable;
    private boolean forIndicate = false;

    private InnerCB cb;

    public void setTargetCharacteristic(BleCharacteristicX targetCharacteristic) {
        this.targetCharacteristic = targetCharacteristic;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setForIndicate() {
        this.forIndicate = true;
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

        // check properties
        int prop = forIndicate ? BluetoothGattCharacteristic.PROPERTY_INDICATE : BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        if ((gattCharacteristic.getProperties() & prop) == 0) {
            String propStr = forIndicate ? "INDICATE" : "NOTIFY";
            finishedWithError("Not found required property " + propStr + " in " + gattCharacteristic.getUuid());
            return 0;
        }

        if (!gattX.isConnected()) {
            if (enable) {
                finishedWithError("Failed to enable notify. The connection is not established.");
            } else {
                finishedWithError("Failed to disable notify. The connection is not established.");
            }
            return 0;
        }

        // 监听回调
        cb = new InnerCB();
        gattX.register(cb);

        if (gattX.tryEnableNotification(gattCharacteristic, forIndicate, enable)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                gattX.evtBondStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);
            }
            return (COMMUNICATION_TIMEOUT);
        } else {
            if (enable) {
                finishedWithError("Failed to enable notify.");
            } else {
                finishedWithError("Failed to disable notify.");
            }
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
                log.v(TAG, "Retry to set notification after bonded");
            }
            if (!gattX.tryEnableNotification(targetCharacteristic.getGattCharacteristic(), forIndicate, enable)) {
                if (enable) {
                    finishedWithError("Failed to enable notify after bonded.");
                } else {
                    finishedWithError("Failed to disable notify after bonded.");
                }
            }
        }
    }

    class InnerCB extends BluetoothGattCallback {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            final ILogger log = logger;

            if (!descriptor.getCharacteristic().getUuid().equals(targetCharacteristic.getUuid())) {
                if (log != null) {
                    log.w(TAG, "Unexpected onDescriptorWrite(): " + descriptor.getCharacteristic().getUuid());
                }
                return;
            }

            if (BleUuid.CCCD.equals(descriptor.getUuid())) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = descriptor.getValue();
                    if (data != null && data.length == 2 && data[1] == 0x00) {
                        switch (data[0]) {
                            case 0x00:
                                if (!enable) {
                                    finishedWithDone();
                                }
                                break;
                            case 0x01: // Notification enabled
                                if (!forIndicate && enable) {
                                    finishedWithDone();
                                }
                                break;
                            case 0x02: // Indication enabled
                                if (forIndicate && enable) {
                                    finishedWithDone();
                                }
                                break;
                        }
                    }
                } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == 8 /* GATT INSUF AUTHORIZATION */
                        || status == 137 /* GATT AUTH FAIL */) {
                    String msg = "Authentication required while modifying CCCD: " + BleGattX.gattStatusToString(status);
                    if (log != null) {
                        log.e(TAG, msg);
                    }
                    finishedWithError(msg);
                } else {
                    String msg = "Error on modifying CCCD: " + BleGattX.gattStatusToString(status);
                    if (log != null) {
                        log.e(TAG, msg);
                    }
                    finishedWithError(msg);
                }
            } else {
                if (log != null) {
                    log.w(TAG, "Unexpected descriptor while enable CCCD: " + descriptor.getUuid());
                }
            }
        }
    }
}
