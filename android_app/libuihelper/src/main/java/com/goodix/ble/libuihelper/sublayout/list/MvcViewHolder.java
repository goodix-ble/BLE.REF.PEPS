package com.goodix.ble.libuihelper.sublayout.list;

import android.util.SparseArray;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MvcViewHolder extends RecyclerView.ViewHolder {
    MvcController bindController;
    private SparseArray<View> views;

    public MvcViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public <T extends View> T findViewById(@IdRes int id) {
        if (id == View.NO_ID) {
            return null;
        }
        if (views == null) {
            this.views = new SparseArray<>(16);
        }
        View view = views.get(id);
        if (view == null) {
            view = itemView.findViewById(id);
            views.put(id, view);
        }
        //noinspection unchecked
        return (T) view;
    }
}
