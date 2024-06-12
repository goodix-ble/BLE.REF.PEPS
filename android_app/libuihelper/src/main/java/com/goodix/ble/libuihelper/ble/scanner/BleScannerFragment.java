package com.goodix.ble.libuihelper.ble.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libble.BleUuid;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.util.HexStringParser;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.DebouncedClickListener;
import com.goodix.ble.libuihelper.input.EditTextCleanupHelper;
import com.goodix.ble.libuihelper.input.SeekBarAndCheckBoxHelper;
import com.goodix.ble.libuihelper.input.filter.HexInputFilter;
import com.goodix.ble.libuihelper.view.AsyncBatchLayoutInflater;

import java.util.Objects;
import java.util.UUID;


@SuppressWarnings("FieldCanBeLocal")
public class BleScannerFragment extends DialogFragment implements View.OnClickListener, Runnable, CompoundButton.OnCheckedChangeListener, AsyncBatchLayoutInflater.CB {
    private final static int SCAN_DURATION = 10_000;
    private final static int SCAN_DURATION_LONG = 24 * 60 * 60_000;
    public static final int REQUEST_CODE = 87;

    public static final int EVT_DEVICE_SELECTED = 426;
    private Event<ScannedDeviceItem> eventDeviceSelected;

    private TextView titleTv;
    private String mTitleStr = null;

    private Button startBtn;
    private ImageButton settingBtn, closeBtn;
    private LinearLayout settingLL;

    private CheckBox nameCb;
    private CheckBox macCb;
    private CheckBox uuidCb;
    private CheckBox rssiCb;
    private CheckBox showBondedCb, showConnectedCb, continuouslyScanCb, sortCb;
    private EditText nameEd;
    private EditText macEd;
    private EditText uuidEd;
    private SeekBarAndCheckBoxHelper rssiHelper;

    //    private String filterName;
//    private String filterMac;
//    private int filterRssi;
//    @NonNull
//    private ArrayList<ParcelUuid> serviceUuids = new ArrayList<>();

    private Cfg innerCfg = new Cfg();
    private boolean firstRun = true;

    private InnerClass mAdapter = new InnerClass();
    private BluetoothManager btManager;
    private ScannedDeviceItem selectedDevice = null;


    public interface CB {
        void onDeviceSelected(@Nullable ScannedDeviceItem device);
    }

    public BleScannerFragment setTitle(String title) {
        this.mTitleStr = title;
        if (titleTv != null) {
            if (mTitleStr != null) {
                titleTv.setText(mTitleStr);
            } else {
                titleTv.setVisibility(View.GONE);
                titleTv.setText(R.string.libuihelper_scan);
            }
        }
        return this;
    }

