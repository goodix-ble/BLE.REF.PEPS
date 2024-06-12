package com.goodix.ble.gr.toolbox.common.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.util.AppUtils;

/**
 * Created by yuanmingwu on 18-8-24.
 */

public class GridAdapter extends BaseAdapter{

    private Context mContext;
    private int[] mIconID;
    private int[] mIconName;

    public GridAdapter(Context context, int[] name, int[] iconID) {
        this.mContext = context;
        this.mIconID = iconID;
        this.mIconName = name;
    }
    @Override
    public int getCount() {
        return mIconID.length;
    }

    @Override
    public Object getItem(int i) {
        return mIconID[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = View.inflate(mContext, R.layout.item_grid, null);
            new GridAdapter.ViewHolder(view);
        }
        GridAdapter.ViewHolder holder = (GridAdapter.ViewHolder) view.getTag();
        holder.deviceImage.setImageResource(mIconID[i]);
        holder.nameText.setText(mIconName[i]);
        AppUtils.setImageViewColor(holder.deviceImage, mContext);
        return view;
    }

    private class ViewHolder {
        TextView nameText;
        ImageView deviceImage;
        public ViewHolder(View view) {
            nameText =  view.findViewById(R.id.text);
            deviceImage = view.findViewById(R.id.image);
            view.setTag(this);
        }
    }
}
