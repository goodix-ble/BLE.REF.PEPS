package com.example.myapplication.scan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

/**
 * @see BLEScanner
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BLEScannerLollipop extends ScanCallback implements BLEScanner {
    private String mFilterDeviceAddress;
    private String mFilterDeviceName;
    private ParcelUuid mFilterServiceUUID;
    private BLEScanCallback scanCallback;
    private boolean searchAll = false;
    private BluetoothLeScanner scanner;
    private boolean mFound;
    private boolean mStart;

    @Override
    public void searchForAddress(final String deviceAddress, final BLEScanCallback scanCallback) {
        mFilterDeviceAddress = deviceAddress;
        this.scanCallback = scanCallback;
        startScan();
    }

    @Override
    public void searchForName(String devcieName, BLEScanCallback scanCallback) {
        mFilterDeviceName = devcieName;
        this.scanCallback = scanCallback;
        startScan();
    }

    @Override
    public void searchForServiceUUID(ParcelUuid uuid, BLEScanCallback scanCallback) {
        mFilterServiceUUID = uuid;
        this.scanCallback = scanCallback;
        startScan();
    }

    @Override
    public void searchAll(BLEScanCallback scanCallback) {
        searchAll = true;
        this.scanCallback = scanCallback;
        startScan();
    }

    @Override
    public void stopScan() {
        mStart = false;
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || adapter.getState() != BluetoothAdapter.STATE_ON) {
            scanCallback.onScanError();
            return;
        }
        scanner.stopScan(this);

        mFilterDeviceAddress = null;
        mFilterDeviceName = null;
        mFilterServiceUUID = null;
    }


    private void startScan() {
        mStart = true;
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || adapter.getState() != BluetoothAdapter.STATE_ON) {
            scanCallback.onScanError();
            return;
        }

        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            scanCallback.onScanError();
            return;
        }

        mFound = false;
        // Add timeout
        new Thread(() -> {
            try {
                Thread.sleep(BLEScanner.TIMEOUT);
            } catch (final InterruptedException e) {
                // do nothing
            }
            if (mStart) {
                scanCallback.onScanTimeOut(mFound);
                stopScan();
				mFound = false;
			}

        }, "Scanner timer").start();

        final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        final List<ScanFilter> filters = new ArrayList<>();
        if (mFilterServiceUUID != null) {
            filters.add(new ScanFilter.Builder().setServiceUuid(mFilterServiceUUID).build());
            scanner.startScan(filters, settings, this);
        } else if (mFilterDeviceName != null) {
            filters.add(new ScanFilter.Builder().setDeviceName(mFilterDeviceName).build());
            scanner.startScan(filters, settings, this);
        } else if (mFilterDeviceAddress != null) {
            filters.add(new ScanFilter.Builder().setDeviceAddress(mFilterDeviceAddress).build());
            scanner.startScan(filters, settings, this);
        } else {
            scanner.startScan(/*filters*/ null, settings, this);
        }
    }


    @Override
    public void onScanResult(final int callbackType, final ScanResult result) {
        if (!mStart) return;

        if (searchAll) {
            scanCallback.onScanResult(result.getDevice(), result.getRssi());
        } else {
            if (mFilterServiceUUID != null) {
                mFound = true;
                scanCallback.onScanResult(result.getDevice(), result.getRssi());
            } else {
                final String address = result.getDevice().getAddress();
                final String name = result.getDevice().getName();

                if (mFilterDeviceAddress != null) {
                    if (address.equals(mFilterDeviceAddress)) {
                        mFound = true;
                        scanCallback.onScanResult(result.getDevice(), result.getRssi());
                        return;
                    }
                }

                if (mFilterDeviceName != null) {
                    if (name != null && name.equals(mFilterDeviceName)) {
                        mFound = true;
                        scanCallback.onScanResult(result.getDevice(), result.getRssi());
                    }
                }
            }
        }

    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
    }
}