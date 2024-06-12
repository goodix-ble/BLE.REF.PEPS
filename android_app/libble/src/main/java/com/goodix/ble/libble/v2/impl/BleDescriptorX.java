package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import androidx.annotation.Nullable;

import com.goodix.ble.libble.v2.gb.gatt.GBGattCharacteristic;
import com.goodix.ble.libble.v2.gb.gatt.GBGattDescriptor;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureRead;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureWrite;
import com.goodix.ble.libble.v2.impl.procedure.DescriptorRead;
import com.goodix.ble.libble.v2.impl.procedure.DescriptorWrite;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;

import java.util.ArrayList;
import java.util.UUID;

@SuppressWarnings("unused")
public class BleDescriptorX implements GBGattDescriptor, GattChangeListener {

    private UUID uuid;

    private BleCharacteristicX parent;
    private BleRemoteDevice remoteDevice;

    boolean isDefinedByUser = false;
    boolean isMandatory = false;

    // 定义事件，使用懒加载的方式，避免不必要的内存支出
    private Event<byte[]> readEvent = null;
    private Event<byte[]> writtenEvent = null;


    @Nullable
    private BluetoothGattDescriptor gattDescriptor;

    public BleDescriptorX(BleRemoteDevice remoteDevice, BleCharacteristicX parent, UUID uuid) {
        this.remoteDevice = remoteDevice;
        this.parent = parent;
        this.uuid = uuid;
    }

    @Nullable
    public BluetoothGattDescriptor getGattDescriptor() {
        return gattDescriptor;
    }

    @Override
    public GBGattCharacteristic getCharacteristic() {
        return parent;
    }

    @Override
    public int getInstanceId() {
        // 安卓GATT接口的Descriptor没有InstanceId
        return 0;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public byte[] getValue() {
        if (gattDescriptor != null) {
            return gattDescriptor.getValue();
        }
        return new byte[0];
    }

    @Override
    public void setValue(byte[] value) {
        if (gattDescriptor != null) {
            gattDescriptor.setValue(value);
        }
    }

    @Override
    public GBGattProcedureRead read() {
        DescriptorRead read = new DescriptorRead();
        read.setRemoteDevice(remoteDevice);
        read.setTargetDescriptor(this);
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            read.setLogger(logger);
        }
        return read;
    }

    @Override
    public GBGattProcedureWrite write(byte[] value) {
        DescriptorWrite write = new DescriptorWrite();
        write.setRemoteDevice(remoteDevice);
        write.setTargetDescriptor(this);
        write.setValue(value);
        ILogger logger = remoteDevice.getLogger();
        if (logger != null) {
            write.setLogger(logger);
        }
        return write;
    }

    @Override
    public Event<byte[]> evtRead() {
        if (readEvent == null) {
            synchronized (this) {
                if (readEvent == null) {
                    readEvent = new Event<>(this, GBGattDescriptor.EVT_READ);
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
                    writtenEvent = new Event<>(this, GBGattDescriptor.EVT_WRITTEN);
                }
            }
        }
        return writtenEvent;
    }

    @Override
    public void clearEventListener(Object tag) {
        Event<byte[]> tmp = this.readEvent;
        if (tmp != null) {
            tmp.clear(tag);
        }
        tmp = this.writtenEvent;
        if (tmp != null) {
            tmp.clear(tag);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic, int op) {
        return false;
    }

    @Override
    public boolean onDescriptorChanged(BluetoothGattDescriptor descriptor, int op) {
        if (gattDescriptor != null && gattDescriptor.equals(descriptor)) {
            Event<byte[]> tmp = null;
            if (op == GattChangeListener.OP_READ) {
                tmp = this.readEvent;
            } else if (op == GattChangeListener.OP_WRITTEN) {
                tmp = this.writtenEvent;
            }
            if (tmp != null) {
                tmp.postEvent(gattDescriptor.getValue());
            }
            return true;
        }
        return false;
    }

    void onDiscovered(BluetoothGattDescriptor org, ArrayList<String> errors) {
        // 收到 null 就表示这个对象已经被废弃了
        if (org == null) {
            gattDescriptor = null;
            return;
        }

        // 首先需要UUID相同
        if (!uuid.equals(org.getUuid())) {
            ILogger log = remoteDevice.getLogger();
            if (log != null) {
                log.w("GBGattDescriptor", uuid + " is not match to " + org.getUuid());
            }
            return;
        }

        // 存在，但不是同一个实例，丢弃。但安卓没有为Descriptor提供实例ID
        // if (gattDescriptor != null && gattDescriptor.getInstanceId() != org.getInstanceId()) {
        //    return false;
        // }

        gattDescriptor = org;
    }
}