    public BleScannerFragment setCfg(Cfg cfg) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            setArguments(arguments);
        }
        arguments.putParcelable(Cfg.class.getName(), cfg);
        return this;
    }

    public Event<ScannedDeviceItem> evtDeviceSelected() {
        if (eventDeviceSelected == null) {
            synchronized (this) {
                if (eventDeviceSelected == null) {
                    eventDeviceSelected = new Event<>(this, EVT_DEVICE_SELECTED);
                }
            }
        }
        return eventDeviceSelected;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = getActivity();
        if (ctx == null) {
            ctx = inflater.getContext();
        }

        btManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);

        View ret = null;
        if (container == null) {
            container = new FrameLayout(ctx);
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ret = container;
        }

        // 使用子线程加载布局，避免启动时，界面卡顿
        // 必须使用LayoutInflater.from(getActivity())，如果使用形参inflater则会导致主题丢失。
        new AsyncBatchLayoutInflater(LayoutInflater.from(ctx), container, 1)
                .add(R.layout.libuihelper_fragment_ble_scanner)
                .start(this);

        return ret;
    }


    @Override
    public void onLayoutInflaterReady(LayoutInflater inflater, ViewGroup container) {

        @SuppressLint("InflateParams") final View root = inflater.inflate(R.layout.libuihelper_fragment_ble_scanner, null);
        container.addView(root);
        DebouncedClickListener clickListener = new DebouncedClickListener(this);

        titleTv = root.findViewById(R.id.libuihelper_fragment_ble_scanner_title_tv);
        setTitle(mTitleStr);

        RecyclerView resultRv = root.findViewById(R.id.libuihelper_fragment_ble_scanner_rv);
        resultRv.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        resultRv.getRecycledViewPool().setMaxRecycledViews(0, 32);
        resultRv.setAdapter(mAdapter);

        startBtn = root.findViewById(R.id.libuihelper_fragment_ble_scanner_start_btn);
        settingBtn = root.findViewById(R.id.libuihelper_fragment_ble_scanner_setting_btn);
        closeBtn = root.findViewById(R.id.libuihelper_fragment_ble_scanner_close_btn);

        startBtn.setOnClickListener(clickListener);
        settingBtn.setOnClickListener(clickListener);
        closeBtn.setOnClickListener(clickListener);

        settingLL = root.findViewById(R.id.libuihelper_fragment_ble_scanner_setting_ll);
        settingLL.setVisibility(View.GONE);

        nameCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_name_cb);
        nameEd = root.findViewById(R.id.libuihelper_fragment_ble_scanner_name_ed);
        nameCb.setOnCheckedChangeListener(mAdapter);
        nameEd.addTextChangedListener(mAdapter);
        new EditTextCleanupHelper().attach(nameEd);

        macCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_mac_cb);
        macEd = root.findViewById(R.id.libuihelper_fragment_ble_scanner_mac_ed);
        macEd.setFilters(new InputFilter[]{new HexInputFilter().setSpecialChars(':'), new InputFilter.LengthFilter(5 * 3 + 2)});
        macCb.setOnCheckedChangeListener(mAdapter);
        macEd.addTextChangedListener(mAdapter);
        new EditTextCleanupHelper().attach(macEd);

        uuidCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_uuid_cb);
        uuidEd = root.findViewById(R.id.libuihelper_fragment_ble_scanner_uuid_ed);
        uuidEd.setFilters(new InputFilter[]{new HexInputFilter().setSpecialChars('-'), new InputFilter.LengthFilter(16 * 2 + 4)});
        uuidCb.setOnCheckedChangeListener(mAdapter);
        uuidEd.addTextChangedListener(mAdapter);
        new EditTextCleanupHelper().attach(uuidEd);

        rssiCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_rssi_cb);
        rssiHelper = new SeekBarAndCheckBoxHelper(
                root.findViewById(R.id.libuihelper_fragment_ble_scanner_rssi_seekbar)
                , root.findViewById(R.id.libuihelper_fragment_ble_scanner_rssi_tv)
                , rssiCb
        );
        rssiHelper.setFormat((helper, value, out) -> {
            if (value == 0) {
                out.put("0dBm");
            } else {
                out.a("-").append(value).a("dBm");
            }
            innerCfg.filter.minRssi = -value; // 防止连带把checkRssi也设置为true了
        });
        rssiHelper.setRange(0, 100);
        // rssiHelper.setValue(60); 由Cfg类的构造函数来指定默认值
        rssiCb.setOnClickListener(mAdapter);

        showBondedCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_show_bonded_cb);
        showConnectedCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_show_connected_cb);
        continuouslyScanCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_continuously_scan_cb);
        sortCb = root.findViewById(R.id.libuihelper_fragment_ble_scanner_sort_cb);
        showBondedCb.setOnCheckedChangeListener(mAdapter);
        showConnectedCb.setOnCheckedChangeListener(mAdapter);
        continuouslyScanCb.setOnCheckedChangeListener(mAdapter);
        sortCb.setOnCheckedChangeListener(this); // 是否排序不应该终止扫描，所以单独处理。

        selectedDevice = null;

        Bundle arguments = getArguments();
        boolean autoStart = true;
        if (arguments != null && firstRun) {
            firstRun = false;
            Cfg cfg = arguments.getParcelable(Cfg.class.getName());
            if (cfg != null) {
                innerCfg = cfg;

                settingBtn.setVisibility(cfg.showSetting ? View.VISIBLE : View.INVISIBLE);

                autoStart = cfg.autoStart;
            }
        }

        mAdapter.fromUser = false; // 阻止UI的响应式逻辑
        loadSetting(); // 只需要加载一次
        mAdapter.fromUser = true; // 恢复UI的响应式逻辑

        if (autoStart) {
            startBtn.performClick();
        }
    }

    @Override
    public void onDestroyView() {
        mAdapter.stopScan();
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.FullScreenPopupDialog);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        Objects.requireNonNull(window).setGravity(Gravity.BOTTOM); //可设置dialog的位置
        window.getDecorView().setPadding(0, 0, 0, 0); //消除边距
        //window.getDecorView().setBackgroundColor(0xFFFFFFFF); // 一定要设置背景才能填满屏幕

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;   //设置宽度充满屏幕
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 因为采用了异步加载布局，此时该按钮可能为null。
        if (closeBtn != null) {
            if (getDialog() != null) {
                closeBtn.setVisibility(View.VISIBLE);
            } else {
                closeBtn.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        FragmentActivity host = getActivity();
        if (host instanceof CB) {
            ((CB) host).onDeviceSelected(selectedDevice);
        } else {
            Fragment parent = getParentFragment();
            if (parent instanceof CB) {
                ((CB) parent).onDeviceSelected(selectedDevice);
            }
        }

        Event<ScannedDeviceItem> evt = this.eventDeviceSelected;
        if (evt != null) {
            evt.postEvent(selectedDevice);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode != Activity.RESULT_CANCELED) {
                toggleStart();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleStart();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == startBtn) {
            toggleStart();
        } else if (v == settingBtn) {
            if (settingLL.getVisibility() != View.VISIBLE) {
                settingLL.setVisibility(View.VISIBLE);
            } else {
                settingLL.setVisibility(View.GONE);
            }
        } else if (v == closeBtn) {
            dismiss();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == sortCb) {
            mAdapter.sortByRssi(0); // 启用时立即排序一次
            mAdapter.sortByRssi(isChecked ? -1 : 1);
        }
    }

    private void loadSetting() {

        Cfg cfg = this.innerCfg;

        showBondedCb.setChecked(cfg.showBonded);
        mAdapter.setShowBonded(cfg.showBonded);

        showConnectedCb.setChecked(cfg.showConnected);
        mAdapter.setShowConnected(btManager, cfg.showConnected);

        continuouslyScanCb.setChecked(cfg.longScan);

        nameCb.setChecked(cfg.filter.checkLocalName);
        nameEd.setText(cfg.filter.localName);

        macCb.setChecked(cfg.filter.checkAddress);
        macEd.setText(cfg.filter.address);

        rssiCb.setChecked(cfg.filter.checkRssi);
        if (cfg.filter.minRssi > 0) {
            rssiHelper.setValue(0);
        } else {
            rssiHelper.setValue(-cfg.filter.minRssi);
        }

        uuidCb.setChecked(cfg.filter.checkServiceUuid);
        if (cfg.filter.serviceUuids.isEmpty()) {
            uuidEd.setText("");
        } else {
            UUID uuid = cfg.filter.serviceUuids.get(0);
            if (BleUuid.is16bit32bitUuid(uuid)) {
                int uuid16bit = (int) ((uuid.getMostSignificantBits() >> 32) & 0xFFFFFFFFL);
                uuidEd.setText(String.format("%04X", uuid16bit));
            } else {
                uuidEd.setText(uuid.toString());
            }
        }
    }

    private void saveSetting() {
        Cfg cfg = this.innerCfg;

        cfg.showBonded = showBondedCb.isChecked();
        mAdapter.setShowBonded(cfg.showBonded);

        cfg.showConnected = showConnectedCb.isChecked();
        mAdapter.setShowConnected(btManager, cfg.showConnected);

        cfg.longScan = continuouslyScanCb.isChecked();


        LeScannerFilter filter = cfg.filter;

        filter.checkLocalName = nameCb.isChecked();
        filter.localName = nameEd.getText().toString();

        filter.checkAddress = macCb.isChecked();
        filter.address = macEd.getText().toString();

        filter.checkRssi = rssiCb.isChecked();
        filter.minRssi = -rssiHelper.getValue();

        UUID uuid = null;
        String uuidStr = uuidEd.getText().toString();
        try {
            int strLen = uuidStr.length();
            if (uuidStr.contains("-")) {
                if (strLen == (8 + 4 + 4 + 4 + 12 + 4)) {
                    // 128bit UUID
                    uuid = BleUuid.from(uuidStr);
                }
            } else {
                if (strLen == 4 || strLen == 8) {
                    // 16bit/32bit UUID
                    uuid = BleUuid.from(HexStringParser.parseInt(uuidStr));
                }
            }
        } catch (Throwable ignored) {
        }
        filter.checkServiceUuid = uuidCb.isChecked();
        if (uuid != null) {
            if (filter.serviceUuids.isEmpty()) {
                filter.serviceUuids.add(uuid);
            } else {
                filter.serviceUuids.set(0, uuid);
            }
        }
    }

    private void toggleStart() {
        if (mAdapter.isScanning()) {
            mAdapter.stopScan();
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (!adapter.isEnabled()) {
                final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_CODE);
                return;
            }

            boolean granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!granted) { // 只需要在没有获得权限时申请权限就可以了
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                return;
            }

            int timeout = continuouslyScanCb.isChecked() ? SCAN_DURATION_LONG : SCAN_DURATION;
            mAdapter.clear();
            mAdapter.startScan(timeout);
            if (!sortCb.isChecked()) {
                mAdapter.sortByRssi(1000); // 默认排序前1.5秒的
            }
        }
    }

    @Override
    public void run() {
        // 用于修改设置后及时保存配置，且使用延时来消抖
        long now = System.currentTimeMillis();
        if (now < mAdapter.updateTime) {
            mAdapter.mHandler.postDelayed(this, mAdapter.updateTime - now);
            return;
        }

        // 表示消抖延时已经结束
        mAdapter.idle = true;
        // 保存配置
        saveSetting();
        // 停止扫描，以便用新的配置开启新的扫描
        if (mAdapter.isScanning()) {
            toggleStart();
        }
    }

    class InnerClass extends EasyLeScannerAdapter implements CompoundButton.OnCheckedChangeListener, TextWatcher, View.OnClickListener {
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private boolean idle = true; // 表示消抖延时是否已经存在
        private boolean fromUser = true;
        private long updateTime = 0; // 用于确保有200ms的延时，达到消抖的目标
        private boolean textAdding = false;

        public InnerClass() {
            super(R.layout.libuihelper_item_scan_result);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (idle && fromUser) {
                // post updating
                idle = false;
                updateTime = System.currentTimeMillis() + 200;
                mHandler.postDelayed(BleScannerFragment.this, 200);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //Log.d("+++", "beforeTextChanged() called with: s = [" + s + "], start = [" + start + "], count = [" + count + "], after = [" + after + "]");
            textAdding = count == 0 && after == 1 && s.length() == start; // append one char at the end of string.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //Log.d("+++", "onTextChanged() called with: s = [" + s + "], start = [" + start + "], before = [" + before + "], count = [" + count + "]");
        }

        @Override
        public void afterTextChanged(Editable s) {
            //Log.d("+++", "afterTextChanged() " + fromUser);

            if (s == nameEd.getText()) {
                if (nameCb.isChecked()) {
                    onCheckedChanged(null, false); // 请求更新
                    if (s.length() == 0 && fromUser) { // 使能的情况下才自动设置控件
                        nameCb.setChecked(false);
                    }
                } else {
                    if (s.length() > 0 && fromUser) nameCb.setChecked(true);
                }
            } else if (s == macEd.getText()) {
                if (macCb.isChecked()) {
                    onCheckedChanged(null, false); // 请求更新
                    if (textAdding) {
                        int pos = 2;
                        String delimiterS = ":";
                        char delimiterC = delimiterS.charAt(0);
//                    if (s.length() > pos && s.charAt(pos) != delimiterC) s.insert(pos, delimiterS);
//                    pos = 5;
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 5;
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 8;
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 11;
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 14;
                        if (s.length() == pos) s.append(delimiterC);
                        //Log.e("+++", "+++++++++++");
                    }
                    if (s.length() == 0 && fromUser) {
                        macCb.setChecked(false);
                    }
                } else {
                    if (s.length() > 0 && fromUser) macCb.setChecked(true);
                }
            } else if (s == uuidEd.getText()) {
                if (uuidCb.isChecked()) {
                    onCheckedChanged(null, false); // 请求更新
                    if (textAdding) {
                        int pos = 8;
                        String delimiterS = "-";
                        char delimiterC = delimiterS.charAt(0);
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 13;
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 18;
                        if (s.length() == pos) s.append(delimiterC);
                        pos = 23;
                        if (s.length() == pos) s.append(delimiterC);
                    }
                    if (s.length() == 0 && fromUser) {
                        uuidCb.setChecked(false);
                    }
                } else {
                    if (s.length() > 0 && fromUser) uuidCb.setChecked(true);
                }
            }
        }

        @Override
        public void onClick(View v) {
            onCheckedChanged(null, false);
        }


        @Override
        protected void onStartScan() {
            startBtn.setText(R.string.libuihelper_stop);
        }

        @Override
        protected void onStopScan() {
            startBtn.setText(R.string.libuihelper_start);
        }

        @Override
        protected void onError(String msg) {
            if (startBtn != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(startBtn.getContext());
                builder.setTitle(R.string.libuihelper_error)
                        .setMessage(startBtn.getContext().getString(R.string.libuihelper_failed_to_scan, msg))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } else {
                super.onError(msg);
            }
        }

        @Override
        protected void onViewHolderClickListener(VH vh, View v, ScannedDeviceItem deviceItem, int position) {
            selectedDevice = deviceItem;
            if (getDialog() != null) {
                dismiss();
            }
            //Toast.makeText(requireContext(), "POS: " + pos, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected boolean onMatch(LeScannerReport report) {
            return BleScannerFragment.this.innerCfg.filter.match(report);
        }

        @Override
        protected void onBindViewHolder(@NonNull VH holder, ScannedDeviceItem device, int position) {
            if (holder.itemView.getTag() != device) {
                holder.itemView.setTag(device);
                holder.getTextView(R.id.libuihelper_item_scan_result_name_tv).setText(device.name);
                holder.getTextView(R.id.libuihelper_item_scan_result_mac_tv).setText(device.report.address);
                if (device.isConnected) {
                    holder.getTextView(R.id.libuihelper_item_scan_result_rssi_tv).setText(R.string.libuihelper_connected);
                }
                if (device.isBonded) {
                    holder.getTextView(R.id.libuihelper_item_scan_result_status_tv).setText(R.string.libuihelper_bonded);
                } else {
                    holder.getTextView(R.id.libuihelper_item_scan_result_status_tv).setText(null);
                }
            }
            if (!device.isConnected) {
                if (device.isBonded && device.report.rssi == 0) {
                    holder.getTextView(R.id.libuihelper_item_scan_result_rssi_tv).setText(null);
                } else {
                    holder.getTextView(R.id.libuihelper_item_scan_result_rssi_tv).setText(device.report.rssi + "dBm");
                }
            }
        }
    }

    public static class Cfg implements Parcelable {
        public boolean autoStart = true;
        public boolean showBonded = false;
        public boolean showConnected = false;
        public boolean showSetting = true;
        public boolean longScan = false;

        public LeScannerFilter filter = new LeScannerFilter();

        public Cfg() {
            filter.minRssi = -60; // 默认值-60dbm
        }

        public Cfg addUuid(UUID uuid) {
            if (uuid != null) {
                filter.checkServiceUuid = true;
                filter.serviceUuids.add(uuid);
            }
            return this;
        }

        public Cfg setRssiFilter(int minRssi) {
            filter.checkRssi = true;
            filter.minRssi = minRssi;
            return this;
        }

        private Cfg(Parcel in) {
            autoStart = in.readInt() != 0;
            showBonded = in.readInt() != 0;
            showConnected = in.readInt() != 0;
            showSetting = in.readInt() != 0;
            longScan = in.readInt() != 0;

            ClassLoader classLoader = this.getClass().getClassLoader();
            filter = in.readParcelable(classLoader);
        }

        public static final Creator<Cfg> CREATOR = new Creator<Cfg>() {
            @Override
            public Cfg createFromParcel(Parcel in) {
                return new Cfg(in);
            }

            @Override
            public Cfg[] newArray(int size) {
                return new Cfg[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(autoStart ? 1 : 0);
            dest.writeInt(showBonded ? 1 : 0);
            dest.writeInt(showConnected ? 1 : 0);
            dest.writeInt(showSetting ? 1 : 0);
            dest.writeInt(longScan ? 1 : 0);

            dest.writeParcelable(filter, flags);
        }
    }
}
