package com.goodix.ble.libble.v2.gb.gatt.profile;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;

import java.util.UUID;

public interface GBGattProfile {
    // 因为满足 GB 接口的类并不一定通用，所以，不能作为传入参数。
//    interface CharacteristicDefineHelper {
//        GBGattCharacteristic getCharacteristic();
//
//        GBGattDescriptor defineDescriptor(UUID uuid);
//    }
//
//    interface ServiceDefineHelper {
//        GBGattService getService();
//
//        CharacteristicDefineHelper defineCharacteristic(UUID uuid);
//    }
//
//    ServiceDefineHelper defineService(UUID uuid);
//
//    void removeService(GBGattService service);
//
//    List<GBGattService> getService(UUID uuid);
//
//    /**
//     * 因为，一个Profile只应该绑定到一个设备上，所以该功能让Profile来实现更容易
//     *
//     * 在实现类中，实现类需要利用对GBRemoteDevice实现类的访问可见性，
//     * 将自身定义好的服务（或者自身）添加到GBRemoteDevice实现类中
//     */
//    boolean bindTo(GBRemoteDevice device);

    /**
     * 利用 {@link GBRemoteDevice#defineService(UUID, boolean)} 定义需要的服务
     */
    boolean bindTo(GBRemoteDevice device);
}
