package com.example.myapplication.scan;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.myapplication.R;

import java.util.ArrayList;

public class DeviceListAdapter extends BaseAdapter {
	private final ArrayList<ScanBluetoothDevice> mDeviceList;
	private final Context mContext;

	public DeviceListAdapter(final Context context, ArrayList<ScanBluetoothDevice> list) {
		mContext = context;
		mDeviceList = list;
	}

	@Override
	public int getCount() {
		return mDeviceList.size();
	}


	@Override
	public ScanBluetoothDevice getItem(int position) {
			return mDeviceList.get(position );
	}


	@Override
	public long getItemId(int position) {
		return position;
	}

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = View.inflate(mContext,
					R.layout.item_list_scanner_device, null);
			new ViewHolder(convertView);
		}
		final ViewHolder holder = (ViewHolder) convertView.getTag();
		final ScanBluetoothDevice device = getItem(position);

		final String name = device.name;
		holder.name.setText(name != null ? name : mContext.getString(R.string.not_available));
		holder.address.setText(device.device.getAddress());

		int imgId = device.imageId;
		holder.deviceImage.setImageResource(imgId);
		holder.rssi.setText(device.rssi+"dBm");
		holder.rssi.setVisibility(View.VISIBLE);

		return convertView;
	}

	private class ViewHolder {
		private TextView name;
		private TextView address;
		private TextView rssi;
		private ImageView deviceImage;

		public ViewHolder(View view) {
			name = view.findViewById(R.id.text_name);
			address = view.findViewById(R.id.text_address);
			rssi = view.findViewById(R.id.text_rssi);
			deviceImage = view.findViewById(R.id.img_device);
			view.setTag(this);
		}
	}
}
