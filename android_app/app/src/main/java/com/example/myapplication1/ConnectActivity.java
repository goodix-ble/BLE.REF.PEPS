package com.example.myapplication1;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.example.myapplication1.ble_connect.BLEConnectCallback;
import com.example.myapplication1.ble_connect.BLEConnectManager;
import com.example.myapplication1.scan.BLERequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import de.frank_durr.ecdh_curve25519.ECDHCurve25519;

public class ConnectActivity extends AppCompatActivity implements BLEConnectCallback {

    private BluetoothDevice mBluetoothDevice;
    private String mBluetoothDeviceName;
    private Toast toast;
    private boolean mConnectFlag = false;
    private BLEConnectManager bleConnectManager;

    private final static int MTU_SEND_SIZE = 247;
    public final static UUID UART_SERVICE_UUID = UUID.fromString("a6ed0201-d344-460a-8075-b9e8ec90d71b");
    private final static UUID UART_TX_CHARAC_UUID = UUID.fromString("a6ed0202-d344-460a-8075-b9e8ec90d71b");
    private final static UUID UART_RX_CHARAC_UUID = UUID.fromString("a6ed0203-d344-460a-8075-b9e8ec90d71b");
    private final static UUID UART_FLOW_CHARAC_UUID = UUID.fromString("a6ed0204-d344-460a-8075-b9e8ec90d71b");

    private BluetoothGattCharacteristic mTxCharac, mRxCharc, mFlowControlCharc;
    private int onePacketMaxDataLen  = 20;

    private byte[] bleWillSendData;
    private boolean bleSending = true;
    private int sendedCount = 0;
    byte[] alice_secret_key;
    byte[] alice_shared_secret;
    byte[] oriData;
    byte[] secretValue = null;
    private TextView infoText;
    Button button;

    private int[] paircode;
    private String randomAdvData;
    private Handler mHandler;
    private AlertDialog dialog;
    private TextView countText;
    private int count = 0;

    private final int[] authData = {185,98,214,22,193,31,190,184,209,136,116,6,253,198,26,86,134,34,62,47,41,224,225,57,172,189,205,208,52,
            133,43,200,5,106,114,67,145,197,182,123,242,118,70,246,45,165,37,221,219,105,157,35,229,20,48,235,104,
            102,17,36,156,4,68,175,250,59,227,107,126,18,32,249,103,135,84,146,245,244,138,96,132,212,83,228,170,174,
            150,113,21,3,94,137,239,159,91,93,87,166,237,1,186,100,202,196,131,233,192,220,216,44,7,69,27,199,252,167,
            161,194,9,181,236,97,11,217,108,147,28,64};

    private boolean auth = false;
    private int type = 0;
    private byte[] secureData;
    private String result;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        infoText = findViewById(R.id.infoText);

        Intent intent = getIntent();
        mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mBluetoothDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
        auth = intent.getBooleanExtra(MainActivity.SHARE_INFO_AUTH_ENABLE, false);
        if(auth){
            type = intent.getIntExtra(MainActivity.INFO_TYPE,0x02);
            secureData = intent.getByteArrayExtra(MainActivity.ENC_DATA_INFO);
        }
        SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
        sharedPreferenceUtil.saveCommonAdvName(mBluetoothDeviceName);

