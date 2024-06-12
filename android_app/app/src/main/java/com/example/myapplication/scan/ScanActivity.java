package com.example.myapplication.scan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import com.example.myapplication.ConnectActivity;
import com.example.myapplication.DfuLog;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.UUID;

public class ScanActivity extends AppCompatActivity implements BLEScanCallback {
    private final static UUID DFU_SERVICE_UUID = UUID.fromString("a6ed0401-d344-460a-8075-b9e8ec90d71b");
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int MY_REQUEST_ENABLE_BT = 2;
    private BLEScanner bleScanner;
    private SwipeRefreshLayout mSwipeRefreshLayout;  //下拉刷新控件
    private ListView mListView;
    private ArrayList<ScanBluetoothDevice> mScanDeviceList;
    private DeviceListAdapter mAdapter;
    private ArrayList<BluetoothDevice> scanDevice;
    private LinearLayout noDeviceLayout;
    private MenuItem filterSelection;
    private boolean filterDfuOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        bleScanner = BLEScannerFactory.getScanner();
        checkBluetoothEnable();
        scanDevice = new ArrayList<>();
        initView();
        scanDevice();
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkBluetoothEnable()) {//先检测是否开启蓝牙
            if (checkAPIVersion()) {// if android 6.0 up
                requestLocationAccess();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        filterSelection = menu.add("不过滤");
        filterSelection.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        filterDfuOnly = true;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (filterSelection == item) {
            filterDfuOnly = !filterDfuOnly;
            filterSelection.setTitle(filterDfuOnly ? "不过滤" : "仅DFU");
            bleScanner.stopScan();
            mSwipeRefreshLayout.setRefreshing(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!checkAPIVersion()) return;

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showRequestPermissionAlert(getResources().getString(R.string.main_permission_location_alert));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, MY_REQUEST_ENABLE_BT);
            } else {
                if (checkAPIVersion()) {// if android 6.0 up
                    requestLocationAccess();
                }
            }
        }
    }


    @SuppressLint("ResourceAsColor")
    private void initView() {
        noDeviceLayout = findViewById(R.id.no_devices);
        mSwipeRefreshLayout = findViewById(R.id.id_swipe_ly);
        mSwipeRefreshLayout.setColorSchemeColors(R.color.colorPrimaryBlue);
        mListView = findViewById(R.id.list_view);
        mScanDeviceList = new ArrayList<>();
        mAdapter = new DeviceListAdapter(this, mScanDeviceList);
        mListView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setOnRefreshListener(this::scanDevice);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            bleScanner.stopScan();
            mSwipeRefreshLayout.setRefreshing(false);
            Intent intent = new Intent();
            intent.setClass(this, ConnectActivity.class);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, scanDevice.get(position));
            intent.putExtra(BluetoothDevice.EXTRA_NAME, mScanDeviceList.get(position).name);
            startActivity(intent);
            DfuLog.i(mScanDeviceList.get(position).name);
        });
    }

    private void scanDevice() {
        if (!BLERequest.isBLEEnabled(this)) {
            mSwipeRefreshLayout.setRefreshing(false);
            BLERequest.showBLEDialog(this);
        } else {
            mScanDeviceList.clear();
            scanDevice.clear();
            noDeviceLayout.setVisibility(View.VISIBLE);
            mAdapter.notifyDataSetChanged();
            if (filterDfuOnly) {
                Intent intent = getIntent();
                if (intent != null && intent.hasExtra("uuid")) {
                    bleScanner.searchForServiceUUID(intent.getParcelableExtra("uuid"), ScanActivity.this);
                } else {
                    bleScanner.searchForServiceUUID(new ParcelUuid(DFU_SERVICE_UUID), ScanActivity.this);
                }
            } else {
                bleScanner.searchAll(this);
            }
        }
    }


    /**
     * 请求获取定位权限（android5.0以上蓝牙使用需要此权限）
     */
    private void requestLocationAccess() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    /**
     * 检测SDK版本是否是Android 5.0以上
     *
     * @return 结果
     */
    private boolean checkAPIVersion() {
        int version = Build.VERSION.SDK_INT;
        return version >= 23;
    }


    /**
     * 显示拒绝获取权限后的对话框，提示应用需要获取权限才能运行
     *
     * @param strInfo--显示的对话框的title
     */
    private void showRequestPermissionAlert(String strInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.main_permission_title);
        builder.setMessage(strInfo);
        builder.setPositiveButton(R.string.common_sure, (dialogInterface, i) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", ScanActivity.this.getPackageName(), null);
            intent.setData(uri);
            ScanActivity.this.startActivityForResult(intent, 4);
        });

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * 检测蓝牙是否开启
     *
     * @return 蓝牙开启状态
     */
    private boolean checkBluetoothEnable() {
        boolean status = false;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
//        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MY_REQUEST_ENABLE_BT);
        } else {
            status = true;
        }
        return status;
    }

    @Override
    public void onScanTimeOut(boolean found) {
        runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(false));
    }

    @Override
    public void onScanResult(BluetoothDevice device, int rssi) {
        if (!filterDfuOnly && rssi < -65) {
            return;
        }
        noDeviceLayout.setVisibility(View.INVISIBLE);
        if (!scanDevice.contains(device)) {
            scanDevice.add(device);
            mScanDeviceList.add(new ScanBluetoothDevice(device, rssi));
        } else {
            int index = scanDevice.indexOf(device);
            mScanDeviceList.get(index).updateDevice(rssi);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onScanError() {
    }

    static {
        // Load native library ECDH-Curve25519-Mobile implementing Diffie-Hellman key
        // exchange with elliptic curve 25519.
        try {
            System.loadLibrary("ecdhcurve25519");
            Log.i("ScanActivity", "Loaded ecdhcurve25519 library.");
        } catch (UnsatisfiedLinkError e) {
            Log.e("ScanActivity", "Error loading ecdhcurve25519 library: " + e.getMessage());
        }
    }

}