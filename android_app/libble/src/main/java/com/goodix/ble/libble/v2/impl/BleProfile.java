package com.goodix.ble.libble.v2.impl;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.gatt.profile.GBGattProfile;

public class BleProfile implements GBGattProfile {
    @Override
    public boolean bindTo(GBRemoteDevice device) {
        return false;
    }
}
