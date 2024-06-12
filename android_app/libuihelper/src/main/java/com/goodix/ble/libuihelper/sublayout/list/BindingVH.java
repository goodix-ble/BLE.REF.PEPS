package com.goodix.ble.libuihelper.sublayout.list;

import android.view.View;

import androidx.annotation.NonNull;

public class BindingVH<T> extends MvcViewHolder {
    public T binding;

    public BindingVH(@NonNull View itemView, T binding) {
        super(itemView);
        this.binding = binding;
    }
}