        bleConnectManager = new BLEConnectManager(this, this);
        connectDevice(mBluetoothDeviceName);
        button = findViewById(R.id.bondButton);
        button.setVisibility(View.INVISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!auth || type == 0x01) {
                    startBond();
                }else {//短期钥匙开锁
                    writeSecureData();
                }
            }
        });

        oriData = new byte[129];
        oriData[0] = (byte)128;
        for(int i=0; i<128; i++){
            oriData[i+1] = (byte)authData[i];
        }

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0x1111:
                        count++;
                        if(count >= 10){
                            dialog.dismiss();
                            finish();
                            bleConnectManager.disConnect();
                        } else {
                            countText.setText((10 - count)+"");
                            mHandler.sendEmptyMessageDelayed(0x1111, 1000);
                        }
                        break;
                    case 0x7788:
                        Toast toast = Toast.makeText(ConnectActivity.this, result, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);//竖直居中
                        toast.show();
                    default:break;
                }
            }

        };
    }

    private void writeSecureData(){
        byte[] sendData = new byte[34];
        sendData[0] = 0x09;
        sendData[1] = 0x0a;
        for(int i=0; i<secureData.length; i++){
            sendData[2+i] = secureData[i];
        }
        DfuLog.i("write:"+HexUtil.encodeHexStr(sendData));
        writeCharacteristic(sendData);
    }

    @Override
    public void onBleDisconnected() {
        DfuLog.i("onBleDisconnected");
    }

    @Override
    public void onBleConnected() {
        addLog("连接设备成功");
    }

    @Override
    public void onBleServicesDiscovered(BluetoothGatt gatt) {
        addLog("正在发现服务...");
        final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
        if (service != null) {
            mTxCharac = service.getCharacteristic(UART_TX_CHARAC_UUID);
            mRxCharc = service.getCharacteristic(UART_RX_CHARAC_UUID);
            addLog("成功发现服务");
        }
        if(mTxCharac != null && mRxCharc != null) {
            bleConnectManager.changeMtu(MTU_SEND_SIZE);
            addLog("开始MTU请求，MTU = 247");
        }
    }

    @Override
    public void onBleCharacteristicNotify(BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(UART_TX_CHARAC_UUID)){
            byte[] receive_data = characteristic.getValue();
            if(receive_data[0] == 0x01 && receive_data[1] == 0x02) {
                byte[] bob_public_key = new byte[32];
                for(int i=0; i<32; i++) {
                    bob_public_key[i] = receive_data[2+i];
                }
                alice_shared_secret = ECDHCurve25519.generate_shared_secret(alice_secret_key, bob_public_key);
                DfuLog.i(HexUtil.encodeHexStr(alice_shared_secret));
                addLog("秘钥交换成功，共享秘钥为："+HexUtil.encodeHexStr(alice_shared_secret));
                SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(this);
                sharedPreferenceUtil.saveShareKey(alice_shared_secret);//保存共享秘钥
                startAuth();
            }
            else if(receive_data[0] == 0x03 && receive_data[1] == 0x04) {
                addLog("认证成功\r\n");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!auth) {
                            button.setVisibility(View.VISIBLE);
                            button.setText("开始绑定设备");
                        } else {
                            if(type == 0x02) {
                                button.setVisibility(View.VISIBLE);
                                button.setText("开锁");
                            } else if(type == 0x01){
                                addLog("发送信息进行认证");
                                writeSecureData();
                            }
                        }
                    }
                });
            }
            else if(receive_data[0] == 0x05 && receive_data[1] == 0x06){
                addLog("开启HID广播成功");
                startExChangePairCode();
            }
            else if(receive_data[0] == 0x07 && receive_data[1] == 0x08){
                dialog.dismiss();
                addLog("交换配对码成功");
                Intent intent = new Intent();
                intent.setClass(this, BondDeviceActivity.class);
                intent.putExtra(BluetoothDevice.EXTRA_PAIRING_KEY, paircode);
                intent.putExtra(BluetoothDevice.EXTRA_NAME, randomAdvData);
                intent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, false);
                if(auth && type == 0x01){//是长期用户绑定
                    intent.putExtra(MainActivity.SHARE_INFO_AUTH_ENABLE, true);
                } else {
                    intent.putExtra(MainActivity.SHARE_INFO_AUTH_ENABLE, false);
                }
                bleConnectManager.disConnect();
                finish();
                startActivity(intent);
            }
            else if(receive_data[0] == 0x09 && receive_data[1] == 0x0a){
                mHandler.removeMessages(0x1111);
                addLog("确定开始绑定");
                if(auth) {
                    if(type == 0x01){//需要进行绑定
                        startExChangePairCode();
                    }
                }else {
                    startHidAdv();
                }
            }
            else if(receive_data[0] == 0x0b && receive_data[1] == 0x0c){
                result = "开锁失败";
                if(receive_data[2] == 0x01){
                    result = "开锁成功";
                }
                DfuLog.i("开锁结果"+receive_data[2]);
                mHandler.sendEmptyMessage(0x7788);
                addLog(result);
            }
            else if(receive_data[0] == 0x0d && receive_data[1] == 0x0e){//是否能开始绑定
                    //判断是否需要开始绑定，能绑定，就直接交换密码绑定
                if(receive_data[2] == 0x00){//可以开始绑定
                    randomAdvData = mBluetoothDeviceName.substring(3,9);//Com123456
                    result = "认证成功，可以开始绑定设备";
                    addLog(result);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            button.setText("开始绑定设备");
                            button.setVisibility(View.VISIBLE);
                        }
                    });
                } else  if(receive_data[2] == 0x01){
                    result = "设备绑定个数已超过最大限制";
                    addLog(result);
                } else if(receive_data[2] == 0x02){
                    result = "设备处于连接状态，请让已绑定用户关闭蓝牙";
                    addLog(result);
                }
                mHandler.sendEmptyMessage(0x7788);
            }
        }
    }

    @Override
    public void onBleNotifyEnable() {
        addLog("使能Notify成功");
        startExchangeKey();
    }

    @Override
    public void onBleCharacteristicWriteComplete(BluetoothGattCharacteristic characteristic) {
            DfuLog.i(characteristic.getUuid().toString());
    }

    @Override
    public void onBleMtuChanged(int mtu) {
        onePacketMaxDataLen = mtu -3;
        addLog("MTU请求成功，MTU = "+mtu);
        bleConnectManager.enableNotify(mTxCharac);
        addLog("开始使能Notify");
    }

    @Override
    public void onBleError(String message, int errorCode) {

    }

    @Override
    public void onBleTimeOut(String type) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bleConnectManager.disConnect();
        finish();
    }

    private void startExChangePairCode(){
        SecureRandom random = new SecureRandom();
        byte[] rand =  random.generateSeed(4);
        int value = Math.abs((((int)rand[0]) << 24) + (((int)rand[1]) << 16) + (((int)rand[2]) << 8) + rand[3]);
        value %= 1000000;
        paircode = new int[6];
        paircode[0]= (value / 100000);
        paircode[1]= ((value %100000)/10000);
        paircode[2]= ((value %10000)/1000);
        paircode[3]= ((value %1000)/100);
        paircode[4]= ((value %100)/10);
        paircode[5]= ((value %10));
        addLog("加密发送配对码："+paircode[0]+paircode[1]+paircode[2]+paircode[3]+paircode[4]+paircode[5]);
        byte[] sendCode = new byte[7];
        sendCode[0] = 0x06;
        for(int i=0; i<6; i++){
            sendCode[i+1] = (byte) paircode[i];
        }
        secretValue =  AES256ECB.encrypt(sendCode, alice_shared_secret);
        DfuLog.i(secretValue.length +" "+HexUtil.encodeHexStr(secretValue));
        byte[] sendData = new byte[2+secretValue.length];
        sendData[0] = 0x07;
        sendData[1] = 0x08;
        for(int i=0; i<secretValue.length; i++){
            sendData[2+i] = secretValue[i];
        }
        writeCharacteristic(sendData);
    }

    private void startBond() {
        count = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.waitdialog, null);
        countText = dialogView.findViewById(R.id.countText);
        //设置对话框布局
        dialog.setView(dialogView);
        dialog.setCancelable(false);
        dialog.show();
        mHandler.sendEmptyMessageDelayed(0x1111, 1000);
    }

    private void startHidAdv() {
        randomAdvData = yzm();
        char[] chatAdv = randomAdvData.toCharArray();
        byte[] sendData = new byte[8];
        sendData[0] = 0x05;
        sendData[1] = 0x06;
        sendData[2] = (byte) chatAdv[0];
        sendData[3] = (byte) chatAdv[1];
        sendData[4] = (byte) chatAdv[2];
        sendData[5] = (byte) chatAdv[3];
        sendData[6] = (byte) chatAdv[4];
        sendData[7] = (byte) chatAdv[5];
        addLog("开启HID广播，广播名为：Ebike"+randomAdvData);
        writeCharacteristic(sendData);
    }

    private void startExchangeKey(){
        addLog("开始交换秘钥");
        // Create Alice's secret key from a big random number.
        SecureRandom random = new SecureRandom();
        alice_secret_key = ECDHCurve25519.generate_secret_key(random);
        // Create Alice's public key.
        byte[] alice_public_key = ECDHCurve25519.generate_public_key(alice_secret_key);

        byte[] sendData = new byte[34];
        sendData[0] = 0x01;
        sendData[1] = 0x02;
        for(int i=0; i<32; i++){
            sendData[2+i] = alice_public_key[i];
        }
        writeCharacteristic(sendData);
    }

    private void startAuth(){
        addLog("开始进行认证");
        secretValue =  AES256ECB.encrypt(oriData, alice_shared_secret);
        DfuLog.i(secretValue.length +" "+HexUtil.encodeHexStr(secretValue));
        byte[] sendData = new byte[2+secretValue.length];
        sendData[0] = 0x03;
        sendData[1] = 0x04;
        for(int i=0; i<secretValue.length; i++){
            sendData[2+i] = secretValue[i];
        }
        writeCharacteristic(sendData);
    }

    private void writeCharacteristic(byte[] data) {
        Log.i("write","write_data:"+onePacketMaxDataLen+" "+data.length);
        bleConnectManager.writeCharacteristic(mRxCharc,data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
    }

    private void connectDevice(String device_name) {
        if (!BLERequest.isBLEEnabled(this)) {
            BLERequest.showBLEDialog(this);
        } else {
            addLog("正在连接设备...");
            mConnectFlag = true;
            bleConnectManager.connect(mBluetoothDevice, false, 10000);
        }
    }

    private void disConnectDeviceShow() {
        mConnectFlag = false;
    }

    //当需追加新内容时，直接调用此方法即可；
    public void addLog(final String strLog) {
        final String strText = strLog;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //附加与log前的时间标签（可注释）
                SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
                Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
                String strDate = formatter.format(curDate);

                //已有的log
                String strLogs = infoText.getText().toString().trim();
                if (strLogs.equals("")) {
                    strLogs = strDate + ": " + strText;
                } else {
                    strLogs += "\r\n" + strDate + ": " + strText;
                }
                //刷新添加新的log
                infoText.setText(strLogs);
                //==================add auto scroll========================
                //log View自动滚动
                infoText.post(new Runnable() {
                    @Override
                    public void run() {
                        int scrollAmount = infoText.getLayout().getLineTop(infoText.getLineCount()) - infoText.getHeight();
                        if (scrollAmount > 0)
                            infoText.scrollTo(0, scrollAmount);
                        else
                            infoText.scrollTo(0, 0);
                    }
                });
            }
        });
    }

    private  String yzm(){
        //提前写出来所有的合法的字符,然后,将生成的随机数当成索引使用;
        StringBuilder sb = new StringBuilder();
        // 直接利用for循环,可以将所有合法的字符添加到sb对象中
        for (char i = 'A'; i <='Z' ; i++) {
            sb.append(i);
        }
        for (char i = 'a'; i <='z' ; i++) {
            sb.append(i);
        }
        for (char i = '0'; i <='9' ; i++) {
            sb.append(i);
        }
        System.out.println(sb);
        //到此处,所有的合法字符都已经保存到了sb对象中
        //sb2用于保存最终生成的验证码
        StringBuilder sb2 = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i <6; i++) {
            //每循环一次,从sb中随机获取一个字符,并添加到sb2中
            int index=r.nextInt(sb.length());
            char c = sb.charAt(index);//从sb中随机获取一个字符
            sb2.append(c);//添加到sb2中
        }
        //System.out.println(sb2);
        //为了保证至少有一个数字,可以利用setCharAt方法,随机将已经存在的字符串中的某一个字符修改为我们的数字;
        //随机生成0  9之间的一个字符
        int i=r.nextInt(57-48+1)+48;
        char c =(char)i;
        int index = r.nextInt(sb2.length());
        sb2.setCharAt(index,c);//至少有一个数字;
        return sb2.toString();
    }
}