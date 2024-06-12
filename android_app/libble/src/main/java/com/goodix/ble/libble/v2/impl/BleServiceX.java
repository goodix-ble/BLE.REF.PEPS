package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.Nullable;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.gatt.GBGattCharacteristic;
import com.goodix.ble.libble.v2.gb.gatt.GBGattService;
import com.goodix.ble.libcomx.ILogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class BleServiceX implements GBGattService, GattChangeListener {
    private BleRemoteDevice remoteDevice;
    private UUID uuid;
    private BluetoothGattService gattService;

    boolean isDefinedByUser = false;
    boolean isMandatory = false;

    // 存储自己的特性
    @Nullable
    private ArrayList<BleCharacteristicX> characteristicList;

    BleServiceX(BleRemoteDevice remoteDevice, UUID uuid) {
        this.remoteDevice = remoteDevice;
        this.uuid = uuid;
    }

    public boolean equals(BluetoothGattService service) {
        if (service == null || this.gattService == null) return false;
        if (service == this.gattService) return true;
        if (!service.getUuid().equals(this.gattService.getUuid())) return false;
        return service.getInstanceId() == this.gattService.getInstanceId();
    }

    /**
     * 主要是为了在 {@link com.goodix.ble.libble.v2.impl.procedure.GattDiscover} 中能够判断哪些特性
     * 需要在发现服务的时候直接使能通知。
     */
    public ArrayList<BleCharacteristicX> getCharacteristicList() {
        if (characteristicList == null) {
            return new ArrayList<>(0);
        }
        return characteristicList;
    }

    public boolean isDiscovered() {
        return gattService != null;
    }

    void onDiscovered(BluetoothGattService orgService, ArrayList<String> errors) {
        // 收到 null 就表示这个对象已经被废弃了
        if (orgService == null) {
            gattService = null;
            if (characteristicList != null) {
                for (BleCharacteristicX i : characteristicList) {
                    i.onDiscovered(null, errors);
                }
                // characteristicList.clear(); 不clear，为了让用户定义的服务能够保留下来
            }
            return;
        }

        final ILogger log = remoteDevice.getLogger();

        // 首先需要UUID相同
        if (!uuid.equals(orgService.getUuid())) {
            if (log != null) {
                log.w("GBGattService", uuid + " is not match to " + orgService.getUuid());
            }
            return;
        }

//        // 服务存在，但不是同一个服务实例，丢弃
//        if (gattService != null && gattService.getInstanceId() != orgService.getInstanceId()) {
//            return false;
//        }

        gattService = orgService;

        // 更新服务的特征
        List<BluetoothGattCharacteristic> characteristics = orgService.getCharacteristics();

        if (characteristics.isEmpty()) {
            return;
        }
        if (characteristicList == null) {
            characteristicList = new ArrayList<>(characteristics.size());
        } else {
            characteristicList.ensureCapacity(characteristics.size());
        }

        // 待更新列表
        HashMap<UUID, ArrayList<BleCharacteristicX>> oldCharacteristicMap = new HashMap<>(this.characteristicList.size());

        // 移动到 MAP 中
        for (BleCharacteristicX x : characteristicList) {
            UUID uuid = x.getUuid();
            ArrayList<BleCharacteristicX> values = oldCharacteristicMap.get(uuid);
            if (values == null) {
                values = new ArrayList<>(4);
                oldCharacteristicMap.put(uuid, values);
            }
            values.add(x);
        }
        characteristicList.clear();

        for (BluetoothGattCharacteristic org : characteristics) {
            UUID uuid = org.getUuid();
            ArrayList<BleCharacteristicX> founds = oldCharacteristicMap.get(uuid);
            if (founds == null || founds.isEmpty()) {
                Log.d("onDiscovered", "  C +->  " + uuid);
                // 没有就创建一个
                BleCharacteristicX tmp = new BleCharacteristicX(remoteDevice, this, uuid);
                tmp.onDiscovered(org, errors);
                characteristicList.add(tmp);
            } else {
                Log.d("onDiscovered", "  C =->  " + uuid);
                // 对找到的特性进行初始化
                BleCharacteristicX foundChar = founds.remove(0);
                foundChar.onDiscovered(org, errors);
                characteristicList.add(foundChar);
                if (founds.isEmpty()) {
                    oldCharacteristicMap.remove(uuid);
                }
            }
        }

        for (ArrayList<BleCharacteristicX> oldChars : oldCharacteristicMap.values()) {
            for (BleCharacteristicX oldChar : oldChars) {
                if (oldChar.isMandatory) {
                    errors.add("Service " + uuid + " does not find required characteristic: " + oldChar.getUuid());
                }
                if (oldChar.isDefinedByUser) {
                    characteristicList.add(oldChar);
                }
                // 表示已经被移除
                oldChar.onDiscovered(null, errors);
            }
        }
    }

    @Override
    public GBRemoteDevice getRemoteDevice() {
        return remoteDevice;
    }

    @Override
    public int getInstanceId() {
        if (gattService != null) {
            return gattService.getInstanceId();
        }
        return 0;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public GBGattCharacteristic defineCharacteristic(UUID uuid, boolean mandatory, boolean indicate, boolean notify) {
        if (characteristicList == null) {
            characteristicList = new ArrayList<>();
        }
        BleCharacteristicX characteristic = new BleCharacteristicX(remoteDevice, this, uuid);
        characteristic.isDefinedByUser = true;
        characteristic.isMandatory = mandatory;
        characteristic.needEnableNotify = notify;
        characteristic.needEnableIndicate = indicate;
        characteristicList.add(characteristic);
        return characteristic;
    }

    @Override
    public GBGattCharacteristic requireCharacteristic(UUID uuid, boolean mandatory, boolean indicate, boolean notify) {
        GBGattCharacteristic characteristic = null;
        synchronized (this) {
            if (characteristicList != null) {
                for (GBGattCharacteristic x : characteristicList) {
                    if (x.getUuid().equals(uuid)) {
                        characteristic = x;
                        // 为了在下次发现服务的时候，能够自动使能notification
                        if (x instanceof BleCharacteristicX) {
                            BleCharacteristicX tmp = (BleCharacteristicX) x;
                            tmp.isDefinedByUser = true;
                            tmp.isMandatory = mandatory;
                            tmp.needEnableNotify = notify;
                            tmp.needEnableIndicate = indicate;
                        }
                        break;
                    }
                }
            }
        }
        if (characteristic == null) {
            characteristic = defineCharacteristic(uuid, mandatory, indicate, notify);
        }
        return characteristic;
    }

    @Override
    public List<GBGattCharacteristic> getCharacteristic(UUID uuid) {
        if (characteristicList == null || characteristicList.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GBGattCharacteristic> ret = new ArrayList<>();
        for (BleCharacteristicX x : characteristicList) {
            if (x.getUuid().equals(uuid)) {
                ret.add(x);
            }
        }
        return ret;
    }

    @Override
    public List<GBGattCharacteristic> getCharacteristics() {
        //return (List<GBGattCharacteristic>) (Object) characteristicList;
        if (characteristicList == null || characteristicList.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(characteristicList);
    }

    @Override
    public List<GBGattService> getIncludeService() {
        ArrayList<GBGattService> ret = new ArrayList<>();
        if (gattService != null) {
            List<BluetoothGattService> includedServices = gattService.getIncludedServices();
            for (BluetoothGattService service : includedServices) {
                //  当一种类型有多个实例时，会导致重复添加。
                //  这里用了一个比较暴力的方法，每次添加时判断是否已经存在来排除重复。
                for (GBGattService found : remoteDevice.getService(service.getUuid())) {
                    if (!ret.contains(found)) {
                        ret.add(found);
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public void clearEventListener(Object tag) {
        if (characteristicList != null) {
            for (BleCharacteristicX x : characteristicList) {
                x.clearEventListener(tag);
            }
        }
    }

    @Override
    public boolean undefineService() {
        if (isDefinedByUser) {
            isDefinedByUser = false;
            isMandatory = false;
            onDiscovered(null, null);
            return remoteDevice.getServiceList().remove(this);
        }
        return false;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic, int op) {
        if (characteristicList != null && gattService != null && gattService.equals(characteristic.getService())) {
            for (BleCharacteristicX x : characteristicList) {
                if (x.onCharacteristicChanged(characteristic, op)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onDescriptorChanged(BluetoothGattDescriptor descriptor, int op) {
        if (characteristicList != null && gattService != null && gattService.equals(descriptor.getCharacteristic().getService())) {
            for (BleCharacteristicX x : characteristicList) {
                if (x.onDescriptorChanged(descriptor, op)) {
                    return true;
                }
            }
        }
        return false;
    }
}
