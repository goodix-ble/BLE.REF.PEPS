package com.example.myapplication1;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication1.ble_connect.BLEConnectCallback;
import com.example.myapplication1.ble_connect.BLEConnectManager;
import com.example.myapplication1.scan.BLEScanCallback;
import com.example.myapplication1.scan.BLEScanner;
import com.example.myapplication1.scan.BLEScannerFactory;
import com.king.zxing.util.CodeUtils;

import java.util.UUID;

public class BondDeviceActivity extends AppCompatActivity implements BLEScanCallback, BLEConnectCallback, View.OnClickListener {
    private BLEScanner bleScanner;
    private TextView titleTv;
    String deviceName;
    String deviceAddr;
    private BLEConnectManager bleConnectManager;
    private BluetoothDevice scanDevice;
    private Handler mHandler;
    public final static String CONNECT_STATE = "connect_str_state";
    private AlertDialog alertDialog;
    private TextView infoText;
    private final static int CONNECT_TIMEOUT = 0x810;
    private final static int SCAN_TIMEOUT  = 0x820;

    private final static int MTU_SEND_SIZE = 247;
    public final static UUID UART_SERVICE_UUID = UUID.fromString("a6ed0101-d344-460a-8075-b9e8ec90d71b");
    private final static UUID UART_TX_CHARAC_UUID = UUID.fromString("a6ed0102-d344-460a-8075-b9e8ec90d71b");
    private final static UUID UART_RX_CHARAC_UUID = UUID.fromString("a6ed0103-d344-460a-8075-b9e8ec90d71b");
    private final static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic mTxCharac, mRxCharc;
    private int onePacketMaxDataLen  = 20;
    private BluetoothGatt mBluetoothGatt= null;

    private Button shareLongButton, shareShortButton, getInfoButton;

    private byte[] shareKey;
    private String DeviceName;
    private String MacAddr;
    private String UserId;
    private boolean auth =false;
    private boolean ifManager = false;

    private final int[] LONG_USER_AUTH = {119,30,117,187,149,182,105,189,103,144,72,210,92,55,229,143,};

    private final int[] SHORT_USERAUTH = {49,119,115,173,215,177,8,112,101,219,170,222,105,195,41,4};

    public static final byte[] PRIVATE_KEY = {119,30,117,(byte)187,(byte)149,(byte)182,105,(byte)189,103,(byte)144,72,(byte)210,92,55,(byte)229,(byte)143,49,119,115,(byte)173,(byte)215,(byte)177,8,112,101,(byte)219,(byte)170,(byte)222,105,(byte)195,41,4};

    private String toastStr = "";
    private String[] macString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bond);
        titleTv = findViewById(R.id.txt_title);
        shareLongButton = findViewById(R.id.LongButton);
        shareShortButton = findViewById(R.id.ShortButton);
        getInfoButton = findViewById(R.id.getInfoButton);
        shareShortButton.setOnClickListener(this::onClick);
        shareLongButton.setOnClickListener(this::onClick);
        getInfoButton.setOnClickListener(this::onClick);

        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        UserId = sharedPreferenceUtil.getAppID();
        deviceName = sharedPreferenceUtil.getBondDeviceName();
        deviceAddr = sharedPreferenceUtil.getBondDeviceAddr();
        shareKey = sharedPreferenceUtil.getShareKey();
        DfuLog.i(UserId+" --"+deviceName+" --"+deviceAddr+"-- "+shareKey.length +"--"+HexUtil.encodeHexStr(shareKey));

        bleConnectManager = new BLEConnectManager(this, this);
        Intent intent = getIntent();
        auth = intent.getBooleanExtra(MainActivity.SHARE_INFO_AUTH_ENABLE, false);
        if(auth){
            getInfoButton.setEnabled(false);
        }
        boolean bondState = intent.getBooleanExtra(BluetoothDevice.EXTRA_BOND_STATE, false);
        if(bondState){//设备已经绑定了，可以直接连接发现服务
            boolean connectState = intent.getBooleanExtra(CONNECT_STATE, false);
            titleTv.setText("已连接");
            BluetoothDevice bondDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            DfuLog.i(bondDevice.getAddress()+" "+bondDevice.getName());
            if(connectState) {
                DfuLog.i("获取服务");
                bondDevice.connectGatt(this, false, new BluetoothGattCallback() {
                   @Override
                   public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                       super.onConnectionStateChange(gatt, status, newState);
                       DfuLog.i("onConnectionStateChange" + status + "---" + newState);
                       if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED){
                           mBluetoothGatt = gatt;
                           gatt.discoverServices();
                       }
                   }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                        final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
                        if (service != null) {
                            mTxCharac = service.getCharacteristic(UART_TX_CHARAC_UUID);
                            mRxCharc = service.getCharacteristic(UART_RX_CHARAC_UUID);
                            DfuLog.i("成功发现服务");
                        }
                        if(mTxCharac != null && mRxCharc != null) {
                            mBluetoothGatt.requestMtu(MTU_SEND_SIZE);
                            DfuLog.i("开始MTU请求，MTU = 247");
                        }
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        super.onMtuChanged(gatt, mtu, status);
                        onePacketMaxDataLen = mtu -3;
                        DfuLog.i("MTU请求成功，MTU = "+mtu);
                        enableNotify(mTxCharac);
                    }

                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorWrite(gatt, descriptor, status);
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if(descriptor != null && CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
                                final byte[] value = descriptor.getValue();
                                if (value != null && value.length == 2 && value[1] == 0x00) {
                                    if(value[0] == 0x01) {
                                        DfuLog.i("Notify 成功");
                                        //需要先获取user id，看当前用户是否为管理员用户，如果是管理员
                                        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(BondDeviceActivity.this);
                                        String commonName = sharedPreferenceUtil.getCommonAdvName();
                                        if(commonName.equals("Com123456")){//是管理员用户
                                            ifManager = true;
                                            if(sharedPreferenceUtil.ifSharedInfo() == false) {
                                                write_info();
                                            }
                                        }else {
                                            ifManager = false;
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    shareLongButton.setEnabled(false);
                                                    shareShortButton.setEnabled(false);
                                                    getInfoButton.setEnabled(true);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);
                        DfuLog.i(characteristic.getUuid().toString());
                        if(characteristic.getUuid().equals(UART_TX_CHARAC_UUID)){
                            byte[] receive_data = characteristic.getValue();
                            if(receive_data[0] == 0x05 && receive_data[1] == 0x06) {
                                char[] receiveUserid = new char[6];
                                for(int i=0; i<6; i++) {
                                    receiveUserid[i] = (char) receive_data[2+i];
                                }
                                String reUserIDStr = receiveUserid.toString();
                                if(receiveUserid.equals(UserId)) {//是管理员账户

                                }
                            }
                            else if(receive_data[0] == 0x01 && receive_data[1] == 0x02){
                                DfuLog.i("receve_info:"+HexUtil.encodeHexStr(receive_data));
                                if(receive_data[2] == 0x01) {
                                    int num = receive_data[6];
                                    macString = new String[num];
                                    int len = 7;
                                    byte[] bytes = new byte[6];
                                    for(int i=0; i<num; i++) {
                                        for(int j=0; j<6; j++) {
                                            bytes[j] = receive_data[len];
                                            len++;
                                        }
                                        bytes[5] += 1;
                                        String mac = byteToMac(bytes);
                                        macString[i] = mac;
                                        DfuLog.i(mac);
                                    }
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showListDialog();
                                        }
                                    });
                                }else{
                                    toastStr = "固件版本为:"+(int)receive_data[3]+"."+(int)receive_data[4]+"."+(int)receive_data[5];
                                    mHandler.sendEmptyMessage(0x7788);
                                }
                            }
                            else if(receive_data[0] == 0x07 && receive_data[1] == 0x08){
                                if(receive_data[2] == 0x00) {
                                    toastStr = "删除成功";
                                }else {
                                    toastStr = "删除失败";
                                }
                                mHandler.sendEmptyMessage(0x7788);
                            }
                        }
                    }
                });
            }else {
                titleTv.setText("未连接");
                getInfoButton.setClickable(false);
                getInfoButton.setEnabled(false);
                //获取设备信息按钮灰色
            }

        } else {
            int[] paircode = intent.getIntArrayExtra(BluetoothDevice.EXTRA_PAIRING_KEY);
            deviceName = "Ebike"+intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

            bleScanner =  BLEScannerFactory.getScanner();
            titleTv.setText(""+paircode[0]+paircode[1]+paircode[2]+paircode[3]+paircode[4]+paircode[5]);

            DfuLog.i("开始扫描设备:"+deviceName);
            bleScanner.searchForName(deviceName, this);
            showDialog("开始扫描设备:"+deviceName);
        }

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0x1111:
                        DfuLog.i("check");
                        mHandler.sendEmptyMessageDelayed(0x1111, 1000);
                        if(scanDevice !=null){
                            switch (scanDevice.getBondState()) {
                                case BluetoothDevice.BOND_BONDING:
                                    DfuLog.i("正在绑定");
                                    break;
                                case BluetoothDevice.BOND_BONDED:
                                    DfuLog.i("完成绑定，绑定成功");
                                    alertDialog.dismiss();
                                    mHandler.removeMessages(0x1111);
                                    SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(BondDeviceActivity.this);
                                    sharedPreferenceUtil.BondDeviceAdd(deviceName, deviceAddr);
                                    titleTv.setText("");//清空配对码显示
                                    //进行保存
                                    Intent LaunchIntent = new Intent();
                                    LaunchIntent.setClass(BondDeviceActivity.this, MainActivity.class);
                                    startActivity(LaunchIntent);
                                    finish();
                                    break;
                                default:break;
                            }
                        }

                        break;
                    case CONNECT_TIMEOUT:
                        alertDialog.dismiss();
                        titleTv.setText("未连接");
                        showToast("连接超时");
                        break;
                    case SCAN_TIMEOUT:
                        titleTv.setText("未连接");
                        showToast("扫描超时");
                        break;
                    case 0x7788:
                        showToast(toastStr);
                        break;
                    default:break;
                }
            }
        };
        if(!bondState) {
            mHandler.sendEmptyMessageDelayed(0x1111, 1000);
        }
    }
    private ArrayAdapter<String> adapter;
    private String[] datas={"1","2","3","4","5"};

    private void showListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BondDeviceActivity.this);
        builder.setTitle("已绑定用户手机地址");
        builder.setPositiveButton("确定",null);
        builder.setItems(macString, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                DfuLog.i("选择的地址为：" + macString[which]);
               // if(which != 0) {
                    //showDeleteSureDialog(macString[which]);
               // }
            }
        });
        builder.show();
    }

    private void showDeleteSureDialog(String addr)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(BondDeviceActivity.this);
        builder.setTitle("提示");
        builder.setMessage("删除"+addr+"设备");
        builder.setNegativeButton("取消",null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DfuLog.i("删除设备:"+addr);
                String macAddr = addr.replace(":","");
                DfuLog.i("删除设备:"+macAddr);
                byte[] mac = HexUtil.decodeHex(macAddr.toCharArray());
                byte[] sendData = new byte[8];
                mac[5]-=1;
                sendData[0] = 0x07;
                sendData[1]= 0x08;
                for(int j=0; j<6; j++) {
                    sendData[2+j] = mac[5-j];
                }
                writeCharacteristic(sendData);
            }
        });
        builder.show();
    }

    private String byteToMac(byte[] bytes) {
        String str = HexUtil.encodeHexStr(bytes);
        str = str.substring(0,bytes.length*2);

        // 在每两个字符中插入冒号
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < str.length(); i += 2) {
            resultBuilder.append(str.substring(i, i + 2));
            if (i < str.length() - 2) {
                resultBuilder.append(":");
            }
        }
        String result = resultBuilder.toString();
        return result;
    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    public void onScanTimeOut(boolean found) {
        DfuLog.i("onScanTimeOut" + found);
        alertDialog.dismiss();
        mHandler.sendEmptyMessage(SCAN_TIMEOUT);
    }

    @Override
    public void onScanResult(BluetoothDevice device, int rssi) {
        bleScanner.stopScan();
        DfuLog.i("扫描到设备");
        scanDevice =device;
        deviceAddr = device.getAddress();
        showDialog("开始连接:"+deviceName);
        bleConnectManager.connect(device,false, 10000);
    }

    @Override
    public void onScanError() {

    }

    @Override
    public void onBleDisconnected() {

    }

    @Override
    public void onBleConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showDialog("已连接设备");
            }
        });
    }

    @Override
    public void onBleServicesDiscovered(BluetoothGatt gatt) {
        scanDevice.createBond();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showDialog("正在绑定设备");
            }
        });
    }

    @Override
    public void onBleCharacteristicNotify(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onBleNotifyEnable() {

    }

    @Override
    public void onBleCharacteristicWriteComplete(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onBleMtuChanged(int mtu) {

    }

    @Override
    public void onBleError(String message, int errorCode) {

    }

    @Override
    public void onBleTimeOut(String type) {
        DfuLog.i("连接超时");
        mHandler.sendEmptyMessage(CONNECT_TIMEOUT);

    }

    private boolean write_info() {
        byte[] sendData = new byte[46];
        DfuLog.i("发送数据:");
        sendData[0] = 0x03;
        sendData[1] = 0x04;
        char[] name = deviceName.substring(5,11).toCharArray();//ebike123456
        for(int i=0; i<6; i++){
            sendData[2+i] = (byte)name[i];
        }
        char[] userid = UserId.toCharArray();
        for(int i=0; i<6; i++){
            sendData[8+i] = (byte)userid[i];
        }
        for(int i=0; i<32; i++) {
            sendData[14+i] = shareKey[i];
        }
        return writeCharacteristic(sendData);
    }

    private boolean getDeviceUserID() {
        byte[] sendData = new byte[2];
        DfuLog.i("发送数据:");
        sendData[0] = 0x05;
        sendData[1] = 0x06;
        return writeCharacteristic(sendData);
    }

    private boolean getInfo() {
        byte[] sendData = new byte[3];
        DfuLog.i("发送数据:");
        sendData[0] = 0x01;
        sendData[1] = 0x02;
        sendData[2] = 0x00;
        if(ifManager) {
            sendData[2] = 0x01;
        }
       return writeCharacteristic(sendData);
    }

    private boolean writeCharacteristic(byte[] sendData) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || mRxCharc == null)
            return false;
        mRxCharc.setValue(sendData);
        mRxCharc.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        DfuLog.i("发送数据: " +sendData.length+" " +HexUtil.encodeHexStr(sendData));
        // Check characteristic property
        final int properties = mRxCharc.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;
        return gatt.writeCharacteristic(mRxCharc);
    }

    private void showDialog (String showStr) {
        if(alertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            alertDialog = builder.create();
            View dialogView = View.inflate(this, R.layout.waitdialog, null);
            infoText = dialogView.findViewById(R.id.infoText);
            TextView contText = dialogView.findViewById(R.id.countText);
            contText.setText("");
            infoText.setText(showStr);
            //设置对话框布局
            alertDialog.setView(dialogView);
            alertDialog.setCancelable(false);
            alertDialog.show();
        } else {
            infoText.setText(showStr);
        }
    }

    public void showToast(String showStr)
    {
        Toast toast = Toast.makeText(getApplicationContext(), showStr, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);//竖直居中
        toast.show();
    }

    public boolean enableNotify(BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0)
            return false;
        gatt.setCharacteristicNotification(characteristic, true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor == null)
            return false;

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
        final int originalWriteType = parentCharacteristic.getWriteType();
        parentCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        final boolean result = mBluetoothGatt.writeDescriptor(descriptor);
        parentCharacteristic.setWriteType(originalWriteType);
        return result;
    }

    private void showQRCode(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog1 = builder.create();
        View dialogView = View.inflate(this, R.layout.qrcoedialog, null);
        ImageView qrImg = dialogView.findViewById(R.id.qrImg);
        //生成二维码
        Bitmap qrCode = CodeUtils.createQRCode(str, 800, null);
        qrImg.setImageBitmap(qrCode);
        dialog1.setView(dialogView);
        dialog1.show();
    }

    @Override
    public void onClick(View view) {
        byte[] sendData = new byte[12];
        int len = 0;
        String qrString = null;

        int viewID = view.getId();
        if(viewID == R.id.LongButton || viewID == R.id.ShortButton)
        {
            byte[]authInfo = new byte[10];
            len = 0;
            if(viewID == R.id.LongButton) {
                sendData[len++] = 0x01;
                sendData[len++]= 0x01;
                for(int i=0; i<10; i++){
                    authInfo[i] = (byte)LONG_USER_AUTH[i];
                }
            }
            else {
                sendData[len++] = 0x02;
                sendData[len++]= 0x02;
                for(int i=0; i<10; i++){
                    authInfo[i] = (byte)SHORT_USERAUTH[i];
                }
            }
            long time = System.currentTimeMillis()/1000;
            DfuLog.i("time = "+time);
            sendData[len++] = (byte)(time & 0xff);
            sendData[len++] = (byte)(time>>8 & 0xff);
            sendData[len++] = (byte)(time>>16 & 0xff);
            sendData[len++] = (byte)(time>>24 & 0xff);

            // device name
            char[] name = deviceName.substring(5,11).toCharArray();//ebike123456
            for(int i=0; i<6; i++){
                sendData[len] = (byte)name[i];
                len++;
            }
            DfuLog.i(HexUtil.encodeHexStr(sendData));
            byte[] secureSendData = AES256ECB.encrypt(sendData, PRIVATE_KEY);
            String str1 = HexUtil.encodeHexStr(secureSendData).substring(0,32);
            DfuLog.i(secureSendData.length +" " +str1);
            //-----------------------------------------------------
            byte[] sendData1 = new byte[18];
            len = 0;
            sendData1[len++] = sendData[0];
            sendData1[len++]= sendData[1];

            char[] userid = UserId.toCharArray();//userID
            for(int i=0; i<6; i++){
                sendData1[len] = (byte)userid[i];
                len++;
            }
            for(int i=0; i<10; i++){//认证码
                sendData1[len] = authInfo[i];
                len++;
            }
            DfuLog.i("sendData1:",HexUtil.encodeHexStr(sendData1));
            //是有sharekey对数据进行加密
            byte[] secureSendData1 = AES256ECB.encrypt(sendData1, shareKey);
            String str2 = HexUtil.encodeHexStr(secureSendData1).substring(0,64);
            DfuLog.i(secureSendData1.length +" "+str2);
            qrString = str1 + str2;
            DfuLog.i("qrstr:" + qrString);
            showQRCode(qrString);//显示二维码
        }
        else if(viewID == R.id.getInfoButton) {
            getInfo();
        }
    }
}