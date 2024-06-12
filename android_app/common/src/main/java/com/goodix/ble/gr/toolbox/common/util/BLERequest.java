package com.goodix.ble.gr.toolbox.common.util;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Created by yuanmingwu on 18-8-13.
 */

public class BLERequest {

    public final static int REQUEST_ENABLE_BT = 205;
    public static final int REQUEST_LOCATION_PERMISSION = 514;

    public static boolean isBLEEnabled(Context context) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }

        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    public static void showBLEDialog(Activity activity) {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    public static void showBLEDialog(Fragment fragment) {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        fragment.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    public static boolean checkBlePermission(final @NonNull Activity activity) {
        boolean granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!granted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android10需要拥有ACCESS_FINE_LOCATION权限才行，低版本的才可以只用两个权限之一。
            // 需要后台扫描的话，还需要申请ACCESS_BACKGROUND_LOCATION权限
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        return granted;
    }

    public static boolean checkBlePermission(final @NonNull Fragment fragment) {
        boolean granted = ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!granted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fragment.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        return granted;
    }
}
