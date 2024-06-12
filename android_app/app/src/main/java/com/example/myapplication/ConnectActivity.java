package com.example.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;

import com.example.myapplication.ble_connect.BLEConnectCallback;
import com.example.myapplication.ble_connect.BLEConnectManager;
import com.example.myapplication.scan.BLERequest;

import androidx.appcompat.app.AppCompatActivity;

import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import java.security.SecureRandom;

import de.frank_durr.ecdh_curve25519.ECDHCurve25519;

public class ConnectActivity extends AppCompatActivity implements BLEConnectCallback {

    private ProgressDialog p_dialog;
    private BluetoothDevice mBluetoothDevice;
    private String mBluetoothDeviceName;
    private Toast toast;
    private MenuItem mStateItem = null;
    private boolean mConnectFlag = false;
    private BLEConnectManager bleConnectManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mBluetoothDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
        bleConnectManager = new BLEConnectManager(this, this);
        connectDevice(mBluetoothDeviceName);

        // Create Alice's secret key from a big random number.
        SecureRandom random = new SecureRandom();
        byte[] alice_secret_key = ECDHCurve25519.generate_secret_key(random);
        // Create Alice's public key.
        byte[] alice_public_key = ECDHCurve25519.generate_public_key(alice_secret_key);

        // Bob is also calculating a key pair.
        byte[] bob_secret_key = ECDHCurve25519.generate_secret_key(random);
        byte[] bob_public_key = ECDHCurve25519.generate_public_key(bob_secret_key);

        // Assume that Alice and Bob have exchanged their public keys.

        // Alice is calculating the shared secret.
        byte[] alice_shared_secret = ECDHCurve25519.generate_shared_secret(
                alice_secret_key, bob_public_key);

        // Bob is also calculating the shared secret.
        byte[] bob_shared_secret = ECDHCurve25519.generate_shared_secret(
                bob_secret_key, alice_public_key);

        String alice_shared_secret_str = HexUtil.encodeHexStr(alice_shared_secret);
        String bob_shared_secret_str = HexUtil.encodeHexStr(bob_shared_secret);
        DfuLog.i(alice_shared_secret_str);
        DfuLog.i(bob_shared_secret_str);
    }

    @Override
    public void onBleDisconnected() {
        DfuLog.i("onBleDisconnected");
    }

    @Override
    public void onBleConnected() {
        DfuLog.i("onBleConnected");
        p_dialog.dismiss();
    }

    @Override
    public void onBleServicesDiscovered(BluetoothGatt gatt) {

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

    }

    private void connectDevice(String device_name) {
        if (!BLERequest.isBLEEnabled(this)) {
            BLERequest.showBLEDialog(this);
        } else {
            mConnectFlag = true;
            bleConnectManager.connect(mBluetoothDevice, false, 10000);
            showProgressDialog(getString(R.string.connect_device)+device_name);
            if(mStateItem != null)
                mStateItem.setTitle(getString(R.string.common_disconnect));
        }
    }

    private void disConnectDeviceShow() {
        mConnectFlag = false;
        showAlertDialog(getString(R.string.device_disconnect));
        mStateItem.setTitle(getString(R.string.common_connect));
    }

    public void showProgressDialog(String string){
        if(p_dialog == null) {
            p_dialog = new ProgressDialog(this);
            p_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            p_dialog.setTitle(R.string.please_wait);
            p_dialog.setMessage(string);
            p_dialog.setCancelable(false);
            p_dialog.show();
        } else {
            p_dialog.setMessage(string);
        }
    }

    private void dismissProgressDialog() {
        if(p_dialog != null) {
            p_dialog.dismiss();
            p_dialog = null;
        }
    }

    private void showAlertDialog(String showStr) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ConnectActivity.this);
        builder.setTitle(showStr);
        builder.setNegativeButton(R.string.reconnect_device, (dialog, which) -> {
            connectDevice(mBluetoothDeviceName);
        });
        builder.setPositiveButton(R.string.common_cancel, null);
        builder.setCancelable(false);
        builder.show();
    }
}