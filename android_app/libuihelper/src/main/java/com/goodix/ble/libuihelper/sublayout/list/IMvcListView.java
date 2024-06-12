package com.goodix.ble.libuihelper.sublayout.list;

import android.view.ViewGroup;

/**
 * 核心职责：
 * 1. 获得数据的数量
 * 2. 复用或创建可复用的视图
 * 3. 将需要展示的数据绑定到视图上
 * 4. 监听Adapter的变化，及时重复上面的3个步骤，更新视图
 */
public interface IMvcListView {
    void setAdapter(MvcAdapter adapter);

    MvcAdapter getAdapter();

    ViewGroup.LayoutParams generateItemLayoutParams(int position);

    void scrollToPosition(int position, boolean smooth);
}
