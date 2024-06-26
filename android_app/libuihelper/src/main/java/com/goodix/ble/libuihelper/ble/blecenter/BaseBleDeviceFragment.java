package com.goodix.ble.libuihelper.ble.blecenter;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.goodix.ble.libble.center.BleCenter;
import com.goodix.ble.libble.center.BleItem;
import com.goodix.ble.libcomx.event.EventDisposer;
import com.goodix.ble.libuihelper.fragment.ClosableTabFragment;

public class BaseBleDeviceFragment extends ClosableTabFragment {
    @Nullable
    protected String targetAddress = null;

    @Nullable
    protected BleItem targetDevice;

    protected final EventDisposer deviceEventDisposer = new EventDisposer(); // disposed when device changed
    protected final EventDisposer uiEventDisposer = new EventDisposer(); // disposed when UI destroyed

    public void onDeviceChanged(BleItem device) {
    }

    public String getTargetAddress() {
        if (targetAddress == null && getArguments() != null) {
            targetAddress = getArguments().getString(BluetoothDevice.EXTRA_DEVICE);
        }
        return targetAddress;
    }

    public BaseBleDeviceFragment setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;

        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            if (getFragmentManager() == null || !isStateSaved()) {
                setArguments(arguments);
            }
        }
        arguments.putString(BluetoothDevice.EXTRA_DEVICE, targetAddress);
        return this;
    }

    public final BaseBleDeviceFragment setTargetDevice(BleItem newDevice) {
        // 解除前一个设备的绑定，断开设备
        if (targetDevice != null) {
            // 选择了相同的设备，跳过
            if (newDevice != null && targetDevice.getGatt().getAddress().equals(newDevice.getGatt().getAddress())) {
                return this;
            }
            // 移除引用
            targetDevice.release();
            targetDevice = null;
        }
        deviceEventDisposer.disposeAll(this);

        this.targetDevice = newDevice;

        if (newDevice != null) {
            setTargetAddress(newDevice.getGatt().getAddress());
            // 保留引用
            newDevice.retain();
        } else {
            setTargetAddress(null);
        }

        onDeviceChanged(newDevice);
        return this;
    }

    @Override
    public void onStart() {
        super.onStart(); // 显示对话框，或者显示UI

        // 如果还没有选定设备
        if (this.targetDevice == null) {
            BleCenter.setContext(getContext());
            // 获取选定的设备，并刷新UI
            BleItem targetDevice;
            if (getTargetAddress() != null) {
                targetDevice = (BleCenter.get().addDevice(targetAddress));
            } else {
                targetDevice = BleCenter.get().getSelectedDevice();
            }
            if (targetDevice != null) {
                setTargetDevice(targetDevice);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiEventDisposer.disposeAll(this);
        setTargetDevice(null);
    }
}
