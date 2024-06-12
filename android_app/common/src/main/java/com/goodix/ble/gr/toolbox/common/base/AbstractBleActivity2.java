package com.goodix.ble.gr.toolbox.common.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

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
import com.goodix.ble.libuihelper.ble.scanner.BleScannerFragment;
import com.goodix.ble.libuihelper.ble.scanner.IDeviceItem;
import com.goodix.ble.libuihelper.logger.Log;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.util.UUID;
import java.util.concurrent.Executor;

public abstract class AbstractBleActivity2 extends BaseActivity implements ScannerFragment.OnDeviceSelectedListener, BleScannerFragment.CB {

    private String mDeviceName;
    private TextView deviceNameTv;
    private Button connectBtn;

    @Nullable
    ConnStateDialog connStateDialog;

    private ImageView batteryIv;
    private TextView batteryTv;

    private boolean doubleExit = true;
    private long lastBackClickTime = 0;
    private boolean isBackFlag = false; // 判断是否要显示重连对话框

    protected MyScrollView contentScroll;
    protected int scrollCount = 0;
    private boolean pendingConnectionRequestForEnableBt = false;
    private boolean mScanButtonShowFlag = false;

    private BleItem targetDevice = null;
    final protected EventDisposer deviceDisposer = new EventDisposer();
    final protected Executor uiExecutor = UiExecutor.getDefault();

    protected Menu mMenu = null;

    @Nullable
    protected BatteryService mBattery;

