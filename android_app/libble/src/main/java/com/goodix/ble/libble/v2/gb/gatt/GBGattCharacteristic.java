package com.goodix.ble.libble.v2.gb.gatt;

import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureRead;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureWrite;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libcomx.event.Event;

import java.util.List;
import java.util.UUID;

public interface GBGattCharacteristic {
    int EVT_READ = 33;
    int EVT_WRITTEN = 44;
    int EVT_NOTIFY = 55;
    int EVT_INDICATE = 66;

    ///////////////////////////////////////////////////////////////////////////
    // Basic info
    ///////////////////////////////////////////////////////////////////////////
    GBGattService getService();

    int getInstanceId();

    UUID getUuid();

    GBGattDescriptor defineDescriptor(final UUID uuid, boolean mandatory);

    /**
     * 返回一个已经存在的描述符，如果不存在，就通过{@link #defineDescriptor(UUID, boolean)}定义一个服务
     */
    GBGattDescriptor requireDescriptor(final UUID uuid, boolean mandatory);

    List<GBGattDescriptor> getDescriptor(final UUID uuid);

    List<GBGattDescriptor> getDescriptors();

    /**
     * read, write, notify, indicate
     * | 7  | 6  | 5  | 4  | 3  | 2  | 1  | 0  |
     * +----+----+----+----+----+----+----+----+
     * |EXT | WS | I  | N  | WR | WC | RD | B  |
     * Please refer {@link android.bluetooth.BluetoothGattCharacteristic#PROPERTY_READ} and other properties for android OS.
     */
    int getProperty();

    byte[] getValue();

    void setValue(byte[] value);

    boolean writeDirectly(boolean withResponse, boolean useSign, byte[] value);

    ///////////////////////////////////////////////////////////////////////////
    // Data transceiver
    ///////////////////////////////////////////////////////////////////////////

    GBGattProcedureRead read();

    GBGattProcedureWrite writeByRequest(byte[] value); // 写入有2种方式： Request(with response)、Command(without response)

    GBGattProcedureWrite writeByCommand(byte[] value, boolean useSign);

    GBProcedure setEnableNotify(boolean enable);

    GBProcedure setEnableIndicate(boolean enable);

    boolean isNotifyEnabled();

    boolean isIndicateEnabled();

    Event<byte[]> evtRead();

    Event<byte[]> evtWritten();

    Event<byte[]> evtNotify();

    Event<byte[]> evtIndicate();

    void clearEventListener(Object tag);
}
