package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.chain.GattCallbackDispatcher;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.pojo.GBError;
import com.goodix.ble.libble.v2.impl.data.BleCI;
import com.goodix.ble.libble.v2.impl.data.BleIntState;
import com.goodix.ble.libble.v2.impl.data.BlePairingVariant;
import com.goodix.ble.libble.v2.impl.data.BlePhy;
import com.goodix.ble.libble.v2.impl.data.BleValue;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.logger.Logger;
import com.goodix.ble.libcomx.util.HexStringBuilder;

import java.lang.reflect.Method;


/**
 * 1. 封装Gatt事件分发
 * 2. 完成Gatt操作的兼容
 * 3. 为扩展提供框架
 * 4. 处理Broadcast事件
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BleGattX extends GattCallbackDispatcher {
    private static final String TAG = "BleGatt";

    public static final int EVT_CHAR_READ = 1001;
    public static final int EVT_CHAR_WRITTEN = 1002;
    public static final int EVT_CHAR_NOTIFY = 1003;
    public static final int EVT_CHAR_INDICATE = 1004;
    public static final int EVT_CHAR_CHANGED = 1005; // 综合了Notify和Indicate

    public static final int EVT_DESC_READ = 1006;
    public static final int EVT_DESC_WRITTEN = 1007;

    public static final int EVT_CONNECTION_CHANGED = 2001;
    public static final int EVT_SERVICE_DISCOVERED = 2005;

    public static final int EVT_RSSI = 3001;
    public static final int EVT_MTU = 3002;
    public static final int EVT_PHY = 3003;
    public static final int EVT_CI = 3004;

    public static final int EVT_ERROR = 4001;
    public static final int EVT_LINK_LOSS = 4002;
    public static final int EVT_ADAPTER_STATE_CHANGED = 4003;
    public static final int EVT_BOND_STATE_CHANGED = 4004;

    private Event<GBError> eventError = new Event<>(this, EVT_ERROR);
    private Event<Integer> eventStateChanged = new Event<>(this, EVT_CONNECTION_CHANGED);
    private Event<Integer> eventServiceDiscovered = new Event<>(this, EVT_SERVICE_DISCOVERED);

    private Event<BleValue> eventCharRead = new Event<>(this, EVT_CHAR_READ);
    private Event<BleValue> eventCharWritten = new Event<>(this, EVT_CHAR_WRITTEN);
    private Event<BleValue> eventCharNotify = new Event<>(this, EVT_CHAR_NOTIFY);
    private Event<BleValue> eventCharIndicate = new Event<>(this, EVT_CHAR_INDICATE);
    private Event<BleValue> eventCharChanged = new Event<>(this, EVT_CHAR_CHANGED);

    private Event<BluetoothGattDescriptor> eventDescRead = new Event<>(this, EVT_DESC_READ);
    private Event<BluetoothGattDescriptor> eventDescWritten = new Event<>(this, EVT_DESC_WRITTEN);

    private Event<Integer> eventMtu = new Event<>(this, EVT_MTU);
    private Event<Integer> eventRssi = new Event<>(this, EVT_RSSI);
    private Event<BlePhy> eventPhy = new Event<>(this, EVT_PHY);
    private Event<BleCI> eventCI = new Event<>(this, EVT_CI);

    private Event<BleIntState> eventAdapterStateChanged = new Event<>(this, EVT_ADAPTER_STATE_CHANGED);
    private Event<BleIntState> eventBondStateChanged = new Event<>(this, EVT_BOND_STATE_CHANGED);

    @Nullable
    ILogger logger;

    private final Context mCtx;
    BluetoothGatt mGatt;
    private BluetoothDevice mDevice;
    private boolean mAutoConnect;

    private boolean mConnected;
    private int mConnectionState;
    //private long mConnectionTime; // 用于区分发生133错误的原因，如果超时了说明是没连上，否则就确实发生了错误。
    private boolean mReady;

    private int mMtu;

    private boolean receiverRegistered = false;
    private BondStateReceiver bondStateReceiver = new BondStateReceiver();
    private BluetoothAdapterStateReceiver btAdapterStateReceiver = new BluetoothAdapterStateReceiver();
    private PairingRequestReceiver pairingRequestReceiver = new PairingRequestReceiver();

    public BleGattX(@NonNull Context context) {
        mCtx = context.getApplicationContext();
    }

    public void setLogger(@Nullable ILogger logger) {
        this.logger = logger;
    }

    public void setDevice(BluetoothDevice mDevice) {
        this.mDevice = mDevice;
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public int getMtu() {
        return mMtu;
    }

    public void readRemoteRssi() {
        if (mGatt != null) {
            mGatt.readRemoteRssi();
        }
    }

    public boolean isConnected() {
        return mConnected;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public Event<BleValue> evtCharRead() {
        return eventCharRead;
    }

    public Event<BleValue> evtCharWritten() {
        return eventCharWritten;
    }

    public Event<BleValue> evtCharNotify() {
        return eventCharNotify;
    }

    public Event<BleValue> evtCharIndicate() {
        return eventCharIndicate;
    }

    public Event<BleValue> evtCharChanged() {
        return eventCharChanged;
    }

    public Event<BluetoothGattDescriptor> evtDescRead() {
        return eventDescRead;
    }

    public Event<BluetoothGattDescriptor> evtDescWritten() {
        return eventDescWritten;
    }

    public Event<Integer> evtMtu() {
        return eventMtu;
    }

    public Event<Integer> evtRssi() {
        return eventRssi;
    }

    public Event<BlePhy> evtPhy() {
        return eventPhy;
    }

    public Event<BleCI> evtCI() {
        return eventCI;
    }

    public Event<GBError> evtError() {
        return eventError;
    }

    public Event<Integer> evtServiceDiscovered() {
        return eventServiceDiscovered;
    }

    public Event<Integer> evtStateChanged() {
        return eventStateChanged;
    }

    public Event<BleIntState> evtAdapterStateChanged() {
        return eventAdapterStateChanged;
    }

    public Event<BleIntState> evtBondStateChanged() {
        return eventBondStateChanged;
    }

    public void clearListener(Object tag) {
        eventCharNotify.clear(tag);
        eventCharIndicate.clear(tag);
        eventCharRead.clear(tag);
        eventCharWritten.clear(tag);
        eventDescRead.clear(tag);
        eventDescWritten.clear(tag);
        eventMtu.clear(tag);
        eventRssi.clear(tag);
        eventPhy.clear(tag);
        eventCI.clear(tag);
        eventError.clear(tag);
        eventServiceDiscovered.clear(tag);
        eventStateChanged.clear(tag);
    }

    private static String dumpValue(byte[] value) {
        return dumpValue(null, value);
    }

    private static String dumpValue(HexStringBuilder sb, byte[] value) {
        if (value == null) {
            return "null";
        }
        if (value.length == 0) {
            return "[0]";
        }

        if (sb == null) {
            sb = new HexStringBuilder(8 + value.length * 2);
        }

        sb.a("[").append(value.length).a("] ").put(value);
        // 尝试以字符串的方式输出，仅支持 ASCII 码
        sb.a(" (");
        for (byte b : value) {
            int ch = 0xFF & b;
            if (ch < 32) ch = '.';
            if (ch > 126) ch = '.';
            sb.append((char) ch);
        }
        sb.a(")");

        return sb.toString();
    }

    @RequiresApi(value = Build.VERSION_CODES.O)
    private String phyToString(final int phy) {
        switch (phy) {
            case BluetoothDevice.PHY_LE_1M:
                return "LE 1M";
            case BluetoothDevice.PHY_LE_2M:
                return "LE 2M";
            case BluetoothDevice.PHY_LE_CODED:
                return "LE Coded";
            default:
                return "UNKNOWN (" + phy + ")";
        }
    }

    private String stateToString(final int state) {
        switch (state) {
            case BluetoothGatt.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothGatt.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothGatt.STATE_DISCONNECTING:
                return "DISCONNECTING";
            case BluetoothGatt.STATE_DISCONNECTED:
                return "DISCONNECTED";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    private String writeTypeToString(final int type) {
        switch (type) {
            case BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:
                return "WRITE REQUEST";
            case BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:
                return "WRITE COMMAND";
            case BluetoothGattCharacteristic.WRITE_TYPE_SIGNED:
                return "WRITE SIGNED";
            default:
                return "UNKNOWN: " + type;
        }
    }

    //@RequiresApi(value = Build.VERSION_CODES.O)
    private String phyMaskToString(final int mask) {
        switch (mask) {
            case BluetoothDevice.PHY_LE_1M_MASK:
                return "LE 1M";
            case BluetoothDevice.PHY_LE_2M_MASK:
                return "LE 2M";
            case BluetoothDevice.PHY_LE_CODED_MASK:
                return "LE Coded";
            case BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_2M_MASK:
                return "LE 1M or LE 2M";
            case BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_CODED_MASK:
                return "LE 1M or LE Coded";
            case BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK:
                return "LE 2M or LE Coded";
            case BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_2M_MASK
                    | BluetoothDevice.PHY_LE_CODED_MASK:
                return "LE 1M, LE 2M or LE Coded";
            default:
                return "UNKNOWN (" + mask + ")";
        }
    }

    public static String gattStatusToString(final int status) {
        HexStringBuilder sb = new HexStringBuilder(64);
        sb.append("[0x").append(Integer.toHexString(status)).append("] ");
        sb.append(GattStatus.gattStatusToString(status));
        return sb.toString();
    }

    public void setupReceiver() {
        if (receiverRegistered) return;
        Logger.d(logger, TAG, "setupReceiver()");
        mCtx.registerReceiver(btAdapterStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        mCtx.registerReceiver(pairingRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        mCtx.registerReceiver(bondStateReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        receiverRegistered = true;
    }

    public void cleanReceiver() {
        if (!receiverRegistered) return;
        Logger.d(logger, TAG, "cleanReceiver()");
        try {
            mCtx.unregisterReceiver(btAdapterStateReceiver);
            mCtx.unregisterReceiver(pairingRequestReceiver);
            mCtx.unregisterReceiver(bondStateReceiver);
        } catch (final Exception e) {
            // the receiver must have been not registered or unregistered before.
            final ILogger log = logger;
            if (log != null) {
                log.w(TAG, "Error on cleaning up receiver: " + e);
            }
        }
        receiverRegistered = false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 1. 判读是否已连接
     * 2. 决定是否重用连接
     * 3. 决定是否注册广播
     * 4. 根据不同的安卓版本，决定使用哪种方式去连接
     *
     * @param preferredPhy preferred PHY for connections to remote LE device. Bitwise OR of any of {@link
     *                     BluetoothDevice#PHY_LE_1M_MASK}, {@link BluetoothDevice#PHY_LE_2M_MASK}, and {@link
     *                     BluetoothDevice#PHY_LE_CODED_MASK}. This option does not take effect if {@code autoConnect}
     *                     is set to true.
     */
    @AnyThread
    public boolean tryConnect(int preferredPhy, boolean autoConnect) {
        final ILogger log = logger;

        final boolean bluetoothEnabled = BluetoothAdapter.getDefaultAdapter().isEnabled();
        if (!bluetoothEnabled) {
            if (log != null) {
                log.e(TAG, "tryConnect() failed, for bluetooth adapter is not available.");
            }
            return false;
        }

        final BluetoothDevice device = mDevice;
        if (mConnected) {
            if (log != null) {
                log.d(TAG, "tryConnect() skipped, and there is an exist connection.");
            }
            return true;
        }

        synchronized (this) {
            BluetoothGatt gatt = this.mGatt;
            if (gatt == null) {
                // mGatt 即将被赋值，准备好Receiver
                // Register bonding broadcast receiver
                setupReceiver();
            } else {
                // 如果之前的GATT还在，就关闭它。
                try {
                    Logger.d(logger, TAG, "gatt.close()");
                    gatt.close(); // gatt.close() 调用后并不能立即生效
                } catch (final Throwable t) {
                    // ignore
                }

                this.mGatt = null;
                this.mAutoConnect = false;

                try {
                    Logger.d(logger, TAG, "wait(200)");
                    Thread.sleep(200); // Is 200 ms enough?
                } catch (final InterruptedException e) {
                    // Ignore
                }

                // 避免有时候被动断连的情况下，移除绑定，此时会移除广播监听，再建立连接时因为原来的gatt对象还在而导致不会重新注册广播监听
                setupReceiver();
            }
        }

        mConnectionState = GBRemoteDevice.STATE_CONNECTING;

        eventStateChanged.postEvent(mConnectionState);

        // 判断使用哪个版本的API来连接
        BluetoothGatt gatt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // connectRequest will never be null here.
            if (log != null) {
                log.d(TAG, "connectGatt(autoConnect = " + autoConnect + ", TRANSPORT_LE, "
                        + phyMaskToString(preferredPhy) + ")");
            }
            // A variant of connectGatt with Handled can't be used here.
            // Check https://github.com/NordicSemiconductor/Android-BLE-Library/issues/54
            gatt = device.connectGatt(mCtx, autoConnect, this,
                    BluetoothDevice.TRANSPORT_LE, preferredPhy/*, mHandler*/);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (log != null) {
                log.d(TAG, "connectGatt(autoConnect = " + autoConnect + ", TRANSPORT_LE)");
            }
            gatt = device.connectGatt(mCtx, autoConnect, this,
                    BluetoothDevice.TRANSPORT_LE);
        } else {
            if (log != null) {
                log.d(TAG, "connectGatt(autoConnect = " + autoConnect + ")");
            }
            gatt = device.connectGatt(mCtx, autoConnect, this);
        }

        synchronized (this) {
            mGatt = gatt;
            mAutoConnect = autoConnect;
            if (mGatt == null) {
                mAutoConnect = false;
                cleanReceiver();
            }
        }
        return gatt != null;
    }

    /**
     * @return false - connection has not been established.
     */
    public boolean tryDisconnect() {
        mReady = false;

        final ILogger log = logger;
        boolean isConnected = this.mConnected;

        if (mGatt != null) {
            if (log != null) {
                log.v(TAG, isConnected ? "Disconnecting..." : "Cancelling connection...");
            }

            mConnectionState = BluetoothGatt.STATE_DISCONNECTING;
            eventStateChanged.postEvent(mConnectionState);

            mConnected = false;
            mGatt.disconnect();

            if (!isConnected) {
                if (log != null) {
                    log.v(TAG, "Cancel");
                }
                mConnectionState = BluetoothGatt.STATE_DISCONNECTED;
                eventStateChanged.postEvent(mConnectionState);
            }
        } else {
            return false;
        }

        return true;
    }

    public void tryCloseGatt() {
        final ILogger log = logger;
        if (log != null) {
            log.v(TAG, "Close gatt and dispose resource.");
        }

        cleanReceiver();

        synchronized (this) {
            if (mGatt != null) {
                try {
                    mGatt.close();
                } catch (final Throwable t) {
                    if (log != null) {
                        log.w(TAG, "Error on closing gatt: " + t);
                    }
                }
                mGatt = null;
                this.mAutoConnect = false;
                // 发送个事件通知一下外部，已经没有连接了
                if (mConnectionState != GBRemoteDevice.STATE_DISCONNECTED) {
                    mConnected = false;
                    mConnectionState = GBRemoteDevice.STATE_DISCONNECTED;
                    eventStateChanged.postEvent(mConnectionState);
                }
            }
        }
    }

    public boolean tryRefreshDeviceCache() {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null) // no need to be connected
            return false;

        final ILogger log = logger;

        try {
            //noinspection JavaReflectionMemberAccess
            final Method refresh = gatt.getClass().getMethod("refresh");
            Boolean ret = (Boolean) refresh.invoke(gatt);
            boolean result = ret != null && ret;

            if (log != null) {
                log.v(TAG, "Refresh device cache: " + result);
            }

            return result;
        } catch (final Exception e) {
            Log.w(TAG, "Exception on refreshing device", e);
            if (log != null) {
                log.w(TAG, "Exception on refreshing device: " + e);
            }
        }
        return false;
    }

    public boolean tryReadCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || characteristic == null || !mConnected)
            return false;

        ILogger log = this.logger;
        if (log != null) {
            log.v(TAG, "Reading characteristic " + characteristic.getUuid());
        }

        // Check characteristic property.
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            if (log != null) {
                log.e(TAG, "No PROPERTY_READ in characteristic: " + characteristic.getUuid());
            }
            return false;
        }

        return gatt.readCharacteristic(characteristic);
    }

    public boolean tryWriteCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final ILogger log = this.logger;
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || characteristic == null || !mConnected)
            return false;

        // Check characteristic property.
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (log != null) {
                log.w(TAG, "Writing characteristic " + characteristic.getUuid() +
                        " (no required properties)");
            }
            return false;
        }

        // 先写再打印，避免数据量大时打印消耗太多时间。
        boolean ret = gatt.writeCharacteristic(characteristic);
        if (log != null) {
            log.v(TAG, "Writing characteristic " + characteristic.getUuid() +
                    " (" + writeTypeToString(characteristic.getWriteType()) + ", ret = " + ret + "): " + dumpValue(characteristic.getValue()));
        }
        return ret;
    }

    public boolean tryEnableNotification(BluetoothGattCharacteristic characteristic, boolean forIndicate, boolean enable) {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || characteristic == null || !mConnected)
            return false;

        ILogger log = this.logger;

        // check properties
        int prop = forIndicate ? BluetoothGattCharacteristic.PROPERTY_INDICATE : BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        if ((characteristic.getProperties() & prop) == 0) {
            if (log != null) {
                String propStr = forIndicate ? "INDICATE" : "NOTIFY";
                log.w(TAG, "Not found required property " + propStr + " in " + characteristic.getUuid());
            }
            return false;
        }

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleUuid.CCCD);
        //final BluetoothGattDescriptor descriptor = getCccd(characteristic, indicate ? BluetoothGattCharacteristic.PROPERTY_INDICATE : BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        if (descriptor != null) {

            if (log != null) {
                log.d(TAG, "gatt.setCharacteristicNotification(" + characteristic.getUuid() + ", " + enable + ")");
            }
            gatt.setCharacteristicNotification(characteristic, enable);

            if (enable) {
                if (forIndicate) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    if (log != null) {
                        log.v(TAG, "Enabling indication of " + characteristic.getUuid());
                    }
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (log != null) {
                        log.v(TAG, "Enabling notification of " + characteristic.getUuid());
                    }
                }
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                if (log != null) {
                    log.v(TAG, "Disabling notification and indication of " + characteristic.getUuid());
                }
            }
            return writeDescriptorCompat(descriptor);
        } else {
            if (log != null) {
                log.w(TAG, "Can not get CCCD of " + characteristic.getUuid());
            }
        }
        return false;
    }

    public boolean tryReadDescriptor(final BluetoothGattDescriptor descriptor) {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || descriptor == null || !mConnected)
            return false;

        ILogger log = this.logger;
        if (log != null) {
            log.v(TAG, "Reading descriptor " + descriptor.getUuid());
        }
        return gatt.readDescriptor(descriptor);
    }

    public boolean tryWriteDescriptor(final BluetoothGattDescriptor descriptor) {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || descriptor == null || !mConnected)
            return false;

        ILogger log = this.logger;
        if (log != null) {
            log.v(TAG, "Writing descriptor " + descriptor.getUuid());
        }
        return writeDescriptorCompat(descriptor);
    }

    public boolean tryCreateBond() {
        final BluetoothDevice device = mDevice;
        if (device == null)
            return false;

        ILogger log = this.logger;
        if (log != null) {
            log.v(TAG, "Starting pairing...");
        }

        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            if (log != null) {
                log.w(TAG, "Device is already bonded");
            }
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (log != null) {
                log.d(TAG, "device.createBond()");
            }
            return device.createBond();
        } else {
            /*
             * There is a createBond() method in BluetoothDevice class but for now it's hidden.
             * We will call it using reflections. It has been revealed in KitKat (Api19).
             */
            try {
                final Method createBond = device.getClass().getMethod("createBond");
                if (log != null) {
                    log.d(TAG, "device.createBond() (hidden)");
                }
                Boolean result = (Boolean) createBond.invoke(device);
                return result != null ? result : false;
            } catch (final Exception e) {
                if (log != null) {
                    log.w(TAG, "An exception occurred while creating bond: " + e);
                }
            }
        }
        return false;
    }

    /**
     * Enqueues removing bond information. When the device was bonded and the bond
     * information was successfully removed, the device will disconnect.
     * Note, that this will not remove the bond information from the connected device!
     */
    public boolean tryRemoveBond() {
        final BluetoothDevice device = mDevice;
        if (device == null)
            return false;

        ILogger log = this.logger;
        if (log != null) {
            log.v(TAG, "Removing bond information...");
        }

        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            if (log != null) {
                log.w(TAG, "Device is not bonded");
            }
            return false;
        }

        /*
         * There is a removeBond() method in BluetoothDevice class but for now it's hidden.
         * We will call it using reflections.
         */
        try {
            //noinspection JavaReflectionMemberAccess
            final Method removeBond = device.getClass().getMethod("removeBond");
            if (log != null) {
                log.d(TAG, "device.removeBond() (hidden)");
            }
            //noinspection ConstantConditions
            return (Boolean) removeBond.invoke(device);
        } catch (final Exception e) {
            if (log != null) {
                log.w(TAG, "An exception occurred while removing bond: " + e);
            }
        }
        return false;
    }

    private boolean writeDescriptorCompat(final BluetoothGattDescriptor descriptor) {
        final BluetoothGatt gatt = mGatt;
        if (gatt == null || descriptor == null || !mConnected)
            return false;

        final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
        final int originalWriteType = parentCharacteristic.getWriteType();
        parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        final boolean result = gatt.writeDescriptor(descriptor);
        parentCharacteristic.setWriteType(originalWriteType);
        return result;
    }

    /**
     * 1. 将回调转换为对外部的通知事件：
     * 2. 分发事件给需要模块
     */
    private final static String ERROR_CONNECTION_STATE_CHANGE = "Error on connection state change";
    private final static String ERROR_DISCOVERY_SERVICE = "Error on discovering services";
    private final static String ERROR_AUTH_ERROR_WHILE_BONDED = "Phone has lost bonding information";
    //        private final static String ERROR_READ_CHARACTERISTIC = "Error on reading characteristic";
    private final static String ERROR_WRITE_CHARACTERISTIC = "Error on writing characteristic";
    private final static String ERROR_READ_DESCRIPTOR = "Error on reading descriptor";
    private final static String ERROR_WRITE_DESCRIPTOR = "Error on writing descriptor";
    private final static String ERROR_MTU_REQUEST = "Error on mtu request";
    private final static String ERROR_CONNECTION_PRIORITY_REQUEST = "Error on connection priority request";
    private final static String ERROR_READ_RSSI = "Error on RSSI read";
    private final static String ERROR_READ_PHY = "Error on PHY read";
    private final static String ERROR_PHY_UPDATE = "Error on PHY update";
    private final static String ERROR_RELIABLE_WRITE = "Error on Execute Reliable Write";
    private final static long CONNECTION_TIMEOUT_THRESHOLD = 20000; // ms

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        final ILogger log = logger;
        if (log != null) {
            log.d(TAG, "[Callback] Connection state changed with status: " +
                    status + " and new state: " + newState + " (" + stateToString(newState) + ")");
        }

        // 先设置状态，否则 super.onConnectionStateChange(gatt, status, newState) 中
        // 启动的任务会无法正确获得当前的状态
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mGatt == null) {
                    if (log != null) {
                        log.e(TAG, "Device received notification after disconnection.");
                        log.d(TAG, "gatt.close()");
                    }
                    try {
                        gatt.close();
                    } catch (final Throwable t) {
                        // ignore
                    }
                    // 丢弃该事件
                    return;
                }

                if (log != null) {
                    log.i(TAG, "Connected to " + gatt.getDevice().getAddress());
                }

                // 在建立连接的时候重置MTU值
                mMtu = 23;

                mConnected = true;
                mConnectionState = GBRemoteDevice.STATE_CONNECTED;
            }
        } else {
            // 不管操作有没有成功，GATT已经处于 DISCONNECTED 状态了。
            // 在Connect规程中可以判断处于DISCONNECTED状态的原因，然后决定是否要重试连接。
            mConnected = false;
            mConnectionState = GBRemoteDevice.STATE_DISCONNECTED;
        }

        super.onConnectionStateChange(gatt, status, newState);

        // 再分发事件
        eventStateChanged.postEvent(mConnectionState);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            String msg = "Connection Error: (0x" + Integer.toHexString(status) + "): " + GattStatus.gattDisconnectReasonToString(status);
            if (log != null) {
                log.e(TAG, msg);
            }
            eventError.postEvent(new GBError(status, msg));
        }

        if (mConnectionState == GBRemoteDevice.STATE_DISCONNECTED && mAutoConnect) {
            // 使用了自动连接，后台会一直处于回连状态，所以要及时切换为连接中状态
            // 并重新发起连接
            if (gatt.connect()) {
                eventStateChanged.postEvent(mConnectionState = GBRemoteDevice.STATE_CONNECTING);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        String msg = "Services Discovered: " + gattStatusToString(status);

        final ILogger log = logger;
        if (log != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log.i(TAG, msg);
            } else {
                log.e(TAG, msg);
                eventError.postEvent(new GBError(status, msg));
            }
        }

        super.onServicesDiscovered(gatt, status);
        eventServiceDiscovered.postEvent(status);
    }

    /**
     * 1. 要判断status是否表示成功
     * 2. 读可能因为没有权限而错误，在绑定后，需要重新进行写操作。
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        final ILogger log = logger;
        if (log != null) {
            log.i(TAG, "Read characteristic <" + characteristic.getUuid() +
                    ">: " + dumpValue(characteristic.getValue()));
        }

        super.onCharacteristicRead(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventCharRead.postEvent(new BleValue(gatt, characteristic));
        } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                || status == 8 /* GATT INSUF AUTHORIZATION */
                || status == 137 /* GATT AUTH FAIL */) {
            String msg = "Error on reading characteristic <" + characteristic.getUuid() + ">: " + gattStatusToString(status);
            if (log != null) {
                log.w(TAG, msg);
            }
            if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
                // This should never happen but it used to: http://stackoverflow.com/a/20093695/2115352
                if (log != null) {
                    log.w(TAG, ERROR_AUTH_ERROR_WHILE_BONDED);
                }
            }
            eventError.postEvent(new GBError(status, ERROR_AUTH_ERROR_WHILE_BONDED));
        } else {
            String msg = "Error on reading characteristic <" + characteristic.getUuid() + ">: " + gattStatusToString(status);
            if (log != null) {
                log.e(TAG, msg);
            }
            eventError.postEvent(new GBError(status, msg));
        }
    }

    /**
     * 1. 要判断status是否表示成功
     * 2. 写可能因为没有权限而错误，在绑定后，需要重新进行写操作。
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        final ILogger log = logger;
        if (log != null) {
            log.i(TAG, "Data written to <" + characteristic.getUuid() + ">: " + dumpValue(characteristic.getValue())
                    + " status: " + status);
        }

        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventCharWritten.postEvent(new BleValue(gatt, characteristic));
        } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                || status == 8 /* GATT INSUF AUTHORIZATION */
                || status == 137 /* GATT AUTH FAIL */) {
            if (log != null) {
                log.w(TAG, "Authentication required (" + status + ")");
            }
            if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_NONE) {
                // This should never happen but it used to: http://stackoverflow.com/a/20093695/2115352
                if (log != null) {
                    log.w(TAG, ERROR_AUTH_ERROR_WHILE_BONDED);
                }
                eventError.postEvent(new GBError(status, ERROR_AUTH_ERROR_WHILE_BONDED));
            }
            // The request will be repeated when the bond state changes to BONDED.
        } else {
            eventError.postEvent(new GBError(status, ERROR_WRITE_CHARACTERISTIC));
        }
    }

    /**
     * 接收特征上报的数据。
     * 1. 如果是 Service Change 特征上报数据，说明服务发生了变化，需要重新发现服务
     * 2. 忽略电池电量的处理
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // 先输出收到的内容，再进一步处理
        final ILogger log = logger;
        if (log != null) {
            byte[] value = characteristic.getValue();
            HexStringBuilder sb = new HexStringBuilder(256);
            sb.a("CharacteristicChanged <").a(characteristic.getUuid().toString()).a("> : ");
            dumpValue(sb, value);
            log.i(TAG, sb.toString());
        }

        super.onCharacteristicChanged(gatt, characteristic);

        if (BleUuid.SERVICE_CHANGED_CHARACTERISTIC.equals(characteristic.getUuid())) {
            gatt.discoverServices();
            // Forbid enqueuing more operations.
            // Clear queues, services are no longer valid.
            // TODO: 2020/1/15 Service容器里面的Service需要进行更新
            // 不论是否是 SCC 的通知，都需要分发
        }

        BluetoothGattDescriptor cccd = characteristic.getDescriptor(BleUuid.CCCD);
        final boolean notifications = cccd == null || cccd.getValue() == null ||
                cccd.getValue().length != 2 || cccd.getValue()[0] == 0x01;

        BleValue value = new BleValue(gatt, characteristic);
        if (notifications) {
            eventCharNotify.postEvent(value);
        } else { // indications
            eventCharIndicate.postEvent(value);
        }
        eventCharChanged.postEvent(value);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        final ILogger log = logger;
        if (log != null) {
            log.v(TAG, "Data read from descriptor: " + descriptor.getUuid() + " " + dumpValue(descriptor.getValue()));
        }

        super.onDescriptorRead(gatt, descriptor, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventDescRead.postEvent(descriptor);
        } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                || status == 8 /* GATT INSUF AUTHORIZATION */
                || status == 137 /* GATT AUTH FAIL */) {
            String msg = "Authentication required while reading descriptor: " + gattStatusToString(status);
            eventError.postEvent(new GBError(status, msg));
            if (log != null) {
                log.w(TAG, msg);
            }
        } else {
            String msg = "Error on reading descriptor: " + gattStatusToString(status);
            eventError.postEvent(new GBError(status, msg));
            if (log != null) {
                log.e(TAG, msg);
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        final ILogger log = logger;
        if (log != null) {
            log.v(TAG, "Data written to descriptor: " + descriptor.getUuid() + " " + dumpValue(descriptor.getValue()));
        }

        super.onDescriptorWrite(gatt, descriptor, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventDescWritten.postEvent(descriptor);

            if (log != null && BleUuid.CCCD.equals(descriptor.getUuid())) {
                byte[] data = descriptor.getValue();
                if (data != null && data.length == 2 && data[1] == 0x00) {
                    switch (data[0]) {
                        case 0x00:
                            log.i(TAG, "Notification disabled: " + descriptor.getCharacteristic().getUuid());
                            break;
                        case 0x01:
                            log.i(TAG, "Notification enabled: " + descriptor.getCharacteristic().getUuid());
                            break;
                        case 0x02:
                            log.i(TAG, "Indication enabled: " + descriptor.getCharacteristic().getUuid());
                            break;
                    }
                }
            }
        } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                || status == 8 /* GATT INSUF AUTHORIZATION */
                || status == 137 /* GATT AUTH FAIL */) {
            String msg = "Authentication required while writing descriptor: " + gattStatusToString(status);
            eventError.postEvent(new GBError(status, msg));
            if (log != null) {
                log.w(TAG, msg);
            }
        } else {
            String msg = "Error on writing descriptor: " + gattStatusToString(status);
            eventError.postEvent(new GBError(status, msg));
            if (log != null) {
                log.e(TAG, msg);
            }
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        final ILogger log = logger;
        if (log != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log.v(TAG, "MTU changed to: " + mtu);
            } else {
                log.w(TAG, "MTU changed to: " + mtu + " status: " + gattStatusToString(status));
            }
        }

        super.onMtuChanged(gatt, mtu, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            eventError.postEvent(new GBError(status, ERROR_MTU_REQUEST));
        }
        if (mMtu != mtu) {
            eventMtu.postEvent(BleGattX.this, EVT_MTU, mtu);
            mMtu = mtu; // 不管成功与否都要保存最新使用的MTU
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public final void onPhyRead(@NonNull final BluetoothGatt gatt,
                                final int txPhy, final int rxPhy,
                                final int status) {
        final ILogger log = logger;
        if (log != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("PHY read (TX: ").append(phyToString(txPhy))
                    .append(", RX: ").append(phyToString(rxPhy)).append(")");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log.v(TAG, sb.toString());
            } else {
                sb.append("), status: ").append(gattStatusToString(status));
                log.w(TAG, sb.toString());
            }
        }

        super.onPhyRead(gatt, txPhy, rxPhy, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventPhy.postEvent(new BlePhy(txPhy, rxPhy));
        } else {
            if (log != null) {
                log.w(TAG, "PHY read failed with status " + status);
            }
            eventError.postEvent(new GBError(status, ERROR_READ_PHY));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public final void onPhyUpdate(@NonNull final BluetoothGatt gatt,
                                  final int txPhy, final int rxPhy,
                                  final int status) {
        final ILogger log = logger;
        if (log != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("PHY updated (TX: ").append(phyToString(txPhy))
                    .append(", RX: ").append(phyToString(rxPhy)).append(")");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log.v(TAG, sb.toString());
            } else {
                sb.append("), status: ").append(gattStatusToString(status));
                log.w(TAG, sb.toString());
            }
        }

        super.onPhyUpdate(gatt, txPhy, rxPhy, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventPhy.postEvent(new BlePhy(txPhy, rxPhy));
        } else {
            if (log != null) {
                log.e(TAG, "PHY update failed with status " + status);
            }
            eventError.postEvent(new GBError(status, ERROR_PHY_UPDATE));
        }
    }

    // This method is hidden in Android Oreo and Pie
    // @Override
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Keep
    public final void onConnectionUpdated(@NonNull final BluetoothGatt gatt,
                                          @IntRange(from = 6, to = 3200) final int interval,
                                          @IntRange(from = 0, to = 499) final int latency,
                                          @IntRange(from = 10, to = 3200) final int timeout,
                                          final int status) {
        final ILogger log = logger;
        if (log != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Connection parameters updated (interval: ")
                    .append(interval * 1.25)
                    .append("ms, latency: ").append(latency)
                    .append(", timeout: ").append(timeout * 10).append("ms)");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log.i(TAG, sb.toString());
            } else {
                sb.append(", status: ").append(gattStatusToString(status));
                log.w(TAG, sb.toString());
            }
        }

        super.onConnectionUpdated(gatt, interval, latency, timeout, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            eventCI.postEvent(new BleCI(interval, latency, timeout));
        } else if (status == 0x3b) { // HCI_ERR_UNACCEPT_CONN_INTERVAL
            if (log != null) {
                log.e(TAG, "Connection parameters update failed with status: UNACCEPT CONN INTERVAL (0x3b)");
            }
            eventError.postEvent(new GBError(status, "UNACCEPT CONN INTERVAL"));
        } else {
            if (log != null) {
                log.e(TAG, "Connection parameters update failed");
            }
            eventError.postEvent(new GBError(status, ERROR_CONNECTION_PRIORITY_REQUEST));
        }
    }

    @Override
    public void onReadRemoteRssi(@NonNull BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        ILogger log = BleGattX.this.logger;
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (log != null) {
                log.i(TAG, "Remote RSSI received: " + rssi + " dBm");
            }
            eventRssi.postEvent(BleGattX.this, EVT_RSSI, rssi);
        } else {
            if (log != null) {
                log.w(TAG, "Reading remote RSSI failed with status " + status);
            }
            eventError.postEvent(new GBError(status, ERROR_READ_RSSI));
        }
    }

    private class BluetoothAdapterStateReceiver extends BroadcastReceiver {

        private String state2String(final int state) {
            switch (state) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    return "TURNING ON";
                case BluetoothAdapter.STATE_ON:
                    return "ON";
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return "TURNING OFF";
                case BluetoothAdapter.STATE_OFF:
                    return "OFF";
                default:
                    return "UNKNOWN (" + state + ")";
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

            ILogger log = logger;
            if (log != null) {
                log.d(TAG, "[Broadcast] Action received: " + BluetoothAdapter.ACTION_STATE_CHANGED +
                        ", state changed from " + state2String(previousState) + " to " + state2String(state));
            }

            eventAdapterStateChanged.postEvent(new BleIntState(previousState, state));

            switch (state) {
                case BluetoothAdapter.STATE_TURNING_OFF:
                    break;
                case BluetoothAdapter.STATE_OFF:
                    tryDisconnect();
                    tryCloseGatt();
                    break;
            }
        }
    }

    private class PairingRequestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            BluetoothDevice targetDevice = mDevice;
            // Skip other devices.
            if (device == null || targetDevice == null || !device.getAddress().equals(targetDevice.getAddress()))
                return;

            // String values are used as the constants are not available for Android 4.3.
            final int variant = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_VARIANT"/*BluetoothDevice.EXTRA_PAIRING_VARIANT*/, 0);
            final ILogger log = logger;
            if (log != null) {
                log.d(TAG, "[Broadcast] Action received: android.bluetooth.device.action.PAIRING_REQUEST"/*BluetoothDevice.ACTION_PAIRING_REQUEST*/ +
                        ", pairing variant: " + BlePairingVariant.toString(variant) + " (" + variant + ")");
            }

            // onPairingRequestReceived(device, variant);
        }
    }

    private class BondStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

            BluetoothDevice targetDevice = mDevice;
            // Skip other devices.
            if (device == null || targetDevice == null || !device.getAddress().equals(targetDevice.getAddress()))
                return;

            ILogger log = logger;
            if (log != null) {
                log.d(TAG, "[Broadcast] Action received: " + BluetoothDevice.ACTION_BOND_STATE_CHANGED +
                        ", bond state changed from " + bondStateToString(previousBondState) + " to " + bondStateToString(bondState));
            }

            eventBondStateChanged.postEvent(new BleIntState(previousBondState, bondState));

            switch (bondState) {
                case BluetoothDevice.BOND_NONE:
                    if (previousBondState == BluetoothDevice.BOND_BONDING) {
                        // TODO: 2020/1/17 从绑定状态到非绑定，表示绑定失败 
                        if (log != null) {
                            log.w(TAG, "Bonding failed");
                        }
                    } else if (previousBondState == BluetoothDevice.BOND_BONDED) {
                        // TODO: 2020/1/17 从已绑定到非绑定，表示解除绑定成功
                        if (log != null) {
                            log.i(TAG, "Bond information removed");
                        }
                    }
                    break;
                case BluetoothDevice.BOND_BONDING:
                    // TODO: 2020/1/17 正在绑定中，需要进一步处理吗？
                    //mCallbacks.onBondingRequired(device);
                    return;
                case BluetoothDevice.BOND_BONDED:
                    if (log != null) {
                        log.i(TAG, "Device bonded");
                    }
                    //mCallbacks.onBonded(device);
                    // TODO: 2020/1/17 绑定成功后，如果还没有发现服务，就开始发现服务
                    // If the device started to pair just after the connection was
                    // established the services were not discovered.
//                    if (!mServicesDiscovered && !mServiceDiscoveryRequested) {
//                        mServiceDiscoveryRequested = true;
//                        mHandler.post(() -> {
//                            log(Log.VERBOSE, "Discovering services...");
//                            log(Log.DEBUG, "gatt.discoverServices()");
//                            mBluetoothGatt.discoverServices();
//                        });
//                        return;
//                    }
                    // On older Android versions, after executing a command on secured attribute
                    // of a device that is not bonded, let's say a write characteristic operation,
                    // the system will start bonding. The BOND_BONDING and BOND_BONDED events will
                    // be received, but the command will not be repeated automatically.
                    //
                    // Test results:
                    // Devices that require repeating the last task:
                    // - Nexus 4 with Android 5.1.1
                    // - Samsung S6 with 5.0.1
                    // - Samsung S8 with Android 7.0
                    // - Nexus 9 with Android 7.1.1
                    // Devices that repeat the request automatically:
                    // - Pixel 2 with Android 8.1.0
                    // - Samsung S8 with Android 8.0.0
                    //
                    // TODO: 2020/1/17 对于安卓8.0之前的版本，绑定成功后，需要判断是否应该再执行因为未绑定而执行失败的动作
//                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//                        if (mRequest != null && mRequest.type != Request.Type.CREATE_BOND) {
//                            // Repeat the last command in that case.
//                            mGattCallback.enqueueFirst(mRequest);
//                            break;
//                        }
//                    }
            }
        }

        private String bondStateToString(final int state) {
            switch (state) {
                case BluetoothDevice.BOND_NONE:
                    return "BOND_NONE";
                case BluetoothDevice.BOND_BONDING:
                    return "BOND_BONDING";
                case BluetoothDevice.BOND_BONDED:
                    return "BOND_BONDED";
                default:
                    return "UNKNOWN";
            }
        }
    }

}
