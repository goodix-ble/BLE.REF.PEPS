package com.goodix.ble.libble.v2.gb.gatt;

import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureRead;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureWrite;
import com.goodix.ble.libcomx.event.Event;

import java.util.UUID;

public interface GBGattDescriptor {
    int EVT_READ = 0x1101;
    int EVT_WRITTEN = 0x1102;

    ///////////////////////////////////////////////////////////////////////////
    // Basic info
    ///////////////////////////////////////////////////////////////////////////
    GBGattCharacteristic getCharacteristic();

    int getInstanceId();

    UUID getUuid();

    byte[] getValue();

    void setValue(byte[] value);

    ///////////////////////////////////////////////////////////////////////////
    // Data transceiver
    ///////////////////////////////////////////////////////////////////////////

    GBGattProcedureRead read();

    GBGattProcedureWrite write(byte[] value);

    Event<byte[]> evtRead();

    Event<byte[]> evtWritten();

    void clearEventListener(Object tag);
}
