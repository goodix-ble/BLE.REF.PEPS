package com.goodix.ble.libuihelper.config;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;

public class ConfigItemMgr implements IConfigItem, LifecycleEventObserver {
    private ArrayList<IConfigItem> itemList = new ArrayList<>(32);
    private ViewGroup mContainer = null;
    private Activity mHost = null;
    @Nullable
    private Fragment hostFragment = null;

    private static final int STATE_CREATE = 1;
    private static final int STATE_RESUME = 2;
    private static final int STATE_PAUSE = 3;
    private static final int STATE_DESTROY = 4;
    private int mState = 0;

    public ConfigItemMgr add(IConfigItem item) {
        itemList.add(item);
        if (mState >= STATE_CREATE) {
            item.onCreate(mHost, mContainer);
        }
        if (mState >= STATE_RESUME) {
            item.onResume();
        }
        if (mState >= STATE_PAUSE) {
            item.onPause();
        }
        if (mState >= STATE_DESTROY) {
            item.onDestroy();
        }
        return this;
    }

    public ConfigItemMgr remove(IConfigItem item) {
        if (itemList.remove(item)) {
            View view = item.getContentView();
            if (view != null) {
                ViewParent parent = view.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(view);
                }
                view.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public ConfigItemMgr attachTo(ViewGroup container) {
        mContainer = container;
        for (IConfigItem item : itemList) {
            View v = item.getContentView();
            if (v != null && v.getParent() == null) {
                container.addView(v);
            }
        }
        return this;
    }

    public int getCount() {
        return itemList.size();
    }

    public IConfigItem getItem(int pos) {
        if (pos < 0 || pos > itemList.size()) return null;
        return itemList.get(pos);
    }

    @Override
    public void onCreate(Activity host, ViewGroup container) {
        mHost = host;
        mContainer = container;
        mState = STATE_CREATE;
        for (IConfigItem item : itemList) {
            item.onCreate(host, container);
        }
    }

    @Override
    final public void onCreate(Fragment host, ViewGroup container) {
        hostFragment = host;
        mHost = host.requireActivity();
        mContainer = container;
        mState = STATE_CREATE;
        for (IConfigItem item : itemList) {
            item.onCreate(host, container);
        }
    }

    @Override
    public View getContentView() {
        return null;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void onResume() {
        mState = STATE_RESUME;
        for (IConfigItem item : itemList) {
            item.onResume();
        }
    }

    @Override
    public void onPause() {
        mState = STATE_PAUSE;
        for (IConfigItem item : itemList) {
            item.onPause();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (IConfigItem item : itemList) {
            item.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        mState = STATE_DESTROY;
        for (IConfigItem item : itemList) {
            item.onDestroy();
        }
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_RESUME:
                onResume();
                break;
            case ON_PAUSE:
                onPause();
                break;
            case ON_DESTROY:
                onDestroy();
                break;
            default:
        }
    }
}
