package com.goodix.ble.libble.misc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.RequiresApi;

import com.goodix.ble.libcomx.task.Task;
import com.goodix.ble.libcomx.task.TaskOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FindDeviceTask extends Task {

    private int timeout = 30_000;
    private int retryPeriod = 10_000;

    private String targetAddress;
    private String localName;
    private UUID targetUuid;
    private int minRssi = -127;

    private boolean checkNonExistent = false;
    private boolean checkConnected = false;
    private boolean checkBond = false;
    private BluetoothManager bluetoothManager;

    @TaskOutput
    private BluetoothDevice foundDevice;
    private BluetoothAdapter adapter;

    private CbApi21 cb21;
    private CbApi19 cb19;

    public FindDeviceTask setScanFilter(int minRssi) {
        this.minRssi = minRssi;
        return this;
    }

    public FindDeviceTask setScanFilter(UUID targetUuid) {
        this.targetUuid = targetUuid;
        return this;
    }

    public FindDeviceTask setScanFilter(String address) {
        this.targetAddress = address;
        return this;
    }

    public FindDeviceTask setNameFilter(String localName) {
        this.localName = localName;
        return this;
    }

    public FindDeviceTask setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public FindDeviceTask setCheckBond(boolean checkBond) {
        this.checkBond = checkBond;
        return this;
    }

    public FindDeviceTask setCheckConnected(boolean checkConnected, Context ctx) {
        this.checkConnected = checkConnected;
        if (ctx == null) {
            this.checkConnected = false;
        } else {
            this.bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        return this;
    }

    public FindDeviceTask setCheckNonExistent(boolean checkNonExistent) {
        this.checkNonExistent = checkNonExistent;
        return this;
    }

    /**
     * Restart scanning after specified duration.
     */
    public FindDeviceTask setRetryPeriod(int retryPeriod) {
        this.retryPeriod = retryPeriod;
        return this;
    }

    public BluetoothDevice getFoundDevice() {
        return foundDevice;
    }

    @Override
    protected int doWork() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (obtainExistedDevice()) return 0;
        // 分多次扫描
        onTimeout(525);
        return timeout;
    }

    @Override
    protected void onCleanup() {
        super.onCleanup();
        stopScan();
    }

    @Override
    protected void onTaskExpired() {
        // 超时了，还没有找到设备
        if (checkNonExistent) {
            finishedWithDone();
        } else {
            super.onTaskExpired();
        }
    }

    @Override
    protected void onTimeout(int id) {
        if (id == 521) {
            stopScan();
            // 延迟1秒再扫描
            startTimer(525, 1000);
        }
        if (id == 525) {
            if (obtainExistedDevice()) return;
            startScan();
            if (this.retryPeriod > 0) {
                startTimer(521, this.retryPeriod);
            }
        }
    }

    private void startScan() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && adapter.isLeCodedPhySupported()) {
                builder.setLegacy(false)
                        .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
            }

            if (this.cb21 == null) {
                this.cb21 = new CbApi21();
            }

            if (targetAddress != null || targetUuid != null) {
                ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
                ArrayList<ScanFilter> scanFilters = new ArrayList<>(1);
                if (targetAddress != null) {
                    filterBuilder.setDeviceAddress(targetAddress);
                }
                if (targetUuid != null) {
                    filterBuilder.setServiceUuid(new ParcelUuid(targetUuid));
                }
                scanFilters.add(filterBuilder.build());
                adapter.getBluetoothLeScanner().startScan(scanFilters, builder.build(), this.cb21);
            } else {
                adapter.getBluetoothLeScanner().startScan(null, builder.build(), this.cb21);
            }
        } else {
            if (this.cb19 == null) {
                this.cb19 = new CbApi19();
            }
            if (targetUuid != null) {
                adapter.startLeScan(new UUID[]{targetUuid}, this.cb19);
            } else {
                adapter.startLeScan(this.cb19);
            }
        }
    }

    public void stopScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            adapter.getBluetoothLeScanner().stopScan(this.cb21);
        } else {
            adapter.stopLeScan(this.cb19);
        }
    }

    private boolean obtainExistedDevice() {
        if (targetAddress != null) {
            if (checkBond) {
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    if (targetAddress.equals(device.getAddress())) {
                        setResult(device);
                        return true;
                    }
                }
            }
            if (checkConnected && bluetoothManager != null) {
                for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                    if (targetAddress.equals(device.getAddress())) {
                        setResult(device);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setResult(BluetoothDevice device) {
        foundDevice = device;
        setParameter(BluetoothDevice.class, foundDevice);
        if (checkNonExistent && device != null) {
            finishedWithError("Found device: " + device);
        } else {
            finishedWithDone();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    class CbApi21 extends android.bluetooth.le.ScanCallback {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            if (callbackType == android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                if (taskState != STATE_RUNNING) return;

                if (result.getRssi() < minRssi) return;
                if (localName != null) {
                    ScanRecord scanRecord = result.getScanRecord();
                    if (scanRecord == null) return;
                    String name = scanRecord.getDeviceName();
                    if (name == null || !name.contains(localName)) return;
                }

                final BluetoothDevice device = result.getDevice();
                if (logger != null) {
                    logger.v(getName(), "Found device: " + device);
                }
                setResult(device);
            }
        }

        @Override
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results) {
            if (results.size() > 0) {
                if (taskState != STATE_RUNNING) return;

                ScanResult result = results.get(0);

                if (result.getRssi() < minRssi) return;
                if (localName != null) {
                    ScanRecord scanRecord = result.getScanRecord();
                    if (scanRecord == null) return;
                    String name = scanRecord.getDeviceName();
                    if (name == null || !name.contains(localName)) return;
                }

                final BluetoothDevice device = result.getDevice();
                if (logger != null) {
                    logger.v(getName(), "Found device: " + device);
                }
                setResult(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String err;
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    err = "Fails to start scan as BLE scan with the same settings is already started by the app.";
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    err = "Fails to start scan as app cannot be registered.";
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    err = "Fails to start scan due an internal error.";
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    err = "Fails to start power optimized scan as this feature is not supported.";
                    break;
                default:
                    err = "UNKNOWN(" + errorCode + ")";
            }

            finishedWithError("Scan Failed: [" + errorCode + "] " + err);
        }
    }

    class CbApi19 implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (taskState != STATE_RUNNING) return;

            if (rssi < minRssi) return;
            if (targetAddress != null && !targetAddress.equals(device.getAddress())) return;
            if (localName != null) {
                String name = device.getName();
                if (name == null || !name.contains(localName)) return;
            }

            if (logger != null) {
                logger.v(getName(), "Found device: " + device);
            }
            setResult(device);
        }
    }
}
