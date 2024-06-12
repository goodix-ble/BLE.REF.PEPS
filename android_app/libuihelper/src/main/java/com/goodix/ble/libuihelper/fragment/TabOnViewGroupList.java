package com.goodix.ble.libuihelper.fragment;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libuihelper.sublayout.list.IMvcListView;
import com.goodix.ble.libuihelper.sublayout.list.MvcAdapter;
import com.goodix.ble.libuihelper.sublayout.list.MvcController;
import com.goodix.ble.libuihelper.sublayout.list.MvcViewHolder;

import java.util.ArrayList;
import java.util.List;

public class TabOnViewGroupList extends RecyclerView.AdapterDataObserver implements IMvcListView {
    private MvcAdapter adapter;
    private List<ViewGroup> viewGroupList;
    private ArrayList<MvcViewHolder> holdersList;

    public TabOnViewGroupList(List<ViewGroup> viewGroupList) {
        if (viewGroupList != null) {
            this.viewGroupList = viewGroupList;
            holdersList = new ArrayList<>(viewGroupList.size());
        } else {
            this.viewGroupList = new ArrayList<>(0);
        }
    }

    @Override
    public void setAdapter(MvcAdapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterAdapterDataObserver(this);
        }
        this.adapter = adapter;
        adapter.registerAdapterDataObserver(this);

        holdersList.ensureCapacity(adapter.getItemCount());

        onChanged();
    }

    @Override
    public MvcAdapter getAdapter() {
        return adapter;
    }

    @Override
    public ViewGroup.LayoutParams generateItemLayoutParams(int position) {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public void scrollToPosition(int position, boolean smooth) {
        // do nothing
    }

    @Override
    public void onChanged() {
        int pos = 0;
        for (; pos < adapter.getItemCount() && pos < viewGroupList.size(); pos++) {
            final MvcController item = adapter.getItem(pos);
            final ViewGroup parent = viewGroupList.get(pos);

            MvcViewHolder holder;
            // 如果有的就复用，没有的就创建
            if (pos < holdersList.size()) {
                holder = holdersList.get(pos);
            } else {
                holder = item.onCreateView(parent, adapter.getItemViewType(pos));
                holdersList.add(holder);
            }

            parent.removeAllViews();
            parent.setVisibility(View.VISIBLE);
            final ViewParent prvParent = holder.itemView.getParent();
            if (prvParent != null) {
                if (prvParent != parent && prvParent instanceof ViewGroup) {
                    ((ViewGroup) prvParent).removeView(holder.itemView);
                    parent.addView(holder.itemView);
                }
            } else {
                parent.addView(holder.itemView);

            }

            adapter.bindViewHolder(holder, pos);
        }

        for (; pos < viewGroupList.size(); pos++) {
            final ViewGroup parent = viewGroupList.get(pos);
            parent.removeAllViews();
            parent.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
        onChanged();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        onChanged();
    }
}
