package com.goodix.ble.libble.v2.impl;

import com.goodix.ble.libble.v2.gb.GBAdvertiser;
import com.goodix.ble.libble.v2.gb.GBPeripheral;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.GBScanner;
import com.goodix.ble.libble.v2.gb.GBWirelessAdapter;
import com.goodix.ble.libble.v2.gb.procedure.GBProcedure;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;

import java.util.List;

/**
 * 作为工厂类，创建其他控制对象
 */
public class BleAdapter implements GBWirelessAdapter {
    @Override
    public void setLogger(ILogger logger) {
        //
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public GBProcedure setEnable(boolean enable) {
        return null;
    }

    @Override
    public GBProcedure reset(boolean toFactory) {
        return null;
    }

    @Override
    public Event<Integer> evtStateChanged() {
        return null;
    }

    @Override
    public Event<String> evtError() {
        return null;
    }

    @Override
    public GBScanner createScanner() {
        return null;
    }

    @Override
    public GBAdvertiser createAdvertiser() {
        return null;
    }

    @Override
    public GBRemoteDevice createDevice(String mac) {
        return null;
    }

    @Override
    public GBPeripheral createPeripheral(String mac) {
        return null;
    }

    @Override
    public List<GBRemoteDevice> getBondDevices() {
        return null;
    }
}
