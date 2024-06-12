package com.goodix.ble.gr.toolbox.common.test;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.util.ToastUtil;
import com.goodix.ble.libble.center.BleCenter;
import com.goodix.ble.libble.center.BleItem;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libuihelper.ble.blecenter.BaseBleDeviceFragment;
import com.goodix.ble.libuihelper.ble.scanner.BleScannerFragment;
import com.goodix.ble.libuihelper.ble.scanner.ScannedDeviceItem;
import com.goodix.ble.libuihelper.sublayout.GenericSelectHolder;
import com.goodix.ble.libuihelper.test.stress.StressTestHolder;
import com.goodix.ble.libuihelper.test.stress.StressTestTask;
import com.goodix.ble.libuihelper.thread.UiExecutor;

@SuppressLint("SetTextI18n")
public abstract class StressTestFragment extends BaseBleDeviceFragment implements StressTestHolder.CB, BleScannerFragment.CB {

    protected StressTestHolder holder = new StressTestHolder(this);
    protected GenericSelectHolder deviceSelectHolder = null; // call addDutSelector() to init
    protected BleScannerFragment bleScannerFragment = null;

    protected abstract StressTestTask onObtainTestTask(@NonNull BleItem targetDevice);

    protected void onCreateView(LayoutInflater inflater, LinearLayout configLL, LinearLayout statusLL, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onConfigBeforeStart(StressTestTask testTask) {
        // empty
    }

    @Override
    public void onUpdateStatus(StressTestTask testTask) {
        // empty
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = holder.createView(inflater, container, savedInstanceState);

        onCreateView(inflater, holder.getConfigLL(), holder.getStatusLL(), savedInstanceState);

        if (targetDevice != null) {
            BleItem device = targetDevice;
            targetDevice = null;
            setTargetDevice(device);
            holder.getLogger().v(getClass().getSimpleName(), "待测设备：" + device.getGatt().getAddress());
        } else {
            setTabDesc(null);
        }

        return root;
    }

    @Override
    public void onDeviceChanged(BleItem device) {
        if (device != null) {
            if (deviceSelectHolder != null && deviceSelectHolder.actionBtn.getVisibility() == View.VISIBLE) {
                // 再监听当前设备的连接状态
                device.getGatt().evtStateChanged()
                        .subEvent().setDisposer(deviceEventDisposer).setExecutor(UiExecutor.getDefault())
                        .register((src, evtType, evtData) -> updateConnectionStateUI(device.getGatt().getState()));
                updateConnectionStateUI(device.getGatt().getState());
            }
            setTabDesc(device.getGatt().getAddress());

            StressTestTask stressTestTask = onObtainTestTask(device);
            holder.getLogger().i("+++", "onObtainTestTask(): " + stressTestTask);
            if (stressTestTask != null) {
                holder.setTest(stressTestTask);
                if (tabTitle == null || tabTitle.isEmpty()) {
                    setTabTitle(stressTestTask.getName());
                }
                stressTestTask.setParameter(BleItem.class, device);
            }
            if (deviceSelectHolder != null) {
                deviceSelectHolder.setNameValue(device.getGatt().getName(), device.getGatt().getAddress());
            }
        } else {
            setTabDesc(null);
            if (deviceSelectHolder != null) {
                deviceSelectHolder.setNameValue(getString(R.string.libuihelper_na), getString(R.string.libuihelper_na));
            }
        }
    }

    @Override
    public void onDeviceSelected(@Nullable ScannedDeviceItem device) {
        if (device != null) {
            setTargetDevice(BleCenter.get().addDevice(device.report.device));
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void addDutSelector(boolean showConnectBtn) {
        deviceSelectHolder = new GenericSelectHolder()
                .attachView(getLayoutInflater().inflate(R.layout.libuihelper_sublayout_generic_selector, holder.getConfigLL(), false))
                .setCaption("选择设备");
        if (showConnectBtn) {
            deviceSelectHolder.actionBtn.setText(R.string.common_connect);
            deviceSelectHolder.actionBtn.setVisibility(View.VISIBLE);
            deviceSelectHolder.actionBtn.setOnClickListener(v -> {
                if (targetDevice != null) {
                    if (targetDevice.getGatt().isDisconnected()) {
                        targetDevice.getGatt().connect(0).startProcedure();
                        targetDevice.getGatt().discoverServices().startProcedure();
                    } else {
                        targetDevice.getGatt().disconnect(true).startProcedure();
                    }
                } else {
                    ToastUtil.info(v.getContext(), "请先选择设备。").show();
                }
            });
        } else {
            deviceSelectHolder.actionBtn.setVisibility(View.GONE);
        }
        deviceSelectHolder.selectBtn.setOnClickListener(v -> showScanner(bleScannerFragment));

        holder.getConfigLL().addView(deviceSelectHolder.root);
    }

    public void showScanner(BleScannerFragment bleScannerFragment) {
        if (bleScannerFragment == null) {
            bleScannerFragment = new BleScannerFragment();
            bleScannerFragment.setCfg(new BleScannerFragment.Cfg().setRssiFilter(-70));
            this.bleScannerFragment = bleScannerFragment;
        }
        bleScannerFragment.show(getChildFragmentManager(), "selectDevice");
    }

    private void updateConnectionStateUI(int state) {
        TextView connStateTv = deviceSelectHolder.actionBtn;
        if (connStateTv == null) {
            return;
        }

        switch (state) {
            case GBRemoteDevice.STATE_CONNECTING:
                connStateTv.setText("连接中");
                break;
            case GBRemoteDevice.STATE_CONNECTED:
                connStateTv.setText("已连接");
                break;
            case GBRemoteDevice.STATE_DISCONNECTING:
                connStateTv.setText("断开连接中");
                break;
            case GBRemoteDevice.STATE_DISCONNECTED:
            default:
                connStateTv.setText("设备未连接");
        }
    }
}
