package com.goodix.ble.gr.toolbox.common.blecenter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.libble.center.BleCenter;
import com.goodix.ble.libble.center.BleItem;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libuihelper.ble.scanner.BleScannerFragment;
import com.goodix.ble.libuihelper.ble.scanner.IDeviceItem;
import com.goodix.ble.libuihelper.dialog.EasySelectDialog;
import com.goodix.ble.libuihelper.fragment.ClosableTabFragment;
import com.goodix.ble.libuihelper.sublayout.list.MvcAdapter;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BleCenterFragment extends ClosableTabFragment implements IEventListener, View.OnClickListener, BleScannerFragment.CB, Comparator<BleItem> {
    private static final String ARG_SHOW_RADIO = "showRadio";
    private static final String ARG_SHOW_CONNECT = "showConnect";
    private static final String ARG_SHOW_CONNECTED = "showConnected";
    private MvcAdapter adapter = new MvcAdapter(new BleCenterCtrl(this));
    private Button scanBtn;
    private Button addConnectedDeviceBtn;
    IMenuCB menuCB = null;

    boolean showRadio = true;
    boolean showConnect = true;
    boolean showConnected = true;

    public interface IMenuCB {
        void onCreateMenu(BleCenterFragment fragment, BleItem device, MenuInflater inflater, Menu menu);

        void onMenuClicked(BleCenterFragment fragment, BleItem device, MenuItem menuItem);
    }

    public void removeDevice(BleItem device) {
        final Context context = getContext();
        if (context != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(device.getGatt().getAddress())
                    .setMessage(R.string.common_confirm_delete)
                    .setPositiveButton(android.R.string.ok, ((dialog, which) -> BleCenter.get().remove(device)))
                    .setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    public BleCenterFragment setConfig(boolean showRadio, boolean showConnect, boolean showConnected) {
        Bundle arguments = obtainArguments();
        arguments.putBoolean(ARG_SHOW_RADIO, this.showRadio = showRadio);
        arguments.putBoolean(ARG_SHOW_CONNECT, this.showConnect = showConnect);
        arguments.putBoolean(ARG_SHOW_CONNECTED, this.showConnected = showConnected);
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取创建菜单用的回调函数
        FragmentActivity hostAct = getActivity();
        if (hostAct instanceof IMenuCB) {
            menuCB = (IMenuCB) hostAct;
        } else {
            Fragment parent = getParentFragment();
            while (parent != null) {
                if (parent instanceof IMenuCB) {
                    menuCB = (IMenuCB) parent;
                } else {
                    parent = parent.getParentFragment();
                }
            }
        }
        // 获取配置参数
        Bundle bundle = obtainArguments();
        this.showRadio = bundle.getBoolean(ARG_SHOW_RADIO, this.showRadio);
        this.showConnect = bundle.getBoolean(ARG_SHOW_CONNECT, this.showConnect);
        this.showConnected = bundle.getBoolean(ARG_SHOW_CONNECTED, this.showConnected);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.common_fragment_ble_center, container, false);

        final RecyclerView dutRv = root.findViewById(R.id.common_fragment_ble_center_rv);
        scanBtn = root.findViewById(R.id.common_fragment_ble_center_scan_btn);
        addConnectedDeviceBtn = root.findViewById(R.id.common_fragment_ble_center_add_connected_device_btn);

        dutRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        dutRv.setAdapter(adapter);

        final List<BleItem> dutList = BleCenter.get().getDevices(null);
        adapter.update(dutList);

        scanBtn.setOnClickListener(this);
        addConnectedDeviceBtn.setOnClickListener(this);

        addConnectedDeviceBtn.setVisibility(showConnected ? View.VISIBLE : View.GONE);

        BleCenter.get().evtAdded().subEvent(this)
                .setExecutor(UiExecutor.getDefault())
                .register(this);

        BleCenter.get().evtRemoved().subEvent(this)
                .setExecutor(UiExecutor.getDefault())
                .register(this);

        if (getTabTitle() == null) {
            setTabTitle("DUT列表");
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        BleCenter.get().evtAdded().clear(this);
        BleCenter.get().evtRemoved().clear(this);
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (evtType == BleCenter.EVT_ADDED || evtType == BleCenter.EVT_REMOVED) {
            final List<BleItem> dutList = BleCenter.get().getDevices(null);
            Collections.sort(dutList, this);
            adapter.update(dutList);
        }
    }

    @Override
    public int compare(BleItem o1, BleItem o2) {
        int diff = o1.getGatt().getName().compareToIgnoreCase(o2.getGatt().getName());
        if (diff == 0) {
            diff = o1.getGatt().getAddress().compareToIgnoreCase(o2.getGatt().getAddress());
        }
        return diff;
    }

    @Override
    public void onClick(View v) {
        if (v == scanBtn) {
            new BleScannerFragment()
                    .show(getChildFragmentManager(), "Scanner");
        } else if (v == addConnectedDeviceBtn) {
            selectConnectedDevice();
        }
    }

    @Override
    public void onDeviceSelected(@Nullable IDeviceItem device) {
        if (device != null) {
            final BleItem dut = BleCenter.get().addDevice(device.getDevice());
            if (BleCenter.get().getSelectedDevice() == null) {
                BleCenter.get().setSelectedDevice(dut);
            }
        }
    }

    private void selectConnectedDevice() {
        // 验证能否获得已连接的设备
        List<BluetoothDevice> connectedDevices = null;
        final Context context = requireContext();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        }

        new EasySelectDialog<BluetoothDevice>(context)
                .setItemList(connectedDevices)
                .setConverter((pos, item, caption, desc) -> {
                    caption.append(item.getName());
                    desc.append(item.getAddress());
                })
                .setListener((pos, item) -> {
                    final BleItem dut = BleCenter.get().addDevice(item);
                    if (dut != null && BleCenter.get().getSelectedDevice() == null) {
                        BleCenter.get().setSelectedDevice(dut);
                    }
                })
                .show();
    }

    private Bundle obtainArguments() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            setArguments(arguments);
        }
        return arguments;
    }
}
