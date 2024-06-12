package com.example.myapplication1;

import android.Manifest;
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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication1.scan.BLERequest;
import com.example.myapplication1.scan.BLEScanCallback;
import com.example.myapplication1.scan.BLEScanner;
import com.example.myapplication1.scan.BLEScannerFactory;
import com.example.myapplication1.ui.WaveView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements BLEScanCallback {
    public static final String SHARE_INFO_AUTH_ENABLE = "SHARE_INFO_AUTH_ENABLE";
    public static final String INFO_TYPE = "INFO_TYPE";
    public static final String ENC_DATA_INFO = "ENC_DATA_INFO";

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int MY_REQUEST_ENABLE_BT = 2;
    private BLEScanner bleScanner;
    private WaveView wave;
    private ListView mListView;
    private ArrayList<ScanBluetoothDevice> mScanDeviceList;
    private DeviceListAdapter mAdapter;
    private ArrayList<BluetoothDevice> scanDevice;
    private TextView titleTv;
    private boolean mIsScanning = false;
    private ArrayList<BondDeviceInfo> bondDevice;
    private ArrayList<BondDeviceInfo> saveDevice;
    private String app_id;
    private String filterDeviceName = "Com";
    private byte[] secureData;
    private boolean auth;
    private int type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bleScanner = BLEScannerFactory.getScanner();
        checkBluetoothEnable();
        scanDevice = new ArrayList<>();
        mListView = findViewById(R.id.listview);
        mScanDeviceList = new ArrayList<>();
        bondDevice = new ArrayList<>();
        saveDevice = new ArrayList<>();
        mAdapter = new DeviceListAdapter(this, mScanDeviceList);
        mListView.setAdapter(mAdapter);
        wave = findViewById(R.id.wave);
        titleTv = findViewById(R.id.scanner_txt_title);
        Button button = findViewById(R.id.scan_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanDevice();  //扫描设备
            }
        });
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            bleScanner.stopScan();

            DfuLog.i(mScanDeviceList.get(position).name);
            Intent intent = new Intent();
            intent.setClass(this, ConnectActivity.class);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, scanDevice.get(position));
            intent.putExtra(BluetoothDevice.EXTRA_NAME, mScanDeviceList.get(position).name);
            intent.putExtra(SHARE_INFO_AUTH_ENABLE, auth);
            if(auth) {
                intent.putExtra(INFO_TYPE, type);
                intent.putExtra(ENC_DATA_INFO, secureData);
            }
            wave.clearWave();
            wave.stop();
            titleTv.setText("选择设备");
            mScanDeviceList.clear();
            scanDevice.clear();

            startActivity(intent);
            finish();
        });
        //需要判断进入条件
        Intent intent = getIntent();
        auth = intent.getBooleanExtra(SHARE_INFO_AUTH_ENABLE, false);
        String deviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
        filterDeviceName += deviceName;
        DfuLog.i(filterDeviceName+"  "+auth);
        if(auth) {
            type = intent.getIntExtra(INFO_TYPE,0x02);
            secureData = intent.getByteArrayExtra(ENC_DATA_INFO);
            DfuLog.i("type = "+type+" "+HexUtil.encodeHexStr(secureData));
            scanDevice();
        }else {
            getBondDevice();
        }
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

    private void getBondDevice() {
        //需要双向对比，APP卸载了，但是设备已经绑定
        //设备被取消绑定了
        //跟登录名绑定，需要短信登录
        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        app_id = sharedPreferenceUtil.getAppID();
        DfuLog.i(app_id);

        int bondDeviceNum = sharedPreferenceUtil.getBondDeviceCount();
        if(bondDeviceNum == 0) {

        } else {
            for(int i=0; i<bondDeviceNum; i++) {
                BondDeviceInfo deviceInfo = new BondDeviceInfo();
                deviceInfo.setName(sharedPreferenceUtil.getBondDeviceName());
                deviceInfo.setAddr(sharedPreferenceUtil.getBondDeviceAddr());
                saveDevice.add(deviceInfo);
            }
        }

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

        //获取已绑定设备的信息
        for(BluetoothDevice device : devices) {

            if(device.getName().contains("Ebike")) {
                boolean isConnected = false;
                BondDeviceInfo bondDeviceInfo = new BondDeviceInfo();
                bondDeviceInfo.setAddr(device.getAddress());
                bondDeviceInfo.setName(device.getName());
                try {
                    //使用反射调用获取设备连接状态方法
                    Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                    isConnectedMethod.setAccessible(true);
                    isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
                    bondDeviceInfo.setConnected(isConnected);
                    DfuLog.i("isConnected：" + isConnected);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                bondDevice.add(bondDeviceInfo);

                //需要判断下APP是否存储了设备，直接进入到bondDeviceActivity界面
                Intent intent = new Intent();
                intent.setClass(this, BondDeviceActivity.class);
                intent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, true);
                intent.putExtra(BondDeviceActivity.CONNECT_STATE,isConnected);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                intent.putExtra(SHARE_INFO_AUTH_ENABLE, false);
                startActivity(intent);
                finish();
            }
            Log.i("MainActivity", device.getAddress() +"  "+device.getName());
        }
        if(bondDevice.size() == 0) {
            scanDevice();
        }

        //当前手机已经绑定设备，存储了，获取到了存储设备，但是手机端没有绑定设备，可能是在设置界面进行了解除绑定，需要提示用户重新配对
//        if(bondDeviceNum != 0 && bondDevice.size() == 0) {
//
//        }else () { //手机端有绑定设备，但是APP没存储有，这种情况应该是不存在的，需要重置设备
//
//        }else if() //有多个设备

//        for(int i=0; i<bondDevice.size(); i++) {
//            mScanDeviceList.add(new ScanBluetoothDevice(bondDevice.get(i), 0));
//            scanDevice.add(bondDevice.get(i));
//        }
//        mAdapter.notifyDataSetChanged();
    }

    private void scanDevice() {
        if (!BLERequest.isBLEEnabled(this)) {
            BLERequest.showBLEDialog(this);
        } else {
            if(mIsScanning) {
                bleScanner.stopScan();
                mIsScanning = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleTv.setText("选择设备");
                    }
                });
                wave.clearWave();
                wave.stop();
            } else {
                wave.start();
                mIsScanning  =true;
                titleTv.setText("正在扫描设备");
                mScanDeviceList.clear();
                scanDevice.clear();
//                for(int i=0; i<bondDevice.size(); i++) {
//                    mScanDeviceList.add(new ScanBluetoothDevice(bondDevice.get(i), 0));
//                    scanDevice.add(bondDevice.get(i));
//                }
                mAdapter.notifyDataSetChanged();
                bleScanner.searchForName(filterDeviceName, this);
            }
        }
    }

    @Override
    public void onScanTimeOut(boolean found) {
        DfuLog.i("onScanTimeOut" + found);
        mIsScanning = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                titleTv.setText("选择设备");
            }
        });
        wave.clearWave();
        wave.stop();
    }

    @Override
    public void onScanResult(BluetoothDevice device, int rssi) {
        DfuLog.i("onScanResult" + rssi);
        if (!scanDevice.contains(device)) {
            scanDevice.add(device);
            mScanDeviceList.add(new ScanBluetoothDevice(device, rssi));
            mAdapter.notifyDataSetChanged();
        } else {
            int index = scanDevice.indexOf(device);
            int rssiBefore = mScanDeviceList.get(index).rssi;
            if(Math.abs(rssiBefore - rssi) > 5){
                mScanDeviceList.get(index).updateDevice(rssi);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onScanError() {

    }

    /*---------------------------------------------------------------------------------------------------------*/
    /*---------------------------------------------------------------------------------------------------------*/
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
            Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
            intent.setData(uri);
            MainActivity.this.startActivityForResult(intent, 4);
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
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MY_REQUEST_ENABLE_BT);
        } else {
            status = true;
        }
        return status;
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