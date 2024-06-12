package com.goodix.ble.libuihelper.ble.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libuihelper.thread.UiExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class EasyLeScannerAdapter
        extends RecyclerView.Adapter<EasyLeScannerAdapter.VH>
        implements Comparator<ScannedDeviceItem>, IEventListener, Runnable {

    protected Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private long scanStopTimestamp = 0;
    private long sortStopTimestamp = 0;

    // 用于减少刷新界面的次数
    public final int UPDATE_INTERVAL = 2_00;
    private boolean pendingNotifyChanged = false;
    private long clickDebounceTimestamp = 0;

    private boolean showBonded = false;
    private boolean showConnected = false;
    private BluetoothManager btManager = null;

    @LayoutRes
    protected final int itemLayout;
    private final ArrayList<ScannedDeviceItem> mData = new ArrayList<>(128);
    private final ArrayList<ScannedDeviceItem> mDataOnUI = new ArrayList<>(128);
    private final HashMap<String, ScannedDeviceItem> mDataMap = new HashMap<>(128);

    private final EasyLeScanner easyLeScanner = new EasyLeScanner();

    private final DiffCB diffCB = new DiffCB();
    private long lastUpdateUI = 0;

    public EasyLeScannerAdapter(int itemLayout) {
        this.itemLayout = itemLayout;
        easyLeScanner.evtFound().register(this);
        easyLeScanner.evtScan().setExecutor(UiExecutor.getDefault()).register(this);
        easyLeScanner.evtError().setExecutor(UiExecutor.getDefault()).register(this);
    }

    public void setShowBonded(boolean showBonded) {
        this.showBonded = showBonded;
    }

    public void setShowConnected(BluetoothManager btManager, boolean showConnected) {
        this.showConnected = showConnected;
        this.btManager = btManager;
    }

    public void setStartBtn(View startBtn, int timeout) {
        if (startBtn != null) {
            startBtn.setOnClickListener(v -> {
                // 去抖动
                long now = System.currentTimeMillis();
                if (now - clickDebounceTimestamp < 200) {
                    return;
                }
                clickDebounceTimestamp = now;
                // 切换扫描状态
                if (easyLeScanner.isScanning()) {
                    stopScan();
                } else {
                    startScan(timeout);
                }
            });
        }
    }

    public boolean isScanning() {
        return easyLeScanner.isScanning();
    }

    public void startScan(int timeout) {
        if (timeout <= 0) {
            timeout = 5000; // 默认5秒
        }
        startScan();
        scanStopTimestamp = System.currentTimeMillis() + timeout;
        mHandler.removeCallbacks(this);
        pendingNotifyChanged = false; // 防止handler被移除了，但pendingNotifyChanged还为true的情况
        mHandler.postDelayed(this, timeout);
    }

    public void stopScan() {
        if (!easyLeScanner.isScanning()) {
            return;
        }
        easyLeScanner.stop();
        // mHandler.removeCallbacks(this); 等runnable自己清除自己
    }

    public void clear() {
        mData.clear();
        mDataMap.clear();
        mDataOnUI.clear();
        notifyDataSetChanged();
    }

    public void sortByRssi(int timeout) {
        if (timeout < 0) {
            // 一直排序
            sortStopTimestamp = -1;
            return;
        }
        if (timeout > 0) {
            // 在指定时间内对设备进行排序
            sortStopTimestamp = System.currentTimeMillis() + timeout;
            return;
        }

        // 立即排序并刷新
        synchronized (this) {
            Collections.sort(mData, this);
        }
        if (!pendingNotifyChanged) {
            pendingNotifyChanged = true;
            mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    }

    public int findItem(ScannedDeviceItem item) {
        for (int i = 0; i < mDataOnUI.size(); i++) {
            if (mDataOnUI.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public <T extends ScannedDeviceItem> T getItem(int pos) {
        if (pos < 0) return null;

        if (pos < mDataOnUI.size()) {
            //noinspection unchecked
            return (T) mDataOnUI.get(pos);
        }

        return null;
    }

    protected abstract void onStartScan();

    protected abstract void onStopScan();

    protected abstract void onBindViewHolder(@NonNull VH holder, ScannedDeviceItem device, int position);

    protected void onError(String msg) {
        if (mContext != null) {
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
        }
    }

    protected ScannedDeviceItem onCreateDeviceItem(LeScannerReport report) {
        return new ScannedDeviceItem();
    }

    protected VH onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int viewType) {
        VH vh;
        if (itemLayout != 0) {
            vh = new VH(this, inflater.inflate(itemLayout, parent, false));
        } else {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            vh = new VH(this, container);
        }
        return vh;
    }

    protected void onUpdateDeviceItem(ScannedDeviceItem deviceItem, LeScannerReport report) {

    }

    protected void onViewHolderClickListener(VH vh, View clickedView, ScannedDeviceItem deviceItem, int position) {

    }

    @Override
    public int compare(ScannedDeviceItem o1, ScannedDeviceItem o2) {
        if (showConnected) {
            if (o1.isConnected && o2.isConnected) {
                return o1.report.address.compareTo(o2.report.address);
            } else {
                if (o1.isConnected) {
                    return -1;
                }
                if (o2.isConnected) {
                    return 1;
                }
            }
        }
        if (showBonded) {
            if (o1.isBonded && o2.isBonded) {
                return o1.report.address.compareTo(o2.report.address);
            } else {
                if (o1.isBonded) {
                    return -1;
                }
                if (o2.isBonded) {
                    return 1;
                }
            }
        }
        return o2.getRssiAvg() - o1.getRssiAvg();
    }

    protected boolean onMatch(LeScannerReport report) {
        return true;
    }

    /**
     * May be called in I/O threads.
     */
    private ScannedDeviceItem handleScanResult(LeScannerReport report) {
        if (report == null) return null;
        if (!onMatch(report)) return null;
        return createOrUpdateDevice(report);
    }

    private ScannedDeviceItem createOrUpdateDevice(LeScannerReport report) {
        ScannedDeviceItem deviceItem = null;
        try {
            synchronized (this) {
                deviceItem = mDataMap.get(report.device.getAddress());
                if (deviceItem == null) {
                    deviceItem = onCreateDeviceItem(report);
                    deviceItem.name = report.device.getName();
                    mData.add(deviceItem);
                }
                mDataMap.put(report.address, deviceItem);
            }

            deviceItem.lastUpdate = System.currentTimeMillis();
            deviceItem.addRssi(report.rssi);

            String localName = report.advData.getLocalName();
            if (localName != null) {
                deviceItem.name = localName;
            } else {
                if (deviceItem.name == null) {
                    deviceItem.name = "N/A";
                }
            }

            onUpdateDeviceItem(deviceItem, report);
            deviceItem.report = report;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return deviceItem;
    }

    private void startScan() {
        if (easyLeScanner.isScanning()) {
            return;
        }

        handleFixedDevice();

        easyLeScanner.start();
    }

    private void handleFixedDevice() {
        if (showConnected && btManager != null) {
            List<BluetoothDevice> connectedDevices = btManager.getConnectedDevices(BluetoothProfile.GATT);
            for (BluetoothDevice device : connectedDevices) {
                LeScannerReport report = new LeScannerReport(device.getAddress());
                report.device = device;
                report.rssi = 0;
                ScannedDeviceItem item = createOrUpdateDevice(report);
                if (item != null) {
                    item.isConnected = true;
                }
            }
        }

        if (showBonded) {
            Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                LeScannerReport report = new LeScannerReport(device.getAddress());
                report.device = device;
                report.rssi = 0;
                ScannedDeviceItem item = createOrUpdateDevice(report);
                if (item != null) {
                    item.isBonded = true;
                }
            }
        }
    }

    @Override
    public final void run() {
        pendingNotifyChanged = false;
        synchronized (EasyLeScannerAdapter.this) {
            // 在更新界面前排序一次就可以了
            if (sortStopTimestamp == -1 || System.currentTimeMillis() < sortStopTimestamp) {
                Collections.sort(mData, this);
            }

            // 刷新修改
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCB);
            diffResult.dispatchUpdatesTo(this);

            // 将最新的数据显示到UI上
            mDataOnUI.clear();
            mDataOnUI.addAll(mData);

            lastUpdateUI = System.currentTimeMillis();
        }
        if (lastUpdateUI > scanStopTimestamp && isScanning()) {
            stopScan();
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        // 动画必须在UI更新间隔之中完成
        RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            SimpleItemAnimator animator = (SimpleItemAnimator) itemAnimator;
            int duration = UPDATE_INTERVAL;
            animator.setAddDuration(duration);
            animator.setRemoveDuration(duration);
            animator.setMoveDuration(duration);
            animator.setChangeDuration(duration);
            animator.setSupportsChangeAnimations(false);
        }
    }

    @NonNull
    @Override
    final public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VH vh;
        mContext = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        vh = onCreateViewHolder(inflater, parent, viewType);
        return vh;
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull VH holder) {
        // 这些Holder已经不显示在RecyclerView上了，但这些View还处于过渡状态（例如，动画状态），不能被回收
        // 重新这个函数，强制回收
        return true;
    }

    @Override
    final public void onBindViewHolder(@NonNull VH holder, int position) {
        ScannedDeviceItem device = mDataOnUI.get(position);
        onBindViewHolder(holder, device, position);
    }

    @Override
    public int getItemCount() {
        return mDataOnUI.size();
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (src == easyLeScanner) {
            if (evtType == EasyLeScanner.EVT_FOUND) {
                LeScannerReport report = (LeScannerReport) evtData;
                handleScanResult(report);
                if (!pendingNotifyChanged) {
                    pendingNotifyChanged = true;
                    mHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            } else if (evtType == EasyLeScanner.EVT_SCAN) {
                boolean scanning = (boolean) evtData;
                if (scanning) {
                    onStartScan();
                } else {
                    onStopScan();
                }
            } else if (evtType == EasyLeScanner.EVT_ERROR) {
                String msg = (String) evtData;
                onError(msg);
            }
        }
    }

    public static class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final EasyLeScannerAdapter hostAdapter;
        SparseArray<View> viewPool;

        public VH(EasyLeScannerAdapter hostAdapter, @NonNull View itemView) {
            super(itemView);
            this.hostAdapter = hostAdapter;

            itemView.setOnClickListener(this);
        }

        public final <T extends View> T findViewById(@IdRes int id) {
            View view = null;
            if (viewPool != null) {
                view = viewPool.get(id);
            }
            if (view == null) {
                view = itemView.findViewById(id);
                if (view != null) {
                    if (viewPool == null) {
                        viewPool = new SparseArray<>(8);
                    }
                    viewPool.append(id, view);
                }
            }
            //noinspection unchecked
            return (T) view;
        }

        public TextView getTextView(@IdRes int id) {
            return findViewById(id);
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            if (pos >= 0) {
                hostAdapter.onViewHolderClickListener(this, v, hostAdapter.mDataOnUI.get(pos), pos);
            }
        }
    }

    public class DiffCB extends DiffUtil.Callback {

        @Override
        public int getOldListSize() {
            return mDataOnUI.size();
        }

        @Override
        public int getNewListSize() {
            return mData.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mDataOnUI.get(oldItemPosition) == mData.get(newItemPosition);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mData.get(newItemPosition).lastUpdate < lastUpdateUI;
        }
    }
}
