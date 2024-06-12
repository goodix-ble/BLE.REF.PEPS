package com.goodix.ble.libble.v2.profile;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.gatt.GBGattCharacteristic;
import com.goodix.ble.libble.v2.gb.gatt.GBGattService;
import com.goodix.ble.libble.v2.gb.gatt.profile.GBGattProfile;
import com.goodix.ble.libble.v2.gb.procedure.GBGattProcedureRead;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.ITask;

import java.util.UUID;

public class BatteryService implements IEventListener, GBGattProfile {
    public static final int EVT_BATTERY_UPDATE = 745;
    private final static UUID SERVICE = BleUuid.from(0x180F);
    private final static UUID LEVEL_CHARACTERISTIC = BleUuid.from(0x2A19);

    private GBGattService bas;
    private GBGattCharacteristic level;

    private boolean diff;
    private int lastNotifiedPercent;

    private Event<Integer> updateEvent;

    public boolean bindTo(GBRemoteDevice connection) {
        if (connection == null) {
            return false;
        }

        if (level != null) {
            level.evtNotify().remove(this);
            level.evtRead().clear(this);
        }

        bas = connection.requireService(SERVICE, false);
        level = bas.requireCharacteristic(LEVEL_CHARACTERISTIC, true, false, true);
        level.evtNotify().register(this);
        return true;
    }

    public GBGattService getService() {
        return bas;
    }

    public GBGattCharacteristic getLevel() {
        return level;
    }

    public void setNoRepeat() {
        diff = true;
    }

    public void requestUpdate() {
        GBGattProcedureRead read = level.read();
        read.evtFinished().register(this);
        read.startProcedure();
    }

    public Event<Integer> evtUpdate() {
        if (updateEvent == null) {
            updateEvent = new Event<>(this, EVT_BATTERY_UPDATE);
        }
        lastNotifiedPercent = -1;
        return updateEvent;
    }

    public int getLastLevel() {
        return lastNotifiedPercent;
    }

    @Override
    public void onEvent(Object src, int type, Object evtData) {
        if (src == level && type == GBGattCharacteristic.EVT_NOTIFY) {
            byte[] data = (byte[]) evtData;
            Event<Integer> event = this.updateEvent;
            if (event != null && data != null && data.length == 1) {
                int percent = 0xff & data[0];
                if (!diff || lastNotifiedPercent != percent) {
                    lastNotifiedPercent = percent;
                    event.postEvent(this, EVT_BATTERY_UPDATE, percent);
                }
            }
        } else if (type == ITask.EVT_FINISH && src instanceof GBGattProcedureRead) {
            byte[] value = ((GBGattProcedureRead) src).getValue();
            onEvent(level, GBGattCharacteristic.EVT_NOTIFY, value);
        }
    }
}
