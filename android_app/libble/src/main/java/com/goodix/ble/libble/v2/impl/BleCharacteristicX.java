package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.v2.gb.gatt.GBGattCharacteristic;
import com.goodix.ble.libble.v2.gb.gatt.GBGattDescriptor;
import com.goodix.ble.libble.v2.gb.gatt.GBGattService;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureRead;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureWrite;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libble.v2.impl.procedure.CharacteristicRead;
import com.goodix.ble.libble.v2.impl.procedure.CharacteristicWrite;
import com.goodix.ble.libble.v2.impl.procedure.NotificationEnable;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BleCharacteristicX implements GBGattCharacteristic, GattChangeListener {
    private BleRemoteDevice remoteDevice;
    private BleServiceX parent;
    private UUID uuid;

    private BluetoothGattCharacteristic gattCharacteristic;

    boolean isDefinedByUser = false;
    boolean isMandatory = false;
    public boolean needEnableNotify = false; // 使用 public 访问修饰符，节省2个getter函数
    public boolean needEnableIndicate = false;

    // 存储自己的描述符
    @Nullable
    private ArrayList<BleDescriptorX> descriptorList;

    // 定义事件，使用懒加载的方式，避免不必要的内存支出
    private Event<byte[]> readEvent = null;
    private Event<byte[]> writtenEvent = null;
    private Event<byte[]> notifyEvent = null;
    private Event<byte[]> indicateEvent = null;

    BleCharacteristicX(BleRemoteDevice remoteDevice, BleServiceX parent, UUID uuid) {
        this.remoteDevice = remoteDevice;
        this.parent = parent;
        this.uuid = uuid;
    }

    public BluetoothGattCharacteristic getGattCharacteristic() {
        return gattCharacteristic;
    }

    @Override
    public GBGattService getService() {
        return parent;
    }

    @Override
    public int getInstanceId() {
        if (gattCharacteristic != null) {
            return gattCharacteristic.getInstanceId();
        }
        return 0;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public GBGattDescriptor defineDescriptor(UUID uuid, boolean mandatory) {
        if (descriptorList == null) {
            descriptorList = new ArrayList<>();
        }
        BleDescriptorX descriptor = new BleDescriptorX(remoteDevice, this, uuid);
        descriptor.isDefinedByUser = true;
        descriptor.isMandatory = mandatory;
        descriptorList.add(descriptor);
        return descriptor;
    }

    @Override
    public GBGattDescriptor requireDescriptor(UUID uuid, boolean mandatory) {
        GBGattDescriptor descriptor = null;
        synchronized (this) {
            if (descriptorList != null) {
                for (GBGattDescriptor x : descriptorList) {
                    if (x.getUuid().equals(uuid)) {
                        descriptor = x;
                        BleDescriptorX tmp = (BleDescriptorX) x;
                        tmp.isDefinedByUser = true;
                        tmp.isMandatory = mandatory;
                        break;
                    }
                }
            }
        }
        if (descriptor == null) {
            descriptor = defineDescriptor(uuid, mandatory);
        }
        return descriptor;
    }

    @Override
    public List<GBGattDescriptor> getDescriptor(UUID uuid) {
        if (descriptorList == null || descriptorList.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GBGattDescriptor> ret = new ArrayList<>();
        for (BleDescriptorX x : descriptorList) {
            if (x.getUuid().equals(uuid)) {
                ret.add(x);
            }
        }
        return ret;
    }

    @Override
    public List<GBGattDescriptor> getDescriptors() {
        if (descriptorList == null || descriptorList.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(descriptorList);
    }

    @Override
    public int getProperty() {
        if (gattCharacteristic != null) {
            return gattCharacteristic.getProperties();
        }
        return 0;
    }

    @Override
    public byte[] getValue() {
        if (gattCharacteristic != null) {
            return gattCharacteristic.getValue();
        }
        return new byte[0];
    }

    @Override
    public void setValue(byte[] value) {
        if (gattCharacteristic != null) {
            gattCharacteristic.setValue(value);
        }
    }

    @Override
    public boolean writeDirectly(boolean withResponse, boolean useSign, byte[] value) {
        if (parent != null && gattCharacteristic != null) {
            BleGattX gattX = ((BleRemoteDevice) parent.getRemoteDevice()).getGatt();
            int type = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
            if (withResponse) {
                type = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
            } else if (useSign) {
                type = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;
            }
            gattCharacteristic.setWriteType(type);
            gattCharacteristic.setValue(value);
            return gattX.tryWriteCharacteristic(gattCharacteristic);
        }
        return false;
    }

    @Override
    public GBGattProcedureRead read() {
        CharacteristicRead proc = new CharacteristicRead();
        proc.setRemoteDevice(remoteDevice);
        proc.setTargetCharacteristic(this);
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            proc.setLogger(logger);
        }
        return proc;
    }

    @Override
    public GBGattProcedureWrite writeByRequest(byte[] value) {
        CharacteristicWrite proc = new CharacteristicWrite();
        proc.setRemoteDevice(remoteDevice);
        proc.setTargetCharacteristic(this);
        proc.setValue(value);
        proc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            proc.setLogger(logger);
        }
        return proc;
    }

    @Override
    public GBGattProcedureWrite writeByCommand(byte[] value, boolean useSign) {
        CharacteristicWrite proc = new CharacteristicWrite();
        proc.setRemoteDevice(remoteDevice);
        proc.setTargetCharacteristic(this);
        proc.setValue(value);
        proc.setWriteType(useSign ? BluetoothGattCharacteristic.WRITE_TYPE_SIGNED : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            proc.setLogger(logger);
        }
        return proc;
    }

    @Override
    public GBProcedure setEnableNotify(boolean enable) {
        NotificationEnable proc = new NotificationEnable();
        proc.setRemoteDevice(remoteDevice);
        proc.setTargetCharacteristic(this);
        proc.setEnable(enable);
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            proc.setLogger(logger);
        }
        return proc;
    }

    @Override
    public GBProcedure setEnableIndicate(boolean enable) {
        NotificationEnable proc = new NotificationEnable();
        proc.setRemoteDevice(remoteDevice);
        proc.setTargetCharacteristic(this);
        proc.setEnable(enable);
        proc.setForIndicate();
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            proc.setLogger(logger);
        }
        return proc;
    }

    @Override
    public boolean isNotifyEnabled() {
        return checkCCCD(0x01);
    }

    @Override
    public boolean isIndicateEnabled() {
        return checkCCCD(0x02);
    }

    @Override
    public Event<byte[]> evtRead() {
        if (readEvent == null) {
            synchronized (this) {
                if (readEvent == null) {
                    readEvent = new Event<>(this, EVT_READ);
                }
            }
        }
        return readEvent;
    }

    @Override
    public Event<byte[]> evtWritten() {
        if (writtenEvent == null) {
            synchronized (this) {
                if (writtenEvent == null) {
                    writtenEvent = new Event<>(this, EVT_WRITTEN);
                }
            }
        }
        return writtenEvent;
    }

    @Override
    public Event<byte[]> evtNotify() {
        if (notifyEvent == null) {
            synchronized (this) {
                if (notifyEvent == null) {
                    notifyEvent = new Event<>(this, EVT_NOTIFY);
                }
            }
        }
        return notifyEvent;
    }

    @Override
    public Event<byte[]> evtIndicate() {
        if (indicateEvent == null) {
            synchronized (this) {
                if (indicateEvent == null) {
                    indicateEvent = new Event<>(this, EVT_INDICATE);
                }
            }
        }
        return indicateEvent;
    }

    @Override
    public void clearEventListener(Object tag) {
        ArrayList<Event> events = new ArrayList<>(4);
        events.add(readEvent);
        events.add(writtenEvent);
        events.add(notifyEvent);
        events.add(indicateEvent);
        for (Event event : events) {
            if (event != null) {
                event.clear(tag);
            }
        }
    }

    private boolean checkCCCD(int bitMask) {
        BluetoothGattCharacteristic ch = this.gattCharacteristic;
        if (ch != null) {
            BluetoothGattDescriptor cccd = ch.getDescriptor(BleUuid.CCCD);
            if (cccd != null) {
                byte[] data = cccd.getValue();
                if (data != null && data.length == 2 && data[1] == 0x00) {
                    return (0xFF & data[0] & bitMask) == bitMask;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic, int op) {
        if (gattCharacteristic != null && gattCharacteristic.equals(characteristic)) {
            Event<byte[]> tmp = null;
            if (op == GattChangeListener.OP_READ) {
                tmp = this.readEvent;
            } else if (op == GattChangeListener.OP_WRITTEN) {
                tmp = this.writtenEvent;
            } else if (op == GattChangeListener.OP_NOTIFY) {
                tmp = this.notifyEvent;
            } else if (op == GattChangeListener.OP_INDICATE) {
                tmp = this.indicateEvent;
            }
            if (tmp != null) {
                tmp.postEvent(characteristic.getValue());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onDescriptorChanged(BluetoothGattDescriptor descriptor, int op) {
        if (descriptorList != null && gattCharacteristic != null && gattCharacteristic.equals(descriptor.getCharacteristic())) {
            for (BleDescriptorX x : descriptorList) {
                if (x.onDescriptorChanged(descriptor, op)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDiscovered() {
        return gattCharacteristic != null;
    }

    void onDiscovered(BluetoothGattCharacteristic org, ArrayList<String> errors) {
        // 收到 null 就表示这个对象已经被废弃了
        if (org == null) {
            gattCharacteristic = null;
            if (descriptorList != null) {
                for (BleDescriptorX x : descriptorList) {
                    x.onDiscovered(null, errors);
                }
                //descriptorList.clear();
            }
            return;
        }

        final ILogger log = remoteDevice.getLogger();

        // 首先需要UUID相同
        if (!uuid.equals(org.getUuid())) {
            if (log != null) {
                log.w("GBGattCharacteristic", uuid + " is not match to " + org.getUuid());
            }
            return;
        }

//        // 存在，但不是同一个实例，丢弃
//        if (gattCharacteristic != null && gattCharacteristic.getInstanceId() != org.getInstanceId()) {
//            return false;
//        } 对实例的引用没有释放的地方，如果该类被复用时，如果UUID一样但InstanceId不一样，就会导致永远无法讲原始类型与包装类型对应起来

        gattCharacteristic = org;

        // 待更新描述符列表
        List<BluetoothGattDescriptor> descriptors = org.getDescriptors();

        if (descriptors.isEmpty()) {
            return;
        }
        if (descriptorList == null) {
            descriptorList = new ArrayList<>(descriptors.size());
        } else {
            descriptorList.ensureCapacity(descriptors.size());
        }

        // 待更新列表
        HashMap<UUID, ArrayList<BleDescriptorX>> oldDescriptorMap = new HashMap<>(this.descriptorList.size());

        // 移动到 MAP 中
        for (BleDescriptorX service : descriptorList) {
            UUID key = service.getUuid();
            ArrayList<BleDescriptorX> values = oldDescriptorMap.get(key);
            if (values == null) {
                values = new ArrayList<>(4);
                oldDescriptorMap.put(key, values);
            }
            values.add(service);
        }
        descriptorList.clear();


        for (BluetoothGattDescriptor orgDescriptor : descriptors) {
            UUID uuid = orgDescriptor.getUuid();
            ArrayList<BleDescriptorX> foundDescriptors = oldDescriptorMap.get(uuid);
            if (foundDescriptors == null || foundDescriptors.isEmpty()) {
                Log.d("onDiscovered", "    D +->" + uuid);
                // 没有就创建一个
                BleDescriptorX tmp = new BleDescriptorX(remoteDevice, this, uuid);
                tmp.onDiscovered(orgDescriptor, errors);
                descriptorList.add(tmp);
            } else {
                Log.d("onDiscovered", "    D =->" + uuid);
                // 对找到的服务进行初始化
                BleDescriptorX foundDescriptor = foundDescriptors.remove(0);
                foundDescriptor.onDiscovered(orgDescriptor, errors);
                // 将其中一个移动到 descriptorList 中
                descriptorList.add(foundDescriptor);
                // 如果为空了，就从map中移除，方便后面判断还有多少没有处理的
                if (foundDescriptors.isEmpty()) {
                    oldDescriptorMap.remove(uuid);
                }
            }
        }

        for (ArrayList<BleDescriptorX> oldDescriptors : oldDescriptorMap.values()) {
            for (BleDescriptorX oldDescriptor : oldDescriptors) {
                if (oldDescriptor.isMandatory) {
                    errors.add("Characteristic " + uuid + " does not find required descriptor: " + oldDescriptor.getUuid());
                }
                if (oldDescriptor.isDefinedByUser) {
                    descriptorList.add(oldDescriptor);
                }
                // 表示没有和原生对象匹配上
                oldDescriptor.onDiscovered(null, errors);
            }
        }
    }
}
