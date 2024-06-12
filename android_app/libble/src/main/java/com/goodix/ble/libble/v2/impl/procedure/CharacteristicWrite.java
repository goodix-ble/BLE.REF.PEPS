package com.goodix.ble.libble.v2.impl.procedure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureWrite;
import com.goodix.ble.libble.v2.impl.BleCharacteristicX;
import com.goodix.ble.libble.v2.impl.BleGattX;
import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.IEventListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CharacteristicWrite extends BleBaseProcedure implements GBGattProcedureWrite, IEventListener<BleIntState> {
    private static final String TAG = "CharacteristicWrite";

    private BleCharacteristicX targetCharacteristic;
    private BluetoothGattCharacteristic gattCharacteristic;

    private InnerCB cb;
    private byte[] value;
    private int writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;

    private InputStream dataStream = null;
    private byte[] dataStreamBuffer;
    private int dataStreamSentCounter;
    private int dataStreamSize;
    private boolean autoCloseStream = false;
    private boolean flowCtrlPause = false;
    private boolean waitingResume = false;

    public void setTargetCharacteristic(BleCharacteristicX targetCharacteristic) {
        this.targetCharacteristic = targetCharacteristic;
    }

    @Override
    public void setValue(byte[] value) {
        this.value = value;
        this.dataStream = null;
    }

    public void setValue(InputStream dataStream, boolean autoClose) {
        this.value = null;
        this.dataStream = dataStream;
        this.autoCloseStream = autoClose;
    }

    public void setLargeValue(byte[] value, int startPos, int size) {
        if (value == null) {
            return;
        }

        if (startPos < 0) {
            startPos = value.length + startPos;
            if (startPos < 0) {
                startPos = 0;
            }
        }

        if (size < 0 || size > value.length) {
            size = value.length;
        }

        setValue(new ByteArrayInputStream(value, startPos, size), true);
    }

    public void setFlowCtrl(boolean pause) {
        boolean resume = false;
        synchronized (this) {
            if (flowCtrlPause != pause) {
                flowCtrlPause = pause;
                if (!pause && waitingResume) {
                    resume = true;
                    waitingResume = false;
                }
            }
        }
        if (resume) {
            getExecutor().execute(this::nextFragment);
        }
    }

    public void setWriteType(int writeType) {
        this.writeType = writeType;
    }

    @Override
    protected int doWork2() {
        if (targetCharacteristic == null) {
            finishedWithError("Target characteristic is null.");
            return 0;
        }

        if (!gattX.isConnected()) {
            finishedWithError("Failed to write characteristic. The connection is not established.");
            return 0;
        }

        gattCharacteristic = targetCharacteristic.getGattCharacteristic();
        if (gattCharacteristic == null) {
            finishedWithError("Target characteristic is not discovered.");
            return 0;
        }

        final int properties = gattCharacteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            finishedWithError("Target characteristic is not writable.");
            return 0;
        }

        flowCtrlPause = false;
        dataStreamSentCounter = 0;
        if (dataStream != null) {
            try {
                dataStreamSize = dataStream.available();
                reloadValue();
            } catch (Throwable e) {
                dataStreamSize = 0;
                finishedWithError("Failed to load data stream: " + e.getMessage(), e);
                return 0;
            }
        }

        if (value == null) {
            finishedWithError("Value is null.");
            return 0;
        }

        // 监听回调
        cb = new InnerCB();
        gattX.register(cb);

        if (!gattX.isConnected()) {
            finishedWithError("Failed to write characteristic. The connection is not established.");
            return 0;
        }

        gattCharacteristic.setValue(value);
        gattCharacteristic.setWriteType(writeType);
        if (gattX.tryWriteCharacteristic(gattCharacteristic)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                gattX.evtBondStateChanged().subEvent(this).setExecutor(getExecutor()).register2(this);
            }
            return (COMMUNICATION_TIMEOUT);
        } else {
            finishedWithError("Failed to write characteristic.");
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
        if (autoCloseStream && dataStream != null) {
            try {
                dataStream.close();
            } catch (IOException e) {
                ILogger logger = this.logger;
                if (logger != null) {
                    logger.e(getName(), "Failed to close stream: " + e.getMessage(), e);
                }
            }
        }
        super.onCleanup();
    }

    @Override
    public void onEvent(Object src, int evtType, BleIntState evtData) {
        if (evtData.state == BluetoothDevice.BOND_BONDED) {
            final ILogger log = logger;
            if (log != null) {
                log.v(TAG, "Retry to write characteristic after bonded");
            }
            if (!gattX.tryWriteCharacteristic(targetCharacteristic.getGattCharacteristic())) {
                finishedWithError("Failed to write characteristic after bonded.");
            }
        }
    }

    private int reloadValue() throws Throwable {
        if (dataStream != null) {
            int maxPayload = gattX.getMtu() - 3;
            if (dataStreamBuffer == null || dataStreamBuffer.length != maxPayload) {
                dataStreamBuffer = new byte[maxPayload];
            }
            int readSize = dataStream.read(dataStreamBuffer); // throw IOException
            if (readSize > 0) {
                if (value == null || value.length != readSize) {
                    value = new byte[readSize];
                }
                System.arraycopy(dataStreamBuffer, 0, value, 0, readSize);
                dataStreamSentCounter += readSize;
                return readSize;
            }
        }
        return 0;
    }

    private void nextFragment() {
        if (flowCtrlPause) {
            synchronized (this) {
                if (flowCtrlPause) {
                    waitingResume = true;
                    return;
                }
            }
        }

        try {
            if (reloadValue() > 0) {
                gattCharacteristic.setValue(value);
                if (gattX.tryWriteCharacteristic(gattCharacteristic)) {
                    if (dataStreamSize > 0) {
                        publishProgress(dataStreamSentCounter * 100 / dataStreamSize);
                    }
                    refreshTaskTimeout(); // 有可能百分比长时间保持为一个值
                } else {
                    finishedWithError("Failed to write next segment of data stream.");
                }
            } else {
                finishedWithDone();
            }
        } catch (Throwable e) {
            finishedWithError("Failed to read data stream: " + e.getMessage(), e);
        }
    }

    class InnerCB extends BluetoothGattCallback {
        /**
         * 遇到绑定问题的时候，需要等待绑定完成，再继续。
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (taskState != STATE_RUNNING) return;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                nextFragment();

            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                    || status == 8 /* GATT INSUF AUTHORIZATION */
                    || status == 137 /* GATT AUTH FAIL */) {
                //if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
                // This should never happen but it used to: http://stackoverflow.com/a/20093695/2115352
                //}
                // TODO: 2020/2/18 权限不足时，要根据情况判断是否需要重新请求
                finishedWithError("Insufficient Authentication");
            } else {
                String msg = "Error on writing characteristic <" + characteristic.getUuid() + ">: " + BleGattX.gattStatusToString(status);
                final ILogger log = logger;
                if (log != null) {
                    log.e(TAG, msg);
                }
                finishedWithError(status, msg);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (taskState != STATE_RUNNING) return;
            if (newState != BluetoothProfile.STATE_CONNECTED) {
                finishedWithError("Failed to write characteristic. The connection has been lost.");
            }
        }
    }
}
