package com.goodix.ble.gr.toolbox.common.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.goodix.ble.gr.toolbox.common.AboutAlert;
import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.scanner.ExtendedBluetoothDevice;
import com.goodix.ble.gr.toolbox.common.scanner.ScannerFragment;
import com.goodix.ble.gr.toolbox.common.ui.ClickDebounceHelper;
import com.goodix.ble.gr.toolbox.common.ui.MyScrollView;
import com.goodix.ble.gr.toolbox.common.util.AppUtils;
import com.goodix.ble.gr.toolbox.common.util.BLERequest;
import com.goodix.ble.gr.toolbox.common.util.ToastUtil;
import com.goodix.ble.libble.center.BleCenter;
import com.goodix.ble.libble.center.BleItem;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libble.v2.gb.pojo.GBError;
import com.goodix.ble.libble.v2.profile.BatteryService;
import com.goodix.ble.libcomx.event.EventDisposer;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.task.TaskQueue;
import com.goodix.ble.libuihelper.ble.blecenter.BaseBleDeviceFragment;
import com.goodix.ble.libuihelper.ble.scanner.BleScannerFragment;
import com.goodix.ble.libuihelper.ble.scanner.ScannedDeviceItem;
import com.goodix.ble.libuihelper.dialog.TaskBusyDialog;
import com.goodix.ble.libuihelper.logger.Log;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.util.UUID;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public abstract class AbstractBleFragment extends BaseBleDeviceFragment implements ScannerFragment.OnDeviceSelectedListener, BleScannerFragment.CB {

    private TextView deviceNameTv;
    private Button connectBtn;

    @Nullable
    ConnStateDialog connStateDialog;

    private ImageView batteryIv;
    private TextView batteryTv;

    private boolean isDestroying = false; // 判断是否要显示重连对话框

    protected Config fragmentConfig = new Config();
    protected Context appCtx;
    protected MyScrollView contentScroll;
    protected int scrollCount = 0;
    private boolean pendingConnectionRequestForEnableBt = false;
    private boolean mScanButtonShowFlag = false;

    final protected Executor uiExecutor = UiExecutor.getDefault();

    protected Menu mMenu = null;
    protected View mRoot = null;

    @Nullable
    protected BatteryService mBattery;

    @Nullable
    protected BleScannerFragment bleScannerFragment = null; // remain an instance of BleScannerFragment for remaining last settings.

    private final IEventListener connectionEvt = (src, type, evtData) -> {
        switch (type) {
            case GBRemoteDevice.EVT_STATE_CHANGED: {
                switch (((Integer) evtData)) {
                    case GBRemoteDevice.STATE_CONNECTING:
                        onDeviceConnecting();
                        break;
                    case GBRemoteDevice.STATE_CONNECTED:
                        onDeviceConnected();
                        break;
                    case GBRemoteDevice.STATE_DISCONNECTING:
                        onDeviceDisconnecting();
                        break;
                    case GBRemoteDevice.STATE_DISCONNECTED:
                        onDeviceDisconnected();
                        break;
                }
                break;
            }
            case GBRemoteDevice.EVT_ERROR:
                GBError e = (GBError) evtData;
                onError(e.getMessage(), e.getErrorCode());
                break;
            case GBRemoteDevice.EVT_READY:
                Boolean isReady = (Boolean) evtData;
                if (isReady) {
                    onDeviceReady();
                } else {
                    onDeviceNotSupported();
                }
                break;
        }
    };

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        appCtx = inflater.getContext().getApplicationContext();
        BleCenter.setContext(appCtx);
        BleCenter.setRootLogger(Log.getLogger());
        if (!BLERequest.isBLEEnabled(appCtx)) {
            BLERequest.showBLEDialog(this);
        }

        mRoot = inflater.inflate(R.layout.activity_base_profile, container, false);

        // In onInitialize method a final class may register local broadcast receivers that will listen for events from the service
        onInitialize(savedInstanceState);

        // The onCreateContentView class should... create the view
        FrameLayout contentContainer = findViewById(R.id.scroll_content_view_layout);
        onCreateContentView(inflater, mRoot, contentContainer, savedInstanceState);

        setUpView();

        // View is ready to be used
        onViewCreated(savedInstanceState);

        setHasOptionsMenu(getMenuResId() != 0 || onGetAboutInfo() != 0);

        super.confirmClose = true;

        return mRoot;
    }

    protected void onInitialize(final Bundle savedInstanceState) {
        // empty default implementation
    }

    protected void onViewCreated(final Bundle savedInstanceState) {
        // empty default implementation
    }

    protected abstract void onBindTo(BleItem targetDevice, TaskQueue setupSteps, EventDisposer deviceDisposer);

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        mMenu = menu;

        int resId = getMenuResId();

        if (resId != 0) {
            inflater.inflate(getMenuResId(), menu);
        } else {
            if (onGetAboutInfo() != 0) {
                inflater.inflate(R.menu.default_menu, menu);
            }
        }

    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        // 优先让子类判断是否需要拦截
        if (onOptionsItemSelected(mMenu, item, id)) {
            return true;
        }

        // 再进行默认处理
        if (id == android.R.id.home) {
            FragmentActivity act = getActivity();
            if (act != null) {
                //act.onBackPressed();
                act.finish();
                return true;
            }
        } else if (id == R.id.about_dj) {
            int stringId = onGetAboutInfo();
            if (stringId != 0) {
                AboutAlert.showAboutAlert(requireContext(), stringId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (!isDestroying) {
            isDestroying = true;
            // disconnectDevice(); 由 BaseBleDeviceFragment.onDestroy() 来释放目标设备
        }

        if (targetDevice != null) {
            targetDevice.getGatt().clearEventListener(this);
        }

        setTargetDevice((BleItem) null);
    }

    public void onDeviceConnecting() {
        connectBtn.setText(R.string.common_connecting);
        if (connStateDialog != null) {
            connStateDialog.showConnecting();
        }
    }

    public void onDeviceConnected() {
        // 设备连上时，也刷新一下设备名称。
        this.deviceNameTv.setText(getDeviceName());

        if (batteryServiceSupport()) {
            AppUtils.setImageViewColor(batteryIv, appCtx);
        }
        if (connStateDialog != null) {
            connStateDialog.showDiscoveringService();
        }
        connectBtn.setText(R.string.common_disconnect);
        ToastUtil.success(appCtx, R.string.common_connected).show();
    }

    public void onDeviceDisconnecting() {
        connectBtn.setText(R.string.common_disconnecting);
    }

    public void onDeviceDisconnected() {
        if (batteryServiceSupport()) {
            AppUtils.setImageViewColor(batteryIv, ContextCompat.getColor(appCtx, R.color.textColorLight));
        }
        if (!isDestroying) {
            showDeviceDisconnected();
        }
        connectBtn.setText(R.string.common_connect);
        deviceNameTv.setText(R.string.common_no_device);
    }

    public void onDeviceReady() {
        if (connStateDialog != null) {
            connStateDialog.close();
        }
        ToastUtil.success(appCtx, R.string.common_ready).show();
    }

    public void onError(String message, int errorCode) {
        FragmentActivity act = getActivity();
        String msg = message + " (" + errorCode + ")";
        if (act != null) {
            ToastUtil.dialog(act, msg)
                    .setTitle(R.string.libuihelper_error)
                    .show();
        } else {
            ToastUtil.error(appCtx, msg).show();
        }
    }

    public void onDeviceNotSupported() {
        ToastUtil.error(appCtx, R.string.common_not_support).show();
        if (connStateDialog != null) {
            connStateDialog.showNotSupported();
        }
        if (targetDevice != null) {
            targetDevice.getGatt().disconnect(true).startProcedure();
        }
    }

    @Override
    public void onDeviceSelected(ExtendedBluetoothDevice device, String name, byte[] scanByte) {
        setTargetDevice(device.macAddr);

        // 发起连接
        connectDevice();
    }

    @Override
    public void onDialogCanceled() {
        //do nothing
    }

    @Override
    public void onDeviceSelected(@Nullable ScannedDeviceItem device) {
        if (device != null) {
            setTargetDevice(device.report.address);

            // 发起连接
            connectDevice();
        }
    }

    public void connectDevice() {
        if (!BLERequest.isBLEEnabled(appCtx)) {
            pendingConnectionRequestForEnableBt = (targetDevice != null);
            BLERequest.showBLEDialog(this);
            return;
        }
        if (targetDevice != null) {
            targetDevice.getGatt().connect(0).startProcedure();
        }
    }

    /**
     * 留给子类重新定义按钮点击断开时的动作
     */
    public void disconnectDevice() {
        if (targetDevice != null) {
            targetDevice.getGatt().disconnect(true).startProcedure();
        }
    }

    /**
     * 子类覆写以决定怎么显示连接对话框
     */
    protected void showDeviceDisconnected() {
        if (connStateDialog != null) {
            connStateDialog.showDisconnected();
        }
    }

    protected View setContentViewInScroll(int resId) {
        FrameLayout fl = findViewById(R.id.scroll_content_view_layout);
        View view = getLayoutInflater().inflate(resId, fl, false);
        if (fl != null) {
            fl.addView(view);
        }
        return view;
    }

    protected final <T extends View> T findViewById(@IdRes int id) {
        if (id == View.NO_ID) {
            return null;
        }
        if (mRoot == null) {
            return null;
        }
        return mRoot.findViewById(id);
    }

    /**
     * Shows the scanner fragment.
     *
     * @param filter the UUID filter used to filter out available devices. The fragment will always show all bonded devices as there is no information about their
     *               services
     * @see #getFilterUUID()
     */
    public void showDeviceScanningDialog(final UUID filter) {
        final ScannerFragment dialog = ScannerFragment.getInstance(filter, false, null);
        dialog.show(getChildFragmentManager(), "scan_fragment");
    }

    @Nullable
    protected BleItem getTargetDevice() {
        return targetDevice;
    }

    public boolean isConnected() {
        return targetDevice != null && targetDevice.getGatt().isConnected();
    }

    public boolean isDisconnected() {
        return targetDevice == null || targetDevice.getGatt().isDisconnected();
    }

    protected boolean isReady() {
        return targetDevice != null && targetDevice.getGatt().isReady();
    }

    protected void setTargetDevice(String address) {
        if (address == null) {
            setTargetDevice((BleItem) null);
        } else {
            try {
                setTargetDevice(BleCenter.get().addDevice(address));
            } catch (Exception e) {
                ToastUtil.error(appCtx, "Error: " + e.getMessage()).show();
            }
        }
    }

    @Override
    public void onDeviceChanged(BleItem targetDevice) {
        if (targetDevice == null) {
            if (this.deviceNameTv != null) {
                this.deviceNameTv.setText(R.string.common_no_device);
            }
            if (this.connectBtn != null) {
                this.connectBtn.setText(R.string.common_connect);
            }
            this.setTabDesc(getString(R.string.libuihelper_na));
            setDefaultUI();
            return;
        }

        // 绑定必要的事件
        targetDevice.getGatt().evtStateChanged()
                .subEvent(this)
                .setDisposer(deviceEventDisposer)
                .setExecutor(UiExecutor.getDefault())
                .register(connectionEvt);

        targetDevice.getGatt().evtError()
                .subEvent(this)
                .setDisposer(deviceEventDisposer)
                .setExecutor(UiExecutor.getDefault())
                .register(connectionEvt);

        targetDevice.getGatt().evtReady()
                .subEvent(this)
                .setDisposer(deviceEventDisposer)
                .setExecutor(UiExecutor.getDefault())
                .register(connectionEvt);

        if (batteryServiceSupport()) {
            mBattery = targetDevice.requireProfile(BatteryService.class);
            if (mBattery != null) {
                mBattery.evtUpdate()
                        .subEvent(this)
                        .setDisposer(deviceEventDisposer)
                        .setExecutor(UiExecutor.getDefault())
                        .register((IEventListener<Integer>) (src, type, percent) -> setBatteryValue(percent));
            }
        }

        TaskQueue setupSteps = targetDevice.getGatt().getSetupSteps();
        setupSteps.clearTask(); // 防止重复添加初始化步骤
        setupSteps.addTask(targetDevice.getGatt().discoverServices());
        onBindTo(targetDevice, setupSteps, deviceEventDisposer);

        // 设备没有处于断开状态
        if (!targetDevice.getGatt().isDisconnected()) {
            // 显示断开按钮
            if (connectBtn != null) {
                connectBtn.setText(R.string.common_disconnect);
            }
            // 初始化profile中需要的服务
            setupSteps.evtFinished()
                    .subEvent(this, true).setDisposer(deviceEventDisposer).setExecutor(UiExecutor.getDefault()).register2((src, evtType, result) -> {
                if (result.getError() == null) {
                    onDeviceReady();
                } else {
                    onDeviceNotSupported();
                    Context ctx = getContext();
                    if (ctx != null) {
                        ToastUtil.dialog(ctx, ctx.getString(R.string.libuihelper_err_msg, result.getError().getRawMessage())).show();
                    }
                }
            });
            new TaskBusyDialog()
                    .setHost(this)
                    .bind(setupSteps)
                    .setOneshot()
                    .setTitle(getString(R.string.common_finding_service))
                    .startTask();
        }

        // 在切换设备时，立即更新设备名称的显示
        TextView deviceNameTv = this.deviceNameTv;
        if (deviceNameTv != null) {
            deviceNameTv.setText(getDeviceName());
        }
    }


    protected final void setUpView() {
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            int strResId = getToolBarTitle();
//            if (strResId != 0) {
//                actionBar.setTitle(strResId);
//            } else {
//                actionBar.setTitle(getClass().getSimpleName());
//            }
//        }

        connStateDialog = new ConnStateDialog(requireContext());
        connStateDialog.getReconnectBtn().setOnClickListener(v -> connectDevice());
        connStateDialog.getCancelBtn().setOnClickListener(v -> {
            if (targetDevice != null && !targetDevice.getGatt().isDisconnected()) {
                TaskQueue setupSteps = targetDevice.getGatt().getSetupSteps();
                if (setupSteps.isStarted()) {
                    setupSteps.abort();
                }
                targetDevice.getGatt().disconnect(true).startProcedure();
            } else {
                connStateDialog.close();
            }
        });

        connectBtn = findViewById(R.id.profile_connect_button);
        deviceNameTv = findViewById(R.id.profile_device_name);
        if (deviceNameTv != null) {
            deviceNameTv.setText(R.string.common_no_device);
        }

        batteryTv = findViewById(R.id.txt_battery_value);
        batteryIv = findViewById(R.id.img_battery);
        View batteryLabelTv = findViewById(R.id.bat_txt);
        if (batteryServiceSupport()) {
            batteryIv.setVisibility(View.VISIBLE);
            batteryTv.setVisibility(View.VISIBLE);
            if (batteryLabelTv != null) batteryLabelTv.setVisibility(View.VISIBLE);
            AppUtils.setImageViewColor(batteryIv, ContextCompat.getColor(appCtx, R.color.textColorLight));
        } else {
            if (batteryIv != null) batteryIv.setVisibility(View.INVISIBLE);
            if (batteryTv != null) batteryTv.setVisibility(View.INVISIBLE);
            if (batteryLabelTv != null) batteryLabelTv.setVisibility(View.INVISIBLE);
        }

        connectBtn.setOnClickListener(new ClickDebounceHelper(v -> {
            if (targetDevice != null && targetDevice.getGatt().isConnected()) {
                disconnectDevice();
            } else {
                setDefaultUI();
                showDeviceScanningDialog(getFilterUUID());
            }
        }, 1000));

        connectBtn.setOnLongClickListener(v -> {
            if (targetDevice != null && targetDevice.getGatt().isConnected()) {
                return false;
            } else {
                setDefaultUI();
                if (bleScannerFragment == null) {
                    bleScannerFragment = new BleScannerFragment()
                            .setCfg(new BleScannerFragment.Cfg().addUuid(getFilterUUID()));
                }
                bleScannerFragment.show(getChildFragmentManager(), "ble_scan_fragment");
            }
            return true;
        });

        contentScroll = findViewById(R.id.scroll_view);
        if (contentScroll != null) {
            contentScroll.setScrollViewListener(this::handleScrollChanged);
        }
    }

    protected void handleScrollChanged(View scrollView, int x, int y, int oldx, int oldy) {
        handleScrollChanged(scrollView, x - oldx, y - oldy);
    }

    protected void handleScrollChanged(View scrollView, int dx, int dy) {
        scrollCount++;
        if (scrollCount > 0) {
            // dy > 0 表示上滑，显示更多内容
            if (dy > 15) {
                showConnectBtn(false);
            } else if (dy < -15) {
                showConnectBtn(true);
            }
        }
    }

    protected void showConnectBtn(boolean show) {
        if (show) {
            if (!mScanButtonShowFlag) {
                connectBtn.animate().translationY(0);
            }
            mScanButtonShowFlag = true;
        } else {
            //将Y属性变为底部栏高度  (相当于隐藏了)
            if (mScanButtonShowFlag) {
                connectBtn.animate().translationY(150);
            }
            mScanButtonShowFlag = false;
        }
    }

    protected String getDeviceName() {
        return targetDevice != null ? targetDevice.getGatt().getName() : getString(R.string.libuihelper_na);
    }

    /**
     * Called after {@link #onInitialize(Bundle)}, and before {@link #onViewCreated(Bundle)}.
     * Implement this method to create views which are displayed in the framework.
     * <p>
     * Initialize {@link #fragmentConfig} in constructor when needed.
     * <p>
     * Override these methods when needed:
     * <ol>
     *     <li>{@link #setDefaultUI()}</li>
     *     <li>{@link #onOptionsItemSelected(Menu, MenuItem, int)}</li>
     * </ol>
     *
     * @param inflater           LayoutInflater from fragment
     * @param root               the root view of this framework
     * @param contentContainer   container for user's views
     * @param savedInstanceState from fragment
     */
    protected abstract void onCreateContentView(LayoutInflater inflater, View root, FrameLayout contentContainer, final Bundle savedInstanceState);

    @StringRes
    protected int getToolBarTitle() {
        return fragmentConfig.toolBarTitle;
    }

    /**
     * Specify Menu
     *
     * @return Menu resource ID. 0 for default.
     */
    @MenuRes
    protected int getMenuResId() {
        return fragmentConfig.menu;
    }

    /**
     * Specify the content in about dialog
     *
     * @return String resource ID. 0 for hiding.
     */
    @StringRes
    protected int onGetAboutInfo() {
        return fragmentConfig.abortInfo;
    }

    protected UUID getFilterUUID() {
        return fragmentConfig.filterUUID;
    }

    /**
     * Restores the default UI before reconnecting
     */
    protected void setDefaultUI() {

    }

    /**
     * see {@link #onOptionsItemSelected(MenuItem)}
     *
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    protected boolean onOptionsItemSelected(Menu menu, MenuItem item, final int itemId) {
        return false;
    }

    protected boolean batteryServiceSupport() {
        return fragmentConfig.supportBatteryService;
    }


    @SuppressLint("SetTextI18n")
    protected void setBatteryValue(final int percent) {
        final int index = (percent / 11);
        final int[] imgid = {R.mipmap.ic_battery_10, R.mipmap.ic_battery_20, R.mipmap.ic_battery_30, R.mipmap.ic_battery_40,
                R.mipmap.ic_battery_50, R.mipmap.ic_battery_60, R.mipmap.ic_battery_70, R.mipmap.ic_battery_80,
                R.mipmap.ic_battery_90, R.mipmap.ic_battery_100};

        batteryIv.setImageResource(imgid[index]);
        AppUtils.setImageSource(batteryIv, imgid[index], appCtx);
        batteryTv.setText(percent + "%");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLERequest.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (pendingConnectionRequestForEnableBt) {
                    pendingConnectionRequestForEnableBt = false;
                    connectDevice();
                }
            }
        }
    }

    public static class Config {
        public boolean supportBatteryService = false;
        public UUID filterUUID = null;

        @StringRes
        public int toolBarTitle = 0;
        @StringRes
        public int abortInfo = 0;
        @MenuRes
        public int menu = 0;
    }
}
