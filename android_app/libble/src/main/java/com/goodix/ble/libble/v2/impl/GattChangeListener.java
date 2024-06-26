package com.goodix.ble.libble.v2.impl;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

interface GattChangeListener {
    int OP_READ = 1;
    int OP_WRITTEN = 2;
    int OP_NOTIFY = 3;
    int OP_INDICATE = 4;

    /**
     * Dispatch event about characteristic changes.
     *
     * @param characteristic target characteristic
     * @param op             operation on target characteristic:
     *                       {@link #OP_READ} or {@link #OP_WRITTEN}
     *                       {@link #OP_NOTIFY} or {@link #OP_INDICATE}
     * @return true if handled.
     */
    boolean onCharacteristicChanged(BluetoothGattCharacteristic characteristic, int op);

    /**
     * Dispatch event about descriptor changes.
     *
     * @param descriptor target descriptor
     * @param op         operation on target descriptor:
     *                   {@link #OP_READ} or {@link #OP_WRITTEN}
     * @return true if handled.
     */
    boolean onDescriptorChanged(BluetoothGattDescriptor descriptor, int op);
}
