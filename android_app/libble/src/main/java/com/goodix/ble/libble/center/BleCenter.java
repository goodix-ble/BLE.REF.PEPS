package com.goodix.ble.libble.center;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;

import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.impl.BleRemoteDevice;
import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.logger.RingLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class BleCenter {
    public static final int EVT_ADDED = 416;
    public static final int EVT_REMOVED = 598;
    public static final int EVT_SELECTED = 269;

    private static final BleCenter INSTANCE = new BleCenter();
    private static Context CONTEXT = null;
    private static ILogger rootLogger = null;

    private HashMap<String, BleItem> itemHashMap = new HashMap<>();
    private IDeviceModelCreator deviceModelCreator = null;
    private BleItem selectedDevice;

    private int maxDeviceLogCount = 0;

    private Event<BleItem> eventAdded = new Event<>(this, EVT_ADDED);
    private Event<BleItem> eventRemoved = new Event<>(this, EVT_REMOVED);
    private Event<BleItem> eventSelected = new Event<>(this, EVT_SELECTED);

    public static void setContext(Context ctx) {
        if (ctx != null) {
            CONTEXT = ctx.getApplicationContext();
        }
    }

    public static void setRootLogger(ILogger logger) {
        rootLogger = logger;
    }

    public static Context getContext() {
        return CONTEXT;
    }

    public interface IDeviceModelCreator {
        /**
         * Create a device model which holds all needed data.
         * One device can bind only one model during the lifetime.
         * <p>
         * This method will be called in synchronized block. DO NOT block thread.
         *
         * @param device the device added just now
         * @return null is allowed.
         */
        Object onCreateDeviceModel(BleItem device);
    }

    public static BleCenter get() {
        return INSTANCE;
    }

    public Event<BleItem> evtAdded() {
        return eventAdded;
    }

    public Event<BleItem> evtRemoved() {
        return eventRemoved;
    }

    public Event<BleItem> evtSelected() {
        return eventSelected;
    }

    public void setDeviceModelCreator(IDeviceModelCreator creator) {
        this.deviceModelCreator = creator;
    }

    public void setMaxDeviceLogCount(int maxDeviceLogCount) {
        this.maxDeviceLogCount = maxDeviceLogCount;
    }

    public List<BleItem> getDevices(List<BleItem> out) {
        if (out == null) {
            out = new ArrayList<>(itemHashMap.size());
        }
        synchronized (this) {
            out.addAll(itemHashMap.values());
        }
        return out;
    }

    public BleItem getDevice(BluetoothDevice dev) {
        if (dev == null) {
            return null;
        }
        synchronized (this) {
            return itemHashMap.get(dev.getAddress());
        }
    }

    public BleItem getDevice(String address) {
        if (address == null) {
            return null;
        }
        synchronized (this) {
            return itemHashMap.get(address);
        }
    }

    public BleItem addDevice(String address) {
        return addDevice(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address));
    }

    @NonNull
    public BleItem addDevice(BluetoothDevice dev) {
        Context ctx = CONTEXT;
        if (ctx == null) {
            throw new IllegalStateException("Please call setContext() before calling addDevice().");
        }
        if (dev == null) {
            throw new IllegalArgumentException("BluetoothDevice is null.");
        }
        BleItem existItem;
        boolean added = false;
        synchronized (this) {
            existItem = itemHashMap.get(dev.getAddress());
            if (existItem == null) {
                existItem = new BleItem();
                itemHashMap.put(dev.getAddress(), existItem);
                BleRemoteDevice gatt = new BleRemoteDevice(ctx);
                gatt.setBluetoothDevice(dev);
                existItem.setGatt(gatt);
                if (this.maxDeviceLogCount > 0) {
                    existItem.logger = new RingLogger(this.maxDeviceLogCount);
                    existItem.logger.setLogger(rootLogger);
                    gatt.setLogger(existItem.logger);
                } else {
                    if (rootLogger instanceof RingLogger) {
                        existItem.logger = (RingLogger) rootLogger;
                        gatt.setLogger(existItem.logger);
                    }
                }

                added = true;
                if (deviceModelCreator != null) {
                    existItem.setDeviceModel(deviceModelCreator.onCreateDeviceModel(existItem));
                }
            }
        }
        if (added) {
            eventAdded.postEvent(existItem);
        }
        return existItem;
    }

    public BleItem wrapDevice(GBRemoteDevice gatt) {
        Context ctx = CONTEXT;
        if (ctx == null) {
            throw new IllegalStateException("Please call setContext() before calling addDevice().");
        }
        if (gatt == null) {
            throw new IllegalArgumentException("GBRemoteDevice is null.");
        }
        BleItem existItem;
        boolean added = false;
        String address = gatt.getAddress();
        ILogger gattLogger = gatt.getLogger();
        synchronized (this) {
            existItem = itemHashMap.get(address);
            if (existItem == null) {
                existItem = new BleItem();
                itemHashMap.put(address, existItem);
                existItem.setGatt(gatt);
                if (this.maxDeviceLogCount > 0) {
                    existItem.logger = new RingLogger(this.maxDeviceLogCount);
                    if (gattLogger != null) {
                        existItem.logger.setLogger(gattLogger);
                    } else {
                        existItem.logger.setLogger(rootLogger);
                        gatt.setLogger(existItem.logger);
                    }
                } else {
                    if (rootLogger instanceof RingLogger) {
                        existItem.logger = (RingLogger) rootLogger;
                        if (gattLogger == null) {
                            gatt.setLogger(existItem.logger);
                        }
                    }
                }

                added = true;
                if (deviceModelCreator != null) {
                    existItem.setDeviceModel(deviceModelCreator.onCreateDeviceModel(existItem));
                }
            }
        }
        if (added) {
            eventAdded.postEvent(existItem);
        }
        return existItem;
    }

    public BleItem remove(String address) {
        BleItem device = getDevice(address);
        remove(device);
        return device;
    }

    public void remove(BleItem device) {
        if (device == null) {
            return;
        }
        boolean removed = false;
        GBRemoteDevice gatt = device.getGatt();
        synchronized (this) {
            if (itemHashMap.remove(gatt.getAddress()) != null) {
                removed = true;
            }
        }
        if (removed) {
            // disconnect for releasing resource
            if (!gatt.isDisconnected()) {
                gatt.disconnect(false).startProcedure();
            } else {
                ((BleRemoteDevice) gatt).dispose();
            }
            eventRemoved.postEvent(device);

            if (this.selectedDevice == device) {
                setSelectedDevice(null);
            }
        }
    }

    public BleItem getSelectedDevice() {
        return selectedDevice;
    }

    public <M> M getSelectedDeviceModel() {
        BleItem selectedDevice = this.selectedDevice;
        if (selectedDevice != null) {
            return selectedDevice.getDeviceModel();
        }
        return null;
    }

    public void setSelectedDevice(BleItem selectedDevice) {
        if (this.selectedDevice != selectedDevice) {
            boolean selected = false;
            synchronized (this) {
                if (this.selectedDevice != selectedDevice) {
                    this.selectedDevice = selectedDevice;
                    selected = true;
                }
            }
            if (selected) {
                eventSelected.postEvent(selectedDevice);
            }
        }
    }
}
