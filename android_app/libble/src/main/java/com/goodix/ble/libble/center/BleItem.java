package com.goodix.ble.libble.center;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.gatt.profile.GBGattProfile;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.logger.RingLogger;

import java.util.HashMap;

public class BleItem {
    protected HashMap<String, Object> moduleMap = null;
    protected GBRemoteDevice gatt; // 具体的连接对象
    protected int refCounter = 0;
    protected RingLogger logger;
    private Object deviceModel;

    public GBRemoteDevice getGatt() {
        return gatt;
    }

    public RingLogger getLogger() {
        return logger;
    }

    public void setLogger(RingLogger logger) {
        this.logger = logger;
        if (this.gatt != null) {
            this.gatt.setLogger(logger);
        }
    }

    public <T> T getDeviceModel() {
        try {
            //noinspection unchecked
            return (T) deviceModel;
        } catch (Exception e) {
            e.printStackTrace();
            final ILogger log = logger;
            if (log != null) {
                log.w(getClass().getSimpleName(), e.getMessage());
            }
        }
        return null;
    }

    public <T> T getModule(String name) {
        HashMap<String, Object> modules = this.moduleMap;
        if (modules != null) {
            synchronized (this) {
                try {
                    //noinspection unchecked
                    return (T) modules.get(name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public <T> T getModule(Class<T> type) {
        return getModule(type.getName());
    }

    public BleItem setModule(String name, Object val) {
        synchronized (this) {
            if (this.moduleMap == null) {
                this.moduleMap = new HashMap<>();
            }
            this.moduleMap.put(name, val);
        }
        return this;
    }

    public BleItem setModule(Object val) {
        return setModule(val.getClass().getName(), val);
    }

    public <T extends GBGattProfile> T requireProfile(Class<T> type) {
        T module = getModule(type.getName());
        if (module == null) {
            try {
                module = type.newInstance();
                setModule(module);
                module.bindTo(gatt);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
        return module;
    }

    public synchronized void retain() {
        refCounter++;
    }

    public synchronized void release() {
        if (refCounter > 0) {
            refCounter--;
            if (refCounter == 0) {
                BleCenter.get().remove(this);
            }
        }
    }

    void setGatt(GBRemoteDevice gatt) {
        this.gatt = gatt;
    }

    void setDeviceModel(Object model) {
        this.deviceModel = model;
    }
}
