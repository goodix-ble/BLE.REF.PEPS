package com.goodix.ble.libuihelper.ble.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public class EasyLeScanner {
    public static final int EVT_SCAN = 496;
    public static final int EVT_ERROR = 497;
    public static final int EVT_FOUND = 498;

    //    public static class Reports extends ArrayList<Report> implements IRecyclable {
//        AbsPoolItem poolCtrl = new AbsPoolItem();
//
//        @Override
//        public void reuse(Pool pool) {
//            poolCtrl.reuse(pool);
//        }
//
//        @Override
//        public int getRefCnt() {
//            return poolCtrl.getRefCnt();
//        }
//
//        @Override
//        public void retain() {
//            poolCtrl.retain();
//        }
//
//        @Override
//        public void release() {
//            poolCtrl.release();
//        }
//    }

    boolean scanning;
    CbApi21 cb21;
    CbApi19 cb19;
    HashMap<String, LeScannerReport> reportCache = new HashMap<>(128);
    //Pool reportsPool = new Pool(Reports.class, 16);

    private final BluetoothAdapter adapter;
    private ILeScannerFilter filter;

    Event<Boolean> eventScan = new Event<>(this, EVT_SCAN);
    Event<String> eventError = new Event<>(this, EVT_ERROR);
    Event<LeScannerReport> eventFound = new Event<>(this, EVT_FOUND);

    public EasyLeScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.cb21 = new CbApi21();
        } else {
            this.cb19 = new CbApi19();
        }
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setFilter(ILeScannerFilter filter) {
        this.filter = filter;
    }

    public ILeScannerFilter getFilter() {
        return filter;
    }

    public void setLogger(ILogger logger) {

    }

    public void start() {
        if (scanning) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanSettings.Builder builder = new ScanSettings.Builder();
            builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setLegacy(false); // scan extended advertising
                if (adapter.isLeCodedPhySupported()) {
                    builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
                }
            }
            adapter.getBluetoothLeScanner().startScan(null, builder.build(), this.cb21);
        } else {
            adapter.startLeScan(this.cb19);
        }

        eventScan.postEvent(scanning = true);
    }

    public void stop() {
        if (!scanning) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            adapter.getBluetoothLeScanner().stopScan(this.cb21);
        } else {
            adapter.stopLeScan(this.cb19);
        }

        eventScan.postEvent(scanning = false);
    }

    public boolean isScanning() {
        return scanning;
    }

    public Event<Boolean> evtScan() {
        return eventScan;
    }

    public Event<String> evtError() {
        return eventError;
    }

    public Event<LeScannerReport> evtFound() {
        return eventFound;
    }

    private synchronized LeScannerReport fetchReport(String address) {
        LeScannerReport report = reportCache.get(address);
        if (report == null) {
            report = new LeScannerReport(address);
            reportCache.put(address, report);
        }
        return report;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    class CbApi21 extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                String address = result.getDevice().getAddress();
                LeScannerReport report = fetchReport(address);
//                Reports reports = reportsPool.get();
//                reports.clear();
//                reports.add(report);

                setReport(result, report);

                ILeScannerFilter filter = EasyLeScanner.this.filter;
                if (filter == null || filter.match(report)) {
                    eventFound.postEvent(report);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
//            Reports reports = reportsPool.get();
//            reports.clear();

            for (ScanResult result : results) {
                String address = result.getDevice().getAddress();
                LeScannerReport report = fetchReport(address);

                setReport(result, report);

                ILeScannerFilter filter = EasyLeScanner.this.filter;
                if (filter == null || filter.match(report)) {
                    eventFound.postEvent(report);
                }
//                reports.add(report);
            }

//            eventFound.postEvent(reports);
        }

        private void setReport(ScanResult result, LeScannerReport report) {
            report.rssi = result.getRssi();
            report.pduType = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !result.isConnectable()) {
                report.pduType = LeScannerReport.PDU_TYPE_ADV_NONCONN_IND;
            }
            report.timestampNano = result.getTimestampNanos();
            report.timestamp = report.timestampNano / 1000000L;
            report.setPayload(null);
            report.device = result.getDevice();
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                report.setPayload(record.getBytes());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                report.extended = !result.isLegacy();
                report.advertisingSetId = result.getAdvertisingSid();
                report.periodicAdvertisingInterval = result.getPeriodicAdvertisingInterval();
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
            eventError.postEvent(err);
            stop();
        }
    }

    class CbApi19 implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!scanning) return;

            String address = device.getAddress();
            LeScannerReport report = fetchReport(address);

            report.rssi = rssi;
            report.pduType = 0;
            report.extended = false;
            report.timestampNano = System.nanoTime();
            report.timestamp = System.currentTimeMillis();
            report.device = device;
            report.setPayload(scanRecord);

            ILeScannerFilter filter = EasyLeScanner.this.filter;
            if (filter == null || filter.match(report)) {
                eventFound.postEvent(report);
            }
//            Reports reports = reportsPool.get();
//            reports.clear();
//            reports.add(report);
//
//            eventFound.postEvent(reports);
        }
    }
}
