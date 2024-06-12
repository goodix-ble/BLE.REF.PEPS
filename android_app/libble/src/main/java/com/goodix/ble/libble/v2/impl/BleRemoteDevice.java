package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.gatt.GBGattService;
import com.goodix.ble.libble.v2.gb.pojo.GBCI;
import com.goodix.ble.libble.v2.gb.pojo.GBError;
import com.goodix.ble.libble.v2.gb.pojo.GBPhy;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedureConnect;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedureRssiRead;
import com.goodix.ble.libble.v2.impl.procedure.BondCreate;
import com.goodix.ble.libble.v2.impl.procedure.BondRemove;
import com.goodix.ble.libble.v2.impl.procedure.CiSet;
import com.goodix.ble.libble.v2.impl.procedure.GattConnect;
import com.goodix.ble.libble.v2.impl.procedure.GattDisconnect;
import com.goodix.ble.libble.v2.impl.procedure.GattDiscover;
import com.goodix.ble.libble.v2.impl.procedure.MtuExchange;
import com.goodix.ble.libble.v2.impl.procedure.PhyRead;
import com.goodix.ble.libble.v2.impl.procedure.PhySet;
import com.goodix.ble.libble.v2.impl.procedure.RssiRead;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITask;
import com.goodix.ble.libcomx.task.ITaskResult;
import com.goodix.ble.libcomx.task.TaskQueue;
import com.goodix.ble.libcomx.util.AccessLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BleRemoteDevice implements GBRemoteDevice {
    private static final String TAG = BleRemoteDevice.class.getSimpleName();

    private ILogger logger;
    private BluetoothDevice targetDevice;
    private BleGattX myGatt;

    private ArrayList<BleServiceX> serviceList = new ArrayList<>(16);

    private GBPhy mPhy = new GBPhy();
    private GBCI mCI = new GBCI();
    boolean discovered = false;

    @Nullable
    private TaskQueue setupSteps = null;
    private boolean ready;
    public boolean expectConnection;

    // 懒创建
    @Nullable
    private Event<GBCI> ciUpdatedEvent;
    @Nullable
    private Event<GBError> errorEvent;
    @Nullable
    private Event<Integer> mtuUpdatedEvent;
    @Nullable
    private Event<GBPhy> phyUpdatedEvent;
    @Nullable
    private Event<Integer> stateChangedEvent;
    @Nullable
    private Event<Boolean> eventReady;

    public BleRemoteDevice(Context ctx) {
        this.myGatt = new BleGattX(ctx);
        this.myGatt.register(new InnerCb());
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        targetDevice = device;
        myGatt.setDevice(targetDevice);
        final ILogger log = logger;
        if (log != null) {
            log.v(TAG, "setBluetoothDevice: " + device.getName() + " " + device.getAddress());
        }
    }

    public BluetoothDevice getBluetoothDevice() {
        return targetDevice;
    }

    public BleGattX getGatt() {
        return myGatt;
    }

    public synchronized boolean removeService(BleServiceX service) {
        return serviceList.remove(service);
    }

    /**
     * 主要是为了在 {@link com.goodix.ble.libble.v2.impl.procedure.GattDiscover} 中能够判断哪些特性
     * 需要在发现服务的时候直接使能通知。
     */
    public ArrayList<BleServiceX> getServiceList() {
        return serviceList;
    }

    /**
     * 主要是为了在 {@link com.goodix.ble.libble.v2.impl.procedure.GattDiscover} 中
     * 能标记是否完成了服务发现规程
     */
    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }

    @Override
    public ILogger getLogger() {
        return logger;
    }

    public void dispose() {
        clearEventListener(null);
        myGatt.clearListener(null);
        if (myGatt.getGatt() != null) {
            myGatt.tryCloseGatt();
        }
    }

    @Nullable
    public synchronized BleServiceX getService(BluetoothGattService service) {
        for (BleServiceX existService : serviceList) {
            if (existService.equals(service)) {
                return existService;
            }
        }
        return null;
    }


    @Override
    public void setLogger(ILogger logger) {
        this.logger = logger;
        myGatt.setLogger(logger);

        TaskQueue queue = this.setupSteps;
        if (queue != null) {
            queue.setLogger(logger);

        }
    }

    @Override
    public String getName() {
        String name = null;
        if (targetDevice != null) {
            name = targetDevice.getName();
        }
        return name == null ? "N/A" : name;
    }

    @Override
    public String getAddress() {
        if (targetDevice != null) {
            return targetDevice.getAddress();
        }
        return "00:00:00:00:00:00";
    }

    @Override
    public int getState() {
        return myGatt.getConnectionState();
    }

    @Override
    public boolean isConnected() {
        return myGatt.isConnected();
    }

    @Override
    public boolean isDisconnected() {
        return myGatt.getConnectionState() == GBRemoteDevice.STATE_DISCONNECTED;
    }

    @Override
    public boolean isDiscovered() {
        return discovered;
    }

    @Override
    public boolean isBond() {
        return targetDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    @Override
    public int getMtu() {
        return myGatt.getMtu();
    }

    @Override
    public GBPhy getPhy() {
        return mPhy;
    }

    @Override
    public GBCI getConnectionParameter() {
        return mCI;
    }

    @Override
    public TaskQueue getSetupSteps() {
        if (setupSteps == null) {
            synchronized (this) {
                if (setupSteps == null) {
                    setupSteps = new TaskQueue();
                    setupSteps.setAbortOnException(true);
                    setupSteps.setLogger(logger);
                }
            }
        }
        return setupSteps;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public boolean isInService() {
        return expectConnection;
    }

    @Override
    public Event<Boolean> evtReady() {
        if (eventReady == null) {
            synchronized (this) {
                if (eventReady == null) {
                    eventReady = new Event<>(this, EVT_READY);
                }
            }
        }
        return eventReady;
    }

    @Override
    public Event<Integer> evtStateChanged() {
        if (stateChangedEvent == null) {
            synchronized (this) {
                if (stateChangedEvent == null) {
                    stateChangedEvent = new Event<>(this, EVT_STATE_CHANGED);
                    myGatt.evtStateChanged().register2((s, t, d) -> {
                        if (d != GBRemoteDevice.STATE_CONNECTED) {
                            discovered = false;
                        }
                        stateChangedEvent.postEvent(d);
                    });
                }
            }
        }
        return stateChangedEvent;
    }

    @Override
    public Event<Integer> evtMtuUpdated() {
        if (mtuUpdatedEvent == null) {
            synchronized (this) {
                if (mtuUpdatedEvent == null) {
                    mtuUpdatedEvent = new Event<>(this, EVT_MTU_UPDATED);
                }
            }
        }
        return mtuUpdatedEvent;
    }

    @Override
    public Event<GBPhy> evtPhyUpdated() {
        if (phyUpdatedEvent == null) {
            synchronized (this) {
                if (phyUpdatedEvent == null) {
                    phyUpdatedEvent = new Event<>(this, EVT_PHY_UPDATED);
                }
            }
        }
        return phyUpdatedEvent;
    }

    @Override
    public Event<GBCI> evtCIUpdated() {
        if (ciUpdatedEvent == null) {
            synchronized (this) {
                if (ciUpdatedEvent == null) {
                    ciUpdatedEvent = new Event<>(this, EVT_CI_UPDATED);
                }
            }
        }
        return ciUpdatedEvent;
    }

    @Override
    public Event<GBError> evtError() {
        if (errorEvent == null) {
            synchronized (this) {
                if (errorEvent == null) {
                    errorEvent = new Event<>(this, EVT_ERROR);
                    myGatt.evtError().register2((s, t, d) -> errorEvent.postEvent(d));
                }
            }
        }
        return errorEvent;
    }

    @Override
    public void clearEventListener(Object tag) {
        clearEventListener(ciUpdatedEvent, tag);
        clearEventListener(errorEvent, tag);
        clearEventListener(mtuUpdatedEvent, tag);
        clearEventListener(phyUpdatedEvent, tag);
        clearEventListener(stateChangedEvent, tag);
        clearEventListener(eventReady, tag);
    }

    @Override
    public void clearPendingProcedure() {
        final ILogger log = logger;
        if (log != null) {
            log.v(TAG, "Clear all pending procedures");
            for (Object requester : locker.getPendingList(null)) {
                if (requester instanceof GBProcedure) {
                    GBProcedure procedure = (GBProcedure) requester;
                    procedure.abort();
                    log.v(TAG, "remove pending procedure: " + procedure.getName());
                }
            }
        }
    }

    private AccessLock locker = new AccessLock();

    public AccessLock getLocker() {
        return locker;
    }

    private void clearEventListener(Event evt, Object tag) {
        if (evt != null) {
            evt.clear(tag);
        }
    }

    @Override
    public GBProcedureConnect connect(int preferredPhy) {
        GattConnect connect = new GattConnect();
        connect.setRemoteDevice(this);
        connect.setPreferredPhy(preferredPhy);
        if (logger != null) {
            connect.setLogger(logger);
        }
        return connect;
    }

    @Override
    public GBProcedure disconnect(boolean clearCache) {
        GattDisconnect disconnect = new GattDisconnect();
        disconnect.setRemoteDevice(this);
        disconnect.setClearCache(clearCache);
        if (logger != null) {
            disconnect.setLogger(logger);
        }
        return disconnect;
    }

    @Override
    public GBProcedure discoverServices() {
        GattDiscover discover = new GattDiscover();
        discover.setRemoteDevice(this);
        if (logger != null) {
            discover.setLogger(logger);
        }
        return discover;
    }

    @Override
    public GBProcedureRssiRead readRemoteRssi() {
        RssiRead read = new RssiRead();
        read.setRemoteDevice(this);
        if (logger != null) {
            read.setLogger(logger);
        }
        return read;
    }

    @Override
    public GBProcedure setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
        PhySet phy = new PhySet();
        phy.setRemoteDevice(this);
        phy.setPhy(txPhy, rxPhy, phyOptions);
        if (logger != null) {
            phy.setLogger(logger);
        }
        return phy;
    }

    @Override
    public GBProcedure readCurrentPhy() {
        PhyRead phy = new PhyRead();
        phy.setRemoteDevice(this);
        if (logger != null) {
            phy.setLogger(logger);
        }
        return phy;
    }

    @Override
    public GBProcedure setConnectionPriority(int connectionPriority) {
        CiSet ci = new CiSet();
        ci.setRemoteDevice(this);
        ci.setPriority(connectionPriority);
        if (logger != null) {
            ci.setLogger(logger);
        }
        return ci;
    }

    @Override
    public GBProcedure setMtu(int mtu) {
        MtuExchange exchange = new MtuExchange();
        exchange.setRemoteDevice(this);
        exchange.setMtu(mtu);
        if (logger != null) {
            exchange.setLogger(logger);
        }
        return exchange;
    }

    @Override
    public GBProcedure createBond() {
        BondCreate bond = new BondCreate();
        bond.setRemoteDevice(this);
        if (logger != null) {
            bond.setLogger(logger);
        }
        return bond;
    }

    @Override
    public GBProcedure removeBond() {
        BondRemove bond = new BondRemove();
        bond.setRemoteDevice(this);
        if (logger != null) {
            bond.setLogger(logger);
        }
        return bond;
    }

    @Override
    public GBGattService defineService(UUID uuid, boolean mandatory) {
        BleServiceX service = new BleServiceX(this, uuid);
        service.isDefinedByUser = true;
        service.isMandatory = mandatory;
        synchronized (this) {
            serviceList.add(service);
        }
        return service;
    }

    @Override
    public GBGattService requireService(UUID uuid, boolean mandatory) {
        GBGattService service = null;
        synchronized (this) {
            for (GBGattService x : serviceList) {
                if (x.getUuid().equals(uuid)) {
                    service = x;
                    break;
                }
            }
        }
        if (service == null) {
            service = defineService(uuid, mandatory);
        }
        return service;
    }

    @Override
    public synchronized List<GBGattService> getService(UUID uuid) {
        if (serviceList.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GBGattService> ret = new ArrayList<>();
        for (GBGattService x : serviceList) {
            if (x.getUuid().equals(uuid)) {
                ret.add(x);
            }
        }
        return ret;
    }

    @Override
    public synchronized List<GBGattService> getServices() {
        if (serviceList.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(serviceList);
    }

    public synchronized void onDiscovered(BluetoothGatt gatt, ArrayList<String> errors) {
        if (gatt == null) {
            errors.add("gatt is null");
            return;
        }

        final ILogger log = logger;

        List<BluetoothGattService> services = gatt.getServices();
        this.serviceList.ensureCapacity(services.size());

        // 待更新列表
        HashMap<UUID, ArrayList<BleServiceX>> oldServiceMap = new HashMap<>(this.serviceList.size());

        // 移动到 MAP 中
        for (BleServiceX service : serviceList) {
            UUID key = service.getUuid();
            ArrayList<BleServiceX> values = oldServiceMap.get(key);
            if (values == null) {
                values = new ArrayList<>(4);
                oldServiceMap.put(key, values);
            }
            values.add(service);

            if (log != null) {
                log.v(TAG, "old service: " + key.toString() + "  #" + service.getInstanceId());
            }
        }
        serviceList.clear();


        for (BluetoothGattService orgService : services) {
            UUID uuid = orgService.getUuid();
            ArrayList<BleServiceX> foundServices = oldServiceMap.get(uuid);
            if (foundServices == null || foundServices.isEmpty()) {
                Log.d("onDiscovered", "S +->    " + uuid);
                // 没有就创建一个
                BleServiceX tmp = new BleServiceX(this, uuid);
                tmp.onDiscovered(orgService, errors);
                serviceList.add(tmp);
                if (log != null) {
                    log.v(TAG, "add service: " + uuid.toString() + "  #" + tmp.getInstanceId());
                }
            } else {
                Log.d("onDiscovered", "S =->    " + uuid);
                // 对找到的服务进行初始化
                BleServiceX foundService = foundServices.remove(0);
                foundService.onDiscovered(orgService, errors);
                // 将其中一个移动到 serviceList 中
                serviceList.add(foundService);
                // 如果为空了，就从map中移除，方便后面判断还有多少没有处理的
                if (foundServices.isEmpty()) {
                    oldServiceMap.remove(uuid);
                }
                if (log != null) {
                    log.v(TAG, "update service: " + uuid.toString() + "  #" + foundService.getInstanceId());
                }
            }
        }

        for (ArrayList<BleServiceX> oldServices : oldServiceMap.values()) {
            for (BleServiceX oldService : oldServices) {
                if (oldService.isMandatory) {
                    errors.add("Device " + getAddress() + " does not find required service: " + oldService.getUuid());
                }
                // 用户预定义的服务必须留下来，否则会丢失其定义好的事件处理方法
                if (oldService.isDefinedByUser) {
                    serviceList.add(oldService);
                    if (log != null) {
                        log.v(TAG, "remain service: " + oldService.getUuid() + "  #" + oldService.getInstanceId());
                    }
                } else {
                    if (log != null) {
                        log.v(TAG, "discard service: " + oldService.getUuid() + "  #" + oldService.getInstanceId());
                    }
                }
                // 表示已经被移除
                oldService.onDiscovered(null, errors);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    class InnerCb extends BluetoothGattCallback implements IEventListener {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            BluetoothGattDescriptor cccd = characteristic.getDescriptor(BleUuid.CCCD);
            final boolean notifications = cccd == null || cccd.getValue() == null ||
                    cccd.getValue().length != 2 || cccd.getValue()[0] == 0x01;

            int op = notifications ? GattChangeListener.OP_NOTIFY : GattChangeListener.OP_INDICATE;
            synchronized (BleRemoteDevice.this) {
                for (BleServiceX x : serviceList) {
                    x.onCharacteristicChanged(characteristic, op);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                synchronized (BleRemoteDevice.this) {
                    for (BleServiceX x : serviceList) {
                        x.onCharacteristicChanged(characteristic, GattChangeListener.OP_READ);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                synchronized (BleRemoteDevice.this) {
                    for (BleServiceX x : serviceList) {
                        x.onCharacteristicChanged(characteristic, GattChangeListener.OP_WRITTEN);
                    }
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                synchronized (BleRemoteDevice.this) {
                    for (BleServiceX x : serviceList) {
                        x.onDescriptorChanged(descriptor, GattChangeListener.OP_READ);
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                synchronized (BleRemoteDevice.this) {
                    for (BleServiceX x : serviceList) {
                        x.onDescriptorChanged(descriptor, GattChangeListener.OP_WRITTEN);
                    }
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS || mtu != myGatt.getMtu()) {
                Event<Integer> evt = BleRemoteDevice.this.mtuUpdatedEvent;
                if (evt != null) {
                    evt.postEvent(mtu);
                }
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            //super.onPhyRead(gatt, txPhy, rxPhy, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mPhy.txPhy = txPhy;
                mPhy.rxPhy = rxPhy;
                Event<GBPhy> evt = BleRemoteDevice.this.phyUpdatedEvent;
                if (evt != null) {
                    evt.postEvent(mPhy);
                }
            }
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            //super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @SuppressWarnings("unused")
        @Keep
        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout,
                                        int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mCI.interval = interval;
                mCI.latency = latency;
                mCI.timeout = timeout;
                Event<GBCI> evt = BleRemoteDevice.this.ciUpdatedEvent;
                if (evt != null) {
                    evt.postEvent(mCI);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            ready = false;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                TaskQueue setup = setupSteps;
                if (setup != null && setup.getTaskCount() > 0) {
                    setup.evtFinished().register(this);
                    setup.start(null, null);
                }
            }
        }

        @Override
        public void onEvent(Object src, int evtType, Object evtData) {
            if (src == setupSteps && evtType == ITask.EVT_FINISH) {
                ITaskResult result = (ITaskResult) evtData;
                Event<Boolean> evtReady = BleRemoteDevice.this.eventReady;
                if (result.getError() != null) {
                    ready = false;
                    if (evtReady != null) {
                        evtReady.postEvent(false);
                    }

                    Event<GBError> evt = errorEvent;
                    if (evt != null) {
                        evt.postEvent(new GBError(result.getCode(), result.getError().getMessage()));
                    }

                } else {
                    ready = true;
                    if (evtReady != null) {
                        evtReady.postEvent(true);
                    }
                }
            }
        }
    }
}
