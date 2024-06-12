package com.example.myapplication1;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.king.zxing.CaptureActivity;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    //在这个界面检测是否有唯一ID，如果没有唯一ID，需要进行设置，绑定设备会跟唯一ID进行关联，目的是为了不同手机登录，可以检测到当前用户已经绑定的设备是哪些
    //需要检测
    private String app_id;
    private String deviceName;
    private String deviceAddr;
    private byte[] shareKey;
    private EditText editText;
    private ImageView emptyImg;

    final static int COUNTS = 10;// 点击次数
    final static long DURATION = 2000;// 规定有效时间
    long[] mHits = new long[COUNTS];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        app_id = sharedPreferenceUtil.getAppID();
        deviceName = sharedPreferenceUtil.getBondDeviceName();
        deviceAddr = sharedPreferenceUtil.getBondDeviceAddr();
        if(deviceName == null) {
            deviceName = "123456";
        } else {
            deviceName = deviceName.substring(5,11);
        }
        shareKey = sharedPreferenceUtil.getShareKey();
        DfuLog.i(app_id+" "+deviceName+" "+deviceAddr+" "+HexUtil.encodeHexStr(shareKey));
        TextView idText = findViewById(R.id.txt_id);
        idText.setText("用户ID:"+app_id);
        TextView addrText = findViewById(R.id.txtAddr);
        //addrText.setText(getBluetoothAddress());
        Button inButton = findViewById(R.id.sureButton);
        Button shortOpenButton = findViewById(R.id.ShortOpenButton);
        inButton.setOnClickListener(this::onClick);
        shortOpenButton.setOnClickListener(this::onClick);
        emptyImg = (ImageView) findViewById(R.id.emptyImg);

        emptyImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                continuousClick(COUNTS, DURATION);
                DfuLog.i("emptyImg click");
            }
        });
    }

    public static String getBluetoothAddress() {
        //区分版本
        if (Build.VERSION.SDK_INT>=22){
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:",b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            } catch (Exception ex) {
            }
            return "";
        }else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            String macAddr = adapter.getAddress();
            return macAddr;
        }
    }

    private void continuousClick(int count, long time) {
        //每次点击时，数组向前移动一位
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        //为数组最后一位赋值
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
            mHits = new long[COUNTS];//重新初始化数组
            showDialog();
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.ShortOpenButton){
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivityForResult(intent, 1);
        } else {
            //showDialog();
            Intent intent = new Intent();
            intent.setClass(LoginActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.SHARE_INFO_AUTH_ENABLE, false);
            intent.putExtra(BluetoothDevice.EXTRA_NAME, deviceName);
            startActivity(intent);
            finish();
        }

    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重置");
        builder.setMessage("是否要重置APP");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(LoginActivity.this);
                sharedPreferenceUtil.clearInfo();
                final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                //获取已绑定设备的信息
                for(BluetoothDevice device : devices) {
                    if(device.getName().contains("Ebike")) {
                        unpairDevice(device);
                    }
                }

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // 3s后会执行的操作
                        final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                         startActivity(intent);
                         //杀掉以前进程
                         android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }.start();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    //通过反射来调用BluetoothDevice.removeBond取消设备的配对
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.setAccessible(true);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            DfuLog.e("ble",e.toString());
        }
    }

    private final byte[] data = {(byte) 45,98,(byte)214,22,(byte)193,31,(byte)190,(byte)184,(byte)209,(byte)136,
            116,6,(byte)253,(byte)198,11,11,(byte)11,34,62,47,41,(byte)224,(byte)225,57,(byte)172,(byte)189,
            (byte)205,(byte)208,52,13,14,15,16,78,12,13,56,54,76,78,55,56};

    private  void jdkECDSA() {
        try {
            byte[] signedData = ECDSAUtil.signECDSA(data);
            boolean verify = ECDSAUtil.verifyECDSA(signedData, data);
            DfuLog.i(verify + "  " +signedData.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int bytesToInt(byte[] a){
        int ans=0;
        for(int i=0;i<4;i++){
            ans<<=8;//左移 8 位
            ans|=(a[3-i]&0xff);//保存 byte 值到 ans 的最低 8 位上
        }
        return ans;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                    DfuLog.i("onActivityResult");
                    if(data !=null){
                        String result = data.getStringExtra("SCAN_RESULT");
                        DfuLog.i(result.length() + "---"+ result);
                        String enData1 = result.substring(0,32);
                        DfuLog.i(enData1);
                        byte[] enData1Bytes = HexUtil.decodeHex(enData1.toCharArray());
                        DfuLog.i(HexUtil.encodeHexStr(enData1Bytes));
                        byte[] unenData1Bytes = AES256ECB.decrypt(enData1Bytes,BondDeviceActivity.PRIVATE_KEY);
                        DfuLog.i(HexUtil.encodeHexStr(unenData1Bytes));

                        byte[] enData2 = HexUtil.decodeHex(result.substring(32,96).toCharArray());
                        DfuLog.i(HexUtil.encodeHexStr(enData2));

                        //------------------------------------------------------------------------
                        byte[] intBytes = new byte[4];
                        intBytes[0] = unenData1Bytes[2];
                        intBytes[1] = unenData1Bytes[3];
                        intBytes[2] = unenData1Bytes[4];
                        intBytes[3] = unenData1Bytes[5];

                        long time =  bytesToInt(intBytes);
                        long  currentTime = System.currentTimeMillis()/1000;
                        DfuLog.i(time+" -- "+currentTime);
                        long diff = currentTime - time;
                        if(diff > (600)) {
                            Toast toast = Toast.makeText(LoginActivity.this,"分享信息已过期",Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);//竖直居中
                            toast.show();
                        }else {
                            char[] nameBytes = new char[6];
                            for(int i=0; i<6; i++){
                                nameBytes[i] = (char) unenData1Bytes[6+i];
                            }

                            deviceName = String.valueOf(nameBytes);
                            DfuLog.i("name = "+deviceName);

                            Intent intent = new Intent();
                            intent.setClass(LoginActivity.this, MainActivity.class);
                            intent.putExtra(MainActivity.SHARE_INFO_AUTH_ENABLE, true);
                            intent.putExtra(BluetoothDevice.EXTRA_NAME, deviceName);
                            if(unenData1Bytes[0] == 0x02 && unenData1Bytes[1] == 0x02 ) {//short
                                intent.putExtra(MainActivity.INFO_TYPE, 0x02);
                            }else if(unenData1Bytes[0] == 0x01 && unenData1Bytes[1] == 0x01 ) {//long
                                intent.putExtra(MainActivity.INFO_TYPE, 0x01);
                            }
                            intent.putExtra(MainActivity.ENC_DATA_INFO, enData2);
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            }
        }
}
