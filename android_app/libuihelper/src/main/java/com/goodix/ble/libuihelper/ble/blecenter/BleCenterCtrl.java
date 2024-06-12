package com.goodix.ble.libuihelper.ble.blecenter;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.goodix.ble.libble.center.BleCenter;
import com.goodix.ble.libble.center.BleItem;
import com.goodix.ble.libble.v2.gb.GBRemoteDevice;
import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.sublayout.list.MvcController;
import com.goodix.ble.libuihelper.sublayout.list.MvcViewHolder;
import com.goodix.ble.libuihelper.thread.UiExecutor;

public class BleCenterCtrl extends MvcController<BleItem, BleCenterCtrl.VH> implements View.OnClickListener, IEventListener, PopupMenu.OnMenuItemClickListener {
    private BleCenterFragment host;
    private PopupMenu menu = null;

    BleCenterCtrl(BleCenterFragment host) {
        this.host = host;
    }

    @Override
    public MvcController<BleItem, VH> onClone() {
        return new BleCenterCtrl(host);
    }

    @Override
    protected void onCreate(int position, BleItem item) {
        item.getGatt().evtStateChanged().subEvent(this)
                .setExecutor(UiExecutor.getDefault())
                .register(this);

        BleCenter.get().evtSelected().subEvent(this)
                .setExecutor(UiExecutor.getDefault())
                .register(this);
    }

    @Override
    public MvcViewHolder onCreateView(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.libuihelper_item_ble_device, parent, false));
    }

    @Override
    protected void onAttach(int position, BleItem item, VH vh) {
        final GBRemoteDevice remoteDevice = item.getGatt();
        vh.nameTv.setText(remoteDevice.getName());
        vh.addressTv.setText(remoteDevice.getAddress());
        vh.statusTv.setText(null);
        vh.connectBtn.setText(null);
        vh.selectRb.setChecked(item == BleCenter.get().getSelectedDevice());

        updateConnectionState(remoteDevice.getState());

        vh.menuBtn.setVisibility(host.menuCB != null ? View.VISIBLE : View.GONE);
        vh.connectBtn.setVisibility(host.showConnect ? View.VISIBLE : View.GONE);
        vh.selectRb.setVisibility(host.showRadio ? View.VISIBLE : View.GONE);

        vh.menuBtn.setOnClickListener(this);
        vh.connectBtn.setOnClickListener(this);
        vh.selectRb.setOnClickListener(this);
    }

    @Override
    protected void onDetach(int position, BleItem item, VH vh) {
        vh.menuBtn.setOnClickListener(null);
        vh.connectBtn.setOnClickListener(null);
    }

    @Override
    public void onDestroy() {
        item.getGatt().evtStateChanged().clear(this);
        BleCenter.get().evtSelected().clear(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.libuihelper_item_ble_device_connect_btn) {
            final GBRemoteDevice remoteDevice = item.getGatt();
            if (remoteDevice.getState() == GBRemoteDevice.STATE_DISCONNECTED) {
                remoteDevice.connect(0).setRetry(3, 1000).startProcedure();
                //remoteDevice.connect(0).setBackgroundMode(true).startProcedure();
                remoteDevice.discoverServices().startProcedure();
            } else {
                remoteDevice.disconnect(true).startProcedure();
            }
        } else if (v.getId() == R.id.libuihelper_item_ble_device_select_rb) {
            if (holder != null) {
                if (item == BleCenter.get().getSelectedDevice()) {
                    holder.selectRb.setSelected(true);
                } else {
                    BleCenter.get().setSelectedDevice(item);
                }
            }
        } else if (v.getId() == R.id.libuihelper_item_ble_device_menu_btn) {
            if (menu == null) {
                menu = new PopupMenu(v.getContext(), v);
                host.menuCB.onCreateMenu(host, this.item, menu.getMenuInflater(), menu.getMenu());
                menu.setOnMenuItemClickListener(this);
            }
            menu.show();
        }
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (evtType == GBRemoteDevice.EVT_STATE_CHANGED) {
            updateConnectionState((Integer) evtData);
        } else if (evtType == BleCenter.EVT_SELECTED) {
            if (holder != null) {
                holder.selectRb.setChecked(item == BleCenter.get().getSelectedDevice());
            }
        }
    }

    private void updateConnectionState(int state) {
        if (holder == null) {
            return;
        }

        switch (state) {
            case GBRemoteDevice.STATE_CONNECTED:
                holder.statusTv.setText(R.string.libuihelper_connected);
                holder.connectBtn.setText(R.string.libuihelper_disconnect);
                break;
            case GBRemoteDevice.STATE_CONNECTING:
                holder.statusTv.setText(R.string.libuihelper_connecting);
                holder.connectBtn.setText(R.string.libuihelper_disconnect);
                break;
            case GBRemoteDevice.STATE_DISCONNECTING:
                holder.statusTv.setText(R.string.libuihelper_disconnecting);
                holder.connectBtn.setText(R.string.libuihelper_disconnect);
                break;
            case GBRemoteDevice.STATE_DISCONNECTED:
                holder.statusTv.setText(R.string.libuihelper_disconnected);
                holder.connectBtn.setText(R.string.libuihelper_connect);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        host.menuCB.onMenuClicked(host, this.item, item);
        return true;
    }

    static class VH extends MvcViewHolder {
        TextView nameTv;
        TextView addressTv;
        TextView statusTv;
        ImageButton menuBtn;
        Button connectBtn;
        RadioButton selectRb;

        VH(@NonNull View itemView) {
            super(itemView);

            nameTv = itemView.findViewById(R.id.libuihelper_item_ble_device_name_tv);
            addressTv = itemView.findViewById(R.id.libuihelper_item_ble_device_address_tv);
            statusTv = itemView.findViewById(R.id.libuihelper_item_ble_device_status_tv);
            menuBtn = itemView.findViewById(R.id.libuihelper_item_ble_device_menu_btn);
            connectBtn = itemView.findViewById(R.id.libuihelper_item_ble_device_connect_btn);
            selectRb = itemView.findViewById(R.id.libuihelper_item_ble_device_select_rb);
        }
    }
}
