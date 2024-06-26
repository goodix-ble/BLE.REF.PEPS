package com.goodix.ble.libuihelper.sublayout.list;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * 根据一个数据集合，动态创建视图并添加到线性布局中。
 * 可用于创建带折叠功能的多级列表。
 */
public class LinearLayoutHelper {
    private LinearLayout layout;
    private MvcAdapter adapter;
    private ArrayList<MvcViewHolder> vhList = new ArrayList<>();
    private boolean reuseView;

    public LinearLayoutHelper(LinearLayout layout) {
        this.layout = layout;
    }

    public LinearLayout getLayout() {
        return layout;
    }

    public void setReuseView(boolean reuseView) {
        this.reuseView = reuseView;
    }

    public void setAdapter(MvcAdapter adapter) {

        // 原有的Adapter不为NULL时，销毁已有的VH
        if (this.adapter != null) {
            for (MvcViewHolder vh : vhList) {
                if (vh.bindController != null) {
                    Log.v("MvcList", "  remove from linear layout: #" + vh.bindController.position + "  @" + vh.hashCode());
                    //noinspection unchecked
                    vh.bindController.attach(false, vh); // 解除关联
                }
            }
        }

        this.adapter = adapter;

        // 移除视图
        layout.removeAllViews();

        // 设置空Adapter时，就清除所有信息
        if (adapter == null) {
            vhList.clear();
            return;
        } else {
            vhList.ensureCapacity(adapter.getItemCount());
            if (!reuseView) {
                vhList.clear();
            }
        }

        // 准备布局参数
        LinearLayout.LayoutParams layoutParams;
        if (layout.getOrientation() == LinearLayout.VERTICAL) {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // 绑定视图和控制器
        int reuseCnt = 0;
        boolean reuseViewFirst = reuseView;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            MvcViewHolder vh = null;
            // 如果启用了重用功能，就从已经创建的VH中取用
            if (reuseViewFirst && reuseCnt < vhList.size()) {
                vh = vhList.get(reuseCnt++);
            }
            Log.v("MvcList", "  add to linear layout: #" + i + "  reuse: " + (vh != null));
            // 如果没有取用到VH，就创建一个新的
            if (vh == null) {
                reuseViewFirst = false; // 如果开始创建了，那么就不能再reuse了，否则会把刚添加进去的给reuse起来，导致错误。
                vh = adapter.onCreateViewHolder(layout, adapter.getItemViewType(i));
                vhList.add(vh);
            }
            // 添加到视图中，并显示
            layout.addView(vh.itemView, layoutParams);
            adapter.onBindViewHolder(vh, i);
        }
    }

    public MvcAdapter getAdapter() {
        return adapter;
    }
}