    private IEventListener connectionEvt = (src, type, evtData) -> {
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
            case GBRemoteDevice.EVT_READ:
                Boolean isReady = (Boolean) evtData;
                if (isReady) {
                    onDeviceReady();
                } else {
                    onDeviceNotSupported();
                }
                break;
        }
    };


    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BleCenter.setContext(getApplicationContext());
        BleCenter.setRootLogger(Log.getLogger());
        if (!BLERequest.isBLEEnabled(this)) {
            BLERequest.showBLEDialog(this);
        }

        setContentView(R.layout.activity_base_profile);

        // In onInitialize method a final class may register local broadcast receivers that will listen for events from the service
        onInitialize(savedInstanceState);

        // The onCreateContentView class should... create the view
        onCreateContentView(savedInstanceState);
        setUpView();

        // View is ready to be used
        onViewCreated(savedInstanceState);
    }

    protected void onInitialize(final Bundle savedInstanceState) {
        // empty default implementation
    }

    protected void onViewCreated(final Bundle savedInstanceState) {
        // empty default implementation
    }

    protected abstract void onBindTo(BleItem targetDevice, TaskQueue setupSteps, EventDisposer disposer);

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        mMenu = menu;

        boolean hasMenu = false;
        int resId = getMenuResId();

        if (resId != 0) {
            getMenuInflater().inflate(getMenuResId(), menu);
            hasMenu = true;
        }

        if (onGetAboutInfo() != 0) {
            getMenuInflater().inflate(R.menu.default_menu, menu);
            hasMenu = true;
        }

        return hasMenu;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!onOptionsItemSelected(id)) {
                super.onBackPressed();
            }
        } else if (id == R.id.about_dj) {
            AboutAlert.showAboutAlert(this, onGetAboutInfo());
        } else {
            return onOptionsItemSelected(id);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (doubleExit) {
            long now = System.currentTimeMillis();
            if (now - lastBackClickTime > 2000) {
                lastBackClickTime = now;
                ToastUtil.info(this, R.string.common_exit_tip).show();
                return;
            }
        }

        isBackFlag = true;
        disconnectDevice();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isBackFlag) {
            disconnectDevice();
        }
        if (targetDevice != null) {
            targetDevice.getGatt().clearEventListener(this);
        }
        setTargetDevice(null);
    }

    public void onDeviceConnecting() {
        deviceNameTv.setText(mDeviceName != null ? mDeviceName : getString(R.string.common_not_available));
        connectBtn.setText(R.string.common_connecting);
        if (connStateDialog != null) {
            connStateDialog.showConnecting();
        }
    }

    public void onDeviceConnected() {
        if (batteryServiceSupport()) {
            AppUtils.setImageViewColor(batteryIv, this);
        }
        if (connStateDialog != null) {
            connStateDialog.showDiscoveringService();
        }
        connectBtn.setText(R.string.common_disconnect);
        ToastUtil.success(this, R.string.common_connected).show();
    }

    public void onDeviceDisconnecting() {
        connectBtn.setText(R.string.common_disconnecting);
    }

    public void onDeviceDisconnected() {
        if (batteryServiceSupport()) {
            AppUtils.setImageViewColor(batteryIv, ContextCompat.getColor(this, R.color.textColorLight));
        }
        if (!isBackFlag) {
            showDeviceDisconnected();
        }
        connectBtn.setText(R.string.common_connect);
        deviceNameTv.setText(R.string.common_no_device);
    }

    public void onDeviceReady() {
        if (connStateDialog != null) {
            connStateDialog.close();
        }
        ToastUtil.success(this, R.string.common_ready).show();
    }

    public void onError(String message, int errorCode) {
        ToastUtil.error(this, message + " (" + errorCode + ")").show();
    }

    public void onDeviceNotSupported() {
        ToastUtil.error(this, R.string.common_not_support).show();
        if (connStateDialog != null) {
            connStateDialog.showNotSupported();
        }
        if (targetDevice != null) {
            targetDevice.getGatt().disconnect(true).startProcedure();
        }
    }

    @Override
    public void onDeviceSelected(ExtendedBluetoothDevice device, String name, byte[] scanByte) {
        mDeviceName = name;

        setTargetDevice(device.macAddr);

        // 发起连接
        connectDevice();
    }

    @Override
    public void onDialogCanceled() {
        //do nothing
    }

    @Override
    public void onDeviceSelected(@Nullable IDeviceItem device) {
        if (device != null) {
            mDeviceName = device.getName();

            setTargetDevice(device.getDevice().getAddress());

            // 发起连接
            connectDevice();
        }
    }

    protected void setDoubleExit(boolean doubleExit) {
        this.doubleExit = doubleExit;
    }

    private void connectDevice() {
        if (!BLERequest.isBLEEnabled(this)) {
            pendingConnectionRequestForEnableBt = (targetDevice != null);
            BLERequest.showBLEDialog(this);
            return;
        }
        if (targetDevice != null) {
            targetDevice.getGatt().connect(0).setRetry(3, 3000).startProcedure();
        }
    }

    /**
     * 留给子类重新定义按钮点击断开时的动作
     */
    protected void disconnectDevice() {
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

    protected void setContentViewInScroll(int resId) {
        FrameLayout fl = findViewById(R.id.scroll_content_view_layout);
        getLayoutInflater().inflate(resId, fl, true);
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
        dialog.show(getSupportFragmentManager(), "scan_fragment");
    }

    @Nullable
    protected BleItem getTargetDevice() {
        return targetDevice;
    }

    protected boolean isConnected() {
        return targetDevice != null && targetDevice.getGatt().isConnected();
    }

    protected boolean isDisconnected() {
        return targetDevice == null || targetDevice.getGatt().isDisconnected();
    }

    protected boolean isReady() {
        return targetDevice != null && targetDevice.getGatt().isReady();
    }

    protected void setTargetDevice(String address) {
        // 解除前一个设备的绑定，断开设备
        if (targetDevice != null) {
            // 选择了相同的设备，跳过
            if (targetDevice.getGatt().getAddress().equals(address)) {
                return;
            }
            // 移除引用
            targetDevice.release();
            targetDevice = null;
        }
        deviceDisposer.disposeAll(this);

        if (address == null) {
            return;
        }

        try {
            targetDevice = BleCenter.get().addDevice(address);
        } catch (Exception e) {
            return;
        }
        targetDevice.retain();

        // 绑定必要的事件
        targetDevice.getGatt().evtStateChanged()
                .subEvent(this)
                .setDisposer(deviceDisposer)
                .setExecutor(UiExecutor.getDefault())
                .register(connectionEvt);

        targetDevice.getGatt().evtError()
                .subEvent(this)
                .setDisposer(deviceDisposer)
                .setExecutor(UiExecutor.getDefault())
                .register(connectionEvt);

        targetDevice.getGatt().evtReady()
                .subEvent(this)
                .setDisposer(deviceDisposer)
                .setExecutor(UiExecutor.getDefault())
                .register(connectionEvt);

        if (batteryServiceSupport()) {
            if (mBattery == null) {
                mBattery = new BatteryService();
                mBattery.evtUpdate()
                        .subEvent(this)
                        .setDisposer(deviceDisposer)
                        .setExecutor(UiExecutor.getDefault())
                        .register((IEventListener<Integer>) (src, type, percent) -> setBatteryValue(percent));
            }
            mBattery.bindTo(targetDevice.getGatt());
        }

        TaskQueue setupSteps = targetDevice.getGatt().getSetupSteps();
        setupSteps.clearTask(); // 防止重复添加初始化步骤
        setupSteps.addTask(targetDevice.getGatt().discoverServices());
        onBindTo(targetDevice, setupSteps, deviceDisposer);
    }


    protected final void setUpView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            int strResId = getToolBarTitle();
            if (strResId != 0) {
                actionBar.setTitle(strResId);
            } else {
                actionBar.setTitle(getClass().getSimpleName());
            }
        }

        connStateDialog = new ConnStateDialog(this);
        connStateDialog.getReconnectBtn().setOnClickListener(v -> connectDevice());

        connectBtn = findViewById(R.id.profile_connect_button);
        deviceNameTv = findViewById(R.id.profile_device_name);
        deviceNameTv.setText(R.string.common_no_device);

        batteryTv = findViewById(R.id.txt_battery_value);
        batteryIv = findViewById(R.id.img_battery);
        View batteryLabelTv = findViewById(R.id.bat_txt);
        if (batteryServiceSupport()) {
            batteryIv.setVisibility(View.VISIBLE);
            batteryTv.setVisibility(View.VISIBLE);
            batteryLabelTv.setVisibility(View.VISIBLE);
            AppUtils.setImageViewColor(batteryIv, ContextCompat.getColor(this, R.color.textColorLight));
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
                new BleScannerFragment()
                        .setCfg(new BleScannerFragment.Cfg().addUuid(getFilterUUID()))
                        .show(getSupportFragmentManager(), "ble_scan_fragment");
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
        return mDeviceName;
    }


    protected abstract void onCreateContentView(final Bundle savedInstanceState);

    @StringRes
    protected abstract int getToolBarTitle();

    /**
     * Specify Menu
     *
     * @return Menu resource ID. 0 for default.
     */
    @MenuRes
    protected abstract int getMenuResId();

    /**
     * Specify the content in about dialog
     *
     * @return String resource ID. 0 for hiding.
     */
    @StringRes
    protected abstract int onGetAboutInfo();

    protected abstract UUID getFilterUUID();

    /**
     * Restores the default UI before reconnecting
     */
    protected abstract void setDefaultUI();

    /**
     * see {@link Activity#onOptionsItemSelected(MenuItem)}
     *
     * @param itemId The ID of menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     */
    protected abstract boolean onOptionsItemSelected(final int itemId);

    protected abstract boolean batteryServiceSupport();


    @SuppressLint("SetTextI18n")
    protected void setBatteryValue(final int percent) {
        final int index = (percent / 11);
        final int[] imgid = {R.mipmap.ic_battery_10, R.mipmap.ic_battery_20, R.mipmap.ic_battery_30, R.mipmap.ic_battery_40,
                R.mipmap.ic_battery_50, R.mipmap.ic_battery_60, R.mipmap.ic_battery_70, R.mipmap.ic_battery_80,
                R.mipmap.ic_battery_90, R.mipmap.ic_battery_100};

        batteryIv.setImageResource(imgid[index]);
        AppUtils.setImageSource(batteryIv, imgid[index], this);
        batteryTv.setText(percent + "%");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
}
