package com.goodix.ble.libuihelper.config;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public abstract class AbsConfigItem implements IConfigItem {

    private Activity hostActivity;
    @Nullable
    private Fragment hostFragment = null;
    private View contentView;
    private ViewGroup container;
    protected Handler uiHandler = new Handler(Looper.getMainLooper());
    protected Thread uiThread = Looper.getMainLooper().getThread();

    protected abstract void onCreate(ViewGroup container);

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    final public void onCreate(Activity host, ViewGroup container) {
        hostActivity = host;
        this.container = container;
        onCreate(container);
    }

    @Override
    final public void onCreate(Fragment host, ViewGroup container) {
        hostFragment = host;
        onCreate(host.requireActivity(), container);
    }

    @Override
    final public View getContentView() {
        return contentView;
    }

    @Override
    public void onResume() {
        // empty
    }

    @Override
    public void onPause() {
        // empty
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // empty
    }

    protected void setContentView(@LayoutRes int layout) {
        if (contentView != null) {
            throw new IllegalStateException("contentView is already set.");
        }

        if (hostActivity == null) {
            throw new IllegalStateException("hostActivity is null. Must call super.onCreate() first.");
        }

        contentView = LayoutInflater.from(hostActivity).inflate(layout, container, false);
    }

    public final <T extends View> T findViewById(@IdRes int id) {
        return contentView.findViewById(id);
    }

    protected void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != uiThread) {
            uiHandler.post(action);
        } else {
            action.run();
        }
    }

    protected void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        if (hostFragment != null) {
            hostFragment.startActivityForResult(intent, requestCode, options);
        } else if (hostActivity != null) {
            hostActivity.startActivityForResult(intent, requestCode, options);
        }
    }

    public final Context getContext() {
        Context ctx = getHostActivity();
        if (ctx == null) {
            if (contentView != null) {
                ctx = contentView.getContext();
            } else if (container != null) {
                ctx = container.getContext();
            }
        }
        return ctx;
    }

    public Activity getHostActivity() {
        if (hostFragment != null) {
            // Try to retrieve newer host activity.
            FragmentActivity act = hostFragment.getActivity();
            if (act != null) {
                hostActivity = act;
            }
        }
        return hostActivity;
    }

    @Nullable
    public Fragment getHostFragment() {
        return hostFragment;
    }

    public final String getString(@StringRes int resId) {
        return hostActivity.getString(resId);
    }

    public final String getString(@StringRes int resId, Object... formatArgs) {
        return hostActivity.getString(resId, formatArgs);
    }
}
