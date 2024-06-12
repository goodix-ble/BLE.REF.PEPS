package com.goodix.ble.gr.toolbox.common.blecenter;

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

    protected EventDisposer deviceEventDisposer = new EventDisposer();
    protected EventDisposer uiEventDisposer = new EventDisposer();

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

    public final BaseBleDeviceFragment setTargetDevice(BleItem targetDevice) {
        if (this.targetDevice != null && this.targetDevice == targetDevice) {
            return this;
        }

        // 移除先前设备的事件绑定
        deviceEventDisposer.disposeAll(this);
        BleItem prvDev = this.targetDevice;
        if (prvDev != null && !prvDev.getGatt().isDisconnected()) {
            prvDev.getGatt().disconnect(true).startProcedure();
        }

        this.targetDevice = targetDevice;

        if (targetDevice != null) {
            setTargetAddress(targetDevice.getGatt().getAddress());
        } else {
            setTargetAddress(null);
        }

        onDeviceChanged(targetDevice);
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getTargetAddress() != null) {
            targetDevice = (BleCenter.get().getDevice(targetAddress));
        }

        if (targetDevice == null) {
            targetDevice = BleCenter.get().getSelectedDevice();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setTargetDevice(null);
    }
}
