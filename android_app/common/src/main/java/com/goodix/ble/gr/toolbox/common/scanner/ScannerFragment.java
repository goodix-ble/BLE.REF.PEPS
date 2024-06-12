package com.goodix.ble.gr.toolbox.common.scanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.ui.ClickDebounceHelper;
import com.goodix.ble.gr.toolbox.common.ui.WaveView;
import com.goodix.ble.gr.toolbox.common.util.AppUtils;
import com.goodix.ble.gr.toolbox.common.util.BLERequest;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libuihelper.ble.scanner.EasyLeScanner;
import com.goodix.ble.libuihelper.ble.scanner.ILeScannerFilter;
import com.goodix.ble.libuihelper.ble.scanner.LeScannerFilter;
import com.goodix.ble.libuihelper.ble.scanner.LeScannerReport;
import com.goodix.ble.libuihelper.ble.scanner.ScannedDeviceItem;
import com.goodix.ble.libuihelper.thread.UiExecutor;
import com.goodix.ble.libuihelper.view.AsyncBatchLayoutInflater;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ScannerFragment extends DialogFragment implements IEventListener, AsyncBatchLayoutInflater.CB, ILeScannerFilter {

    public interface OnDeviceSelectedListener {
        /**
         * Fired when user selected the device.
         *
         * @param device the device to devcie_fragment to
         * @param name   the device name. Unfortunately on some devices {@link BluetoothDevice#getName()} always returns <code>null</code>, f.e. Sony Xperia Z1 (C6903) with Android 4.3. The name has to
         */
        void onDeviceSelected(final ExtendedBluetoothDevice device, final String name, byte[] scanByte);

        /**
         * Fired when scanner dialog has been cancelled without selecting a device.
         */
        void onDialogCanceled();
    }


    private ScannerAdapter adapter;
    private OnDeviceSelectedListener mListener;

    private TextView titleTv;
    private String titleStr = null;

    private FloatingActionButton mScanButton;
    private WaveView wave;

    private ArrayList<LeScannerFilter> filters;

    public ScannerFragment setTitle(String title) {
        this.titleStr = title;
        if (titleTv != null) {
            titleTv.setText(titleStr);
        }
        return this;
    }

    public static ScannerFragment getInstance(final UUID uuid, boolean findBeaconFlag, String filterName) {
        Builder builder = new Builder();
        if (!findBeaconFlag) {
            if (uuid != null)
                builder.addUUID(uuid);
        }
        if (filterName != null) {
            builder.addDeviceName(filterName);
        }
        return builder.build();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        this.mListener = null;
        if (context instanceof OnDeviceSelectedListener) {
            this.mListener = (OnDeviceSelectedListener) context;
        } else {
            Fragment parent = getParentFragment();
            if (parent instanceof OnDeviceSelectedListener) {
                this.mListener = (OnDeviceSelectedListener) parent;
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Builder.parseArgument(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(inflater.getContext());
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        new AsyncBatchLayoutInflater(inflater, root, 9)
                .add(R.layout.fragment_scanner)
                .add(R.layout.item_list_scanner_title, 2)
                .add(R.layout.item_list_scanner_device, 8) // 预计会显示8个扫描结果
                .start(this);

        return root;
    }

    @Override
    public void onLayoutInflaterReady(LayoutInflater inflater, ViewGroup container) {
        final View root = inflater.inflate(R.layout.fragment_scanner, container);
        wave = root.findViewById(R.id.wave);

        if (adapter != null) {
            adapter.easyLeScanner.evtScan().clear(this);
        }
        adapter = new ScannerAdapter(inflater);
        adapter.eventSelected.register(this);
        adapter.easyLeScanner.evtScan().setExecutor(UiExecutor.getDefault()).register(this);
        adapter.easyLeScanner.evtError().setExecutor(UiExecutor.getDefault()).register(this);

        final RecyclerView resultRv = root.findViewById(android.R.id.list);
        resultRv.setLayoutManager(new LinearLayoutManager(resultRv.getContext()));
        resultRv.getRecycledViewPool().setMaxRecycledViews(0, 16); // 多复用，少创建
        resultRv.setAdapter(adapter);


        titleTv = root.findViewById(R.id.title_tv);

        mScanButton = root.findViewById(R.id.action_cancel);
        mScanButton.setOnClickListener(new ClickDebounceHelper(v -> {
            if (adapter.easyLeScanner.isScanning()) {
                stopScan();
            } else {
                startScan();
            }
        }).setInterval(500));

        // wave.start(); 如果没有得到权限，就不应该播放
        startScan();

        final ImageView cancelIv = root.findViewById(R.id.cancel_iv);
        final ImageView sortIv = root.findViewById(R.id.sort_iv);
        cancelIv.setOnClickListener(view -> {
            Dialog dialog = getDialog();
            if (dialog != null) {
                dialog.cancel();
            }
        });

        // 自带200ms的消抖
        sortIv.setOnClickListener(view -> adapter.sortByRssi());
    }

    @Override
    public void onDestroyView() {
        stopScan();
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final Dialog dialog = new Dialog(requireActivity(), R.style.style_scan_dialog);

        Window window = dialog.getWindow();
        Objects.requireNonNull(window).setGravity(Gravity.BOTTOM); //可设置dialog的位置
        window.getDecorView().setPadding(0, 0, 0, 0); //消除边距

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;   //设置宽度充满屏幕
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);

        return dialog;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

        mListener.onDialogCanceled();
    }

    private void startScan() {
        // 内部需要判断是否已经开始扫描，否则，会因为扫描已经在进行中又开始了一次新的扫描而崩溃
        // 也解决 Android10 一定需要申请一次权限，导致不断调用startScan()
        if (adapter == null || adapter.easyLeScanner.isScanning()) {
            return;
        }

        if (BLERequest.isBLEEnabled(requireContext())) {
            if (!BLERequest.checkBlePermission(this)) {
                return;
            }
        } else {
            BLERequest.showBLEDialog(this);
            return;
        }

        adapter.clear();

        adapter.easyLeScanner.setFilter(this);
        adapter.startScan();
    }

    /**
     * Stop scan if user tap Cancel button
     */
    private void stopScan() {
        if (adapter != null && adapter.easyLeScanner.isScanning()) {
            adapter.stopScan();
        }
    }

    private void setFloatingActionButtonColors(FloatingActionButton fab, int primaryColor) {
        int[][] states = {
                {android.R.attr.state_enabled},
                {android.R.attr.state_pressed},
        };

        int[] colors = {
                primaryColor,
                ContextCompat.getColor(requireContext(), R.color.colorWindowBack),
        };

        ColorStateList colorStateList = new ColorStateList(states, colors);
        fab.setBackgroundTintList(colorStateList);
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src == adapter && evtType == ScannerAdapter.EVT_SELECTED) {
            ScannedDeviceItem scannedDeviceItem = (ScannedDeviceItem) evtData;
            stopScan();
            dismiss();
            final ExtendedBluetoothDevice d = new ExtendedBluetoothDevice(scannedDeviceItem.report.device);
            mListener.onDeviceSelected(d, scannedDeviceItem.name, scannedDeviceItem.report.getPayload());
        }
        if (src == adapter.easyLeScanner) {
            if (evtType == EasyLeScanner.EVT_SCAN) {
                boolean scanning = (boolean) evtData;
                if (scanning) {
                    setFloatingActionButtonColors(mScanButton, AppUtils.getThemeAccentColor(requireContext()));
                    wave.start();
                    if (titleStr != null) {
                        titleTv.setText(titleStr);
                    } else {
                        titleTv.setText(R.string.scanner_scan_device);
                    }
                } else {
                    titleTv.setText(R.string.scanner_select_device);
                    //setFloatingActionButtonColors(mScanButton, getResources().getColor(R.color.colorWindowBack));
                    wave.clearWave();
                    wave.stop();
                }
            } else if (evtType == EasyLeScanner.EVT_ERROR) {
                Context ctx = getContext();
                if (ctx != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setTitle(R.string.libuihelper_error)
                            .setMessage(ctx.getString(R.string.libuihelper_failed_to_scan, (String) evtData))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        }
    }

    @Override
    public boolean match(LeScannerReport report) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (LeScannerFilter filter : filters) {
            if (filter.match(report)) {
                return true;
            }
        }
        return false;
    }


    @SuppressWarnings({"WeakerAccess", "unused"})
    public static class Builder {
        private final static String PARAM_FILTER = "scanner_filter";

        ArrayList<LeScannerFilter> filters;
        Bundle arg = new Bundle();

        public ScannerFragment build() {
            ScannerFragment fragment = new ScannerFragment();
            if (filters != null) {
                arg.putParcelableArrayList(PARAM_FILTER, filters);
            }
            fragment.setArguments(arg);
            return fragment;
        }

        private static void parseArgument(ScannerFragment fragment) {
            final Bundle args = fragment.getArguments();
            if (args != null) {
                fragment.filters = args.getParcelableArrayList(PARAM_FILTER);
            }
        }

        public Builder addUUID(UUID uuid) {
            if (uuid == null) return this;
            LeScannerFilter filter = new LeScannerFilter();
            filter.checkServiceUuid = true;
            filter.serviceUuids.add(uuid);
            return addFilter(filter);
        }

        public Builder addDeviceName(String name) {
            if (name == null) return this;
            LeScannerFilter filter = new LeScannerFilter();
            filter.checkLocalName = true;
            filter.localName = name;
            return addFilter(filter);
        }

        public Builder addDeviceAddr(String addr) {
            if (addr == null) return this;
            LeScannerFilter filter = new LeScannerFilter();
            filter.checkAddress = true;
            filter.address = addr;
            return addFilter(filter);
        }

        public Builder addFilter(LeScannerFilter filter) {
            if (filters == null) {
                filters = new ArrayList<>();
            }
            filters.add(filter);
            return this;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLERequest.REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_CANCELED) {
                startScan();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLERequest.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            }
        }
    }

    static class ScannerAdapter extends RecyclerView.Adapter<VVV> implements Comparator<ScannedDeviceItem>, IEventListener, Runnable {
        final EasyLeScanner easyLeScanner = new EasyLeScanner();
        final Handler mHandler = new Handler(Looper.getMainLooper());
        final LayoutInflater inflater;

        // 用于减少刷新界面的次数
        private final int notifyChangedInterval = 2_00;
        private boolean pendingNotifyChanged = false;
        private long stopTime = 0; // 用于判断是否为超时停止
        private boolean pendingSort = false;

        // 用于减少更新Item的次数
        int lastReportCount = 0; // 用于判断列表长度是否发生变化
        long lastUpdateRvTime = 0; // 记录最后一次更新列表的时间，如果列表中的项的更新时间大于这个时间，就表示那个项需要更新

        private final BluetoothManager btManager;

        private final ArrayList<ScannedDeviceItem> mReports = new ArrayList<>(128);
        private final HashMap<String, ScannedDeviceItem> mReportsMap = new HashMap<>(128);

        private final ArrayList<ScannedDeviceItem> mBondDevice = new ArrayList<>(128);
        private final ArrayList<ScannedDeviceItem> mConnectedDevice = new ArrayList<>(128);
        private final MultiListHelper multiListHelper = new MultiListHelper();

        private static final int EVT_SELECTED = 157;
        private final Event<ScannedDeviceItem> eventSelected = new Event<>(this, EVT_SELECTED);

        public ScannerAdapter(LayoutInflater inflater) {
            easyLeScanner.evtFound().register(this);
            this.inflater = inflater;
            btManager = (BluetoothManager) inflater.getContext().getSystemService(Context.BLUETOOTH_SERVICE);

            ArrayList<Integer> mBondDeviceTitle = new ArrayList<>(1);
            mBondDeviceTitle.add(R.string.scanner_subtitle_bonded);

            ArrayList<Integer> mConnectedDeviceTitle = new ArrayList<>(1);
            mConnectedDeviceTitle.add(R.string.scanner_subtitle_connected);

            ArrayList<Integer> mReportsTitle = new ArrayList<>(1);
            mReportsTitle.add(R.string.scanner_subtitle_not_bonded);

            multiListHelper.clear();
            multiListHelper.add(mBondDeviceTitle, 1);
            multiListHelper.add(mBondDevice, 0);
            multiListHelper.add(mConnectedDeviceTitle, 2);
            multiListHelper.add(mConnectedDevice, 0);
            multiListHelper.add(mReportsTitle, 3);
            multiListHelper.add(mReports, 0);
        }

        public void clear() {
            mReports.clear();
            mReportsMap.clear();
            mBondDevice.clear();
            mConnectedDevice.clear();

            multiListHelper.getList(0).enable = false;
            multiListHelper.getList(1).enable = false;
            multiListHelper.getList(2).enable = false;
            multiListHelper.getList(3).enable = false;

            notifyDataSetChanged();
        }

        private void startScan() {
            if (easyLeScanner.isScanning()) {
                return;
            }

            stopTime = System.currentTimeMillis() + 10_000;
            mHandler.removeCallbacks(this);
            pendingNotifyChanged = false; // 防止handler被移除了，但pendingNotifyChanged还为true的情况
            mHandler.postDelayed(this, 10_000); // 默认10秒

            handleFixedDevice();

            lastReportCount = 0;
            lastUpdateRvTime = 0;

            easyLeScanner.start();
        }


        public void stopScan() {
            if (!easyLeScanner.isScanning()) {
                return;
            }
            easyLeScanner.stop();
            mHandler.removeCallbacks(this);
            pendingNotifyChanged = false;
        }

        public void sortByRssi() {
            pendingSort = true;
            if (!pendingNotifyChanged) {
                pendingNotifyChanged = true;
                mHandler.post(this);
            }
        }

        private void handleFixedDevice() {
            Set<BluetoothDevice> bondDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (BluetoothDevice device : bondDevices) {
                LeScannerReport report = new LeScannerReport(device.getAddress());
                report.device = device;
                report.rssi = 0;
                ScannedDeviceItem deviceItem = new ScannedDeviceItem();
                deviceItem.report = report;
                deviceItem.name = report.device.getName();
                deviceItem.isBonded = true;
                mReportsMap.put(report.address, deviceItem);
                mBondDevice.add(deviceItem);
            }

            if (btManager != null) {
                List<BluetoothDevice> connectedDevices = btManager.getConnectedDevices(BluetoothProfile.GATT);
                for (BluetoothDevice device : connectedDevices) {
                    LeScannerReport report = new LeScannerReport(device.getAddress());
                    report.device = device;
                    report.rssi = 0;
                    ScannedDeviceItem deviceItem = new ScannedDeviceItem();
                    deviceItem.report = report;
                    deviceItem.name = report.device.getName();
                    deviceItem.isConnected = true;
                    // mReportsMap.put(report.address, deviceItem); 已连接的设备不更新RSSI
                    mConnectedDevice.add(deviceItem);
                }
            }

            Collections.sort(mConnectedDevice, this);
            Collections.sort(mBondDevice, this);

            boolean showBond = !mBondDevice.isEmpty();
            multiListHelper.getList(0).enable = showBond;
            multiListHelper.getList(1).enable = showBond;

            boolean showConnected = !mConnectedDevice.isEmpty();
            multiListHelper.getList(2).enable = showConnected;
            multiListHelper.getList(3).enable = showConnected;
        }

        private void handleScanResult(LeScannerReport report) {
            if (report == null) return;

            ScannedDeviceItem deviceItem;
            try {
                synchronized (this) {
                    deviceItem = mReportsMap.get(report.device.getAddress());
                    if (deviceItem == null) {
                        deviceItem = new ScannedDeviceItem();
                        deviceItem.name = report.device.getName();
                        mReports.add(deviceItem);
                        mReportsMap.put(report.address, deviceItem); // 增加时才缓存
                    }
                }

                deviceItem.lastUpdate = System.currentTimeMillis();
                deviceItem.addRssi(report.rssi);

                String localName = report.advData.getLocalName();
                if (localName != null) {
                    deviceItem.name = localName;
                } else {
                    if (deviceItem.name == null) {
                        deviceItem.name = "N/A";
                    }
                }

                deviceItem.report = report;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            pendingNotifyChanged = false;
            synchronized (this) {
                // 如果有排序就全部更新，否则就判断是否能够部分更新
                if (pendingSort) {
                    pendingSort = false;
                    Collections.sort(mReports, this);
                    notifyDataSetChanged();
                } else {
                    // 刷新修改
                    //DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCB);
                    //diffResult.dispatchUpdatesTo(this);
                    // 如果长度发生变化，就全部更新。否则就判断哪些要更新
                    int reportCount = mReports.size();
                    if (lastReportCount != reportCount) {
                        notifyDataSetChanged();
                    } else {
                        // 因为是复合的列表，所以要计算report在界面上的位置
                        int bias = multiListHelper.size() - reportCount;
                        // 只更新需要更新的
                        for (int i = 0; i < reportCount; i++) {
                            ScannedDeviceItem report = mReports.get(i);
                            if (report.lastUpdate > lastUpdateRvTime) {
                                notifyItemChanged(bias + i);
                            }
                        }
                        // 增加对绑定列表中更新过的项目的刷新
                        int bondCount = mBondDevice.size();
                        for (int i = 0; i < bondCount; i++) {
                            ScannedDeviceItem report = mBondDevice.get(i);
                            if (report.lastUpdate > lastUpdateRvTime) {
                                notifyItemChanged(1 + i);
                            }
                        }
                    }
                    lastReportCount = reportCount;
                }
            }

            lastUpdateRvTime = System.currentTimeMillis();
            if (lastUpdateRvTime > stopTime) {
                stopScan();
            }
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);

            // 动画必须在UI更新间隔之中完成
            RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
            if (itemAnimator instanceof SimpleItemAnimator) {
                SimpleItemAnimator animator = (SimpleItemAnimator) itemAnimator;
                int duration = notifyChangedInterval;
                animator.setAddDuration(duration);
                animator.setRemoveDuration(duration);
                animator.setMoveDuration(duration);
                animator.setChangeDuration(duration);
                animator.setSupportsChangeAnimations(false);
            }
        }

        @NonNull
        @Override
        public VVV onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new VVV(inflater.inflate(R.layout.item_list_scanner_device, parent, false), this);
            }
            return new VVV(inflater.inflate(R.layout.item_list_scanner_title, parent, false), this);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull VVV holder, int position) {
            int type = multiListHelper.getItemViewType(position);
            if (type == 0) {
                ScannedDeviceItem item = (ScannedDeviceItem) multiListHelper.getItem(position);
                holder.nameTv.setText(item.name);
                holder.addressTv.setText(item.report.address);
                if (item.report.rssi == 0) {
                    holder.rssiTv.setText(null);
                } else {
                    holder.rssiTv.setText(item.report.rssi + "dBm");
                }
            } else {
                int titleStrId = (Integer) multiListHelper.getItem(position);
                holder.nameTv.setText(titleStrId);
                //String title = (String) multiListHelper.getItem(position);
                //holder.nameTv.setText(title);
            }
        }

        @Override
        public int getItemCount() {
            return multiListHelper.size();
        }

        @Override
        public int getItemViewType(int position) {
            return multiListHelper.getItemViewType(position);
        }

        @Override
        public void onEvent(Object src, int evtType, Object evtData) {
            if (src == easyLeScanner) {
                if (evtType == EasyLeScanner.EVT_FOUND) {
                    LeScannerReport report = (LeScannerReport) evtData;
                    handleScanResult(report);
                    if (!pendingNotifyChanged) {
                        pendingNotifyChanged = true;
                        mHandler.postDelayed(this, notifyChangedInterval);
                    }
                }
            }
        }

        @Override
        public int compare(ScannedDeviceItem o1, ScannedDeviceItem o2) {
            if (o1 == o2) {
                return 0;
            }
            // 对于绑定设备进行排序
            if (o1.isBonded || o1.isConnected) {
                return o1.report.address.compareTo(o2.report.address);
            }
            return o2.getRssiAvg() - o1.getRssiAvg();
        }

        void onClick(int position) {
            int type = multiListHelper.getItemViewType(position);
            if (type == 0) {
                eventSelected.postEvent((ScannedDeviceItem) multiListHelper.getItem(position));
            }
            //else if (type == 1) {
            //    MultiListHelperItem item = multiListHelper.dataSet.get(1);
            //    item.enable = !item.enable;
            //    notifyDataSetChanged();
            //} else if (type == 2) {
            //    MultiListHelperItem item = multiListHelper.dataSet.get(3);
            //    item.enable = !item.enable;
            //    notifyDataSetChanged();
            //}
        }
    }

    static class VVV extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ScannerAdapter adapter;

        final TextView nameTv;
        final TextView addressTv;
        final TextView rssiTv;

        public VVV(@NonNull View itemView, ScannerAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            if (itemView instanceof TextView) {
                nameTv = (TextView) itemView;
                addressTv = null;
                rssiTv = null;
            } else {
                nameTv = itemView.findViewById(R.id.text_name);
                addressTv = itemView.findViewById(R.id.text_address);
                rssiTv = itemView.findViewById(R.id.text_rssi);
            }

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            adapter.onClick(getAdapterPosition());
        }
    }

    static class MultiListHelper {
        final ArrayList<MultiListHelperItem> dataSet = new ArrayList<>();

        public void add(List data, int type) {
            dataSet.add(new MultiListHelperItem(data, type));
        }

        public void clear() {
            dataSet.clear();
        }

        public int size() {
            int cnt = 0;
            for (MultiListHelperItem item : dataSet) {
                if (item.enable) {
                    cnt += item.data.size();
                }
            }
            return cnt;
        }

        public MultiListHelperItem getList(int dataSetIdx) {
            if (dataSetIdx < dataSet.size()) {
                return dataSet.get(dataSetIdx);
            }
            return null;
        }

        public int getItemViewType(int position) {
            for (MultiListHelperItem item : dataSet) {
                if (item.enable) {
                    int size = item.data.size();
                    if (position < size) {
                        return item.type;
                    }
                    position -= size;
                }
            }
            return 0;
        }

        public Object getItem(int position) {
            for (MultiListHelperItem item : dataSet) {
                if (item.enable) {
                    int size = item.data.size();
                    if (position < size) {
                        return item.data.get(position);
                    }
                    position -= size;
                }
            }
            return null;
        }
    }

    static class MultiListHelperItem {
        public final List data;
        public final int type;
        public boolean enable = true;

        MultiListHelperItem(List data, int type) {
            this.data = data;
            this.type = type;
        }
    }
}
