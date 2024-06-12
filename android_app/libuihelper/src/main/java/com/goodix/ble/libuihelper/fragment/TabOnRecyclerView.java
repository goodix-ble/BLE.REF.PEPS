package com.goodix.ble.libuihelper.fragment;

import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libuihelper.sublayout.list.IMvcListView;
import com.goodix.ble.libuihelper.sublayout.list.MvcAdapter;

class TabOnRecyclerView implements IMvcListView {
    private MvcAdapter adapter;
    private RecyclerView tabContainerView;

    public TabOnRecyclerView(RecyclerView tabContainerView) {
        this.tabContainerView = tabContainerView;
    }

    @Override
    public void setAdapter(MvcAdapter adapter) {
        this.adapter = adapter;
        if (tabContainerView != null) {
            if (tabContainerView.getLayoutManager() == null) {
                tabContainerView.setLayoutManager(new LinearLayoutManager(tabContainerView.getContext(), RecyclerView.HORIZONTAL, false));
            }
            tabContainerView.setAdapter(adapter);
        }
    }

    @Override
    public MvcAdapter getAdapter() {
        return adapter;
    }

    @Override
    public ViewGroup.LayoutParams generateItemLayoutParams(int position) {
        ViewGroup.LayoutParams lp = null;
        if (tabContainerView != null && tabContainerView.getLayoutManager() != null) {
            RecyclerView.LayoutManager layoutManager = tabContainerView.getLayoutManager();
            if (layoutManager != null) {
                lp = layoutManager.generateDefaultLayoutParams();
                if (layoutManager instanceof LinearLayoutManager) {
                    if (((LinearLayoutManager) layoutManager).getOrientation() == RecyclerView.HORIZONTAL) {
                        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    } else {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    }
                }
            }
        }
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return lp;
    }

    @Override
    public void scrollToPosition(int position, boolean smooth) {
        if (tabContainerView == null) {
            return;
        }

        if (smooth) {
            tabContainerView.smoothScrollToPosition(position);
        } else {
            tabContainerView.scrollToPosition(position);
        }
    }
}
