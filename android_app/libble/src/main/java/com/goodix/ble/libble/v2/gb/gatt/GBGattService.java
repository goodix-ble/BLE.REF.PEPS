package com.goodix.ble.libble.v2.gb.gatt;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;

import java.util.List;
import java.util.UUID;

public interface GBGattService {
    ///////////////////////////////////////////////////////////////////////////
    // Basic info
    ///////////////////////////////////////////////////////////////////////////
    GBRemoteDevice getRemoteDevice();

    int getInstanceId();

    UUID getUuid();

    GBGattCharacteristic defineCharacteristic(final UUID uuid, boolean mandatory, boolean enableIndicate, boolean enableNotify);

    /**
     * 返回一个已经存在的特性，如果特性不存在，就通过{@link #defineCharacteristic(UUID, boolean, boolean, boolean)}定义一个服务
     */
    GBGattCharacteristic requireCharacteristic(final UUID uuid, boolean mandatory, boolean enableIndicate, boolean enableNotify);

    List<GBGattCharacteristic> getCharacteristic(final UUID uuid);

    List<GBGattCharacteristic> getCharacteristics();

    List<GBGattService> getIncludeService();

    void clearEventListener(Object tag);

    /**
     * 从设备中移除对该服务的预先定义。
     *
     * @return true -  如果成功从设备中移除该服务的定义
     * false - 如果该服务不是预先定义的，或者不存在于设备的预定义列表中
     */
    boolean undefineService();
}
