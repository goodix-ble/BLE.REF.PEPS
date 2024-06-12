package com.goodix.ble.libuihelper.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libuihelper.R;
import com.google.android.material.tabs.TabLayout;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ClosableTabFragment extends DialogFragment implements TabMgrFragment.ITabItem, TabContainer.ITabItem {
    public static final int EVT_CLOSE = 560;

    @Nullable
    private TabContainer tabContainer;

    @Nullable
    protected TextView tabTitleTv;
    @Nullable
    protected TextView tabDescTv;
    @Nullable
    protected ImageView tabCloseIv;

    protected boolean confirmClose = false;
    protected boolean isClosable = true;
    protected int showMode = 0; // 0: wrap content; 1: maximize; 2: fullscreen
    protected String tabTitle = null; // 保存一份，等获得TextView的时候再设置
    protected String tabDesc = null;

    private Event<Void> eventClose = new Event<>(this, EVT_CLOSE);

    public Event<Void> evtClose() {
        if (eventClose == null) {
            synchronized (this) {
                if (eventClose == null) {
                    eventClose = new Event<>(this, EVT_CLOSE);
                }
            }
        }
        return eventClose;
    }

    @Nullable
    public TabContainer getTabContainer() {
        return tabContainer;
    }

    @Nullable
    public String getTabTitle() {
        return tabTitle;
    }

    @Nullable
    public String getTabDesc() {
        return tabDesc;
    }

    public ClosableTabFragment setTabTitle(String tabTitle) {
        this.tabTitle = tabTitle;
        if (tabTitleTv != null) {
            tabTitleTv.setText(tabTitle);
        }
        return this;
    }

    public ClosableTabFragment setTabDesc(String tabDesc) {
        this.tabDesc = tabDesc;
        if (tabDescTv != null) {
            tabDescTv.setText(tabDesc);
            tabDescTv.setVisibility(tabDesc == null ? View.GONE : View.VISIBLE);
        }
        return this;
    }

    public ClosableTabFragment setClosable(boolean closable) {
        isClosable = closable;
        if (tabCloseIv != null) {
            if (isClosable) {
                tabCloseIv.setOnClickListener(v -> {
                    if (onShowConfirmClose()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle(R.string.libuihelper_confirm_close)
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    } else {
                        finish();
                    }
                });
                tabCloseIv.setVisibility(View.VISIBLE);
            } else {
                tabCloseIv.setVisibility(View.GONE);
            }
        }
        return this;
    }

    /**
     * Show dialog in fullscreen mode. It looks like an activity.
     */
    public ClosableTabFragment setFullscreen() {
        showMode = 2;
        return this;
    }

    /**
     * Show dialog in maximized mode. It looks like a dialog.
     */
    public ClosableTabFragment setMaximize() {
        showMode = 1;
        return this;
    }

    @SuppressLint("InflateParams")
    @Override
    public void onBindTab(Context ctx, TabLayout.Tab tab, int position) {
        tab.setCustomView(onBindTab(null, ctx, null, position));
    }

    @Override
    public void onUnbindTab(TabLayout.Tab tab, int position) {
    }

    @Override
    public View onBindTab(TabContainer tabContainer, Context ctx, ViewGroup parent, int position) {
        this.tabContainer = tabContainer;
        final View view = LayoutInflater.from(ctx).inflate(R.layout.libuihelper_closable_tab, parent, false);

        tabCloseIv = view.findViewById(R.id.libuihelper_closable_tab_close_iv);
        setClosable(isClosable);

        tabTitleTv = view.findViewById(R.id.libuihelper_closable_tab_name_tv);
        tabDescTv = view.findViewById(R.id.libuihelper_closable_tab_desc_tv);

        if (tabTitleTv != null) {
            if (tabTitle != null) {
                tabTitleTv.setText(tabTitle);
            } else if (getTag() != null) {
                tabTitleTv.setText(getTag());
            } else {
                tabTitleTv.setText(this.getClass().getSimpleName().replaceFirst("Fragment$", ""));
            }
        }

        if (tabDescTv != null && tabDesc != null) {
            if (tabDescTv.getVisibility() != View.VISIBLE) {
                tabDescTv.setVisibility(View.VISIBLE);
            }
            tabDescTv.setText(tabDesc);
        }
        return view;
    }

    @Override
    public void onUnbindTab(View tab, int position) {

    }

    @Override
    public void onSelectionChanged(boolean selected) {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();

        if (window != null) {
            if (showMode == 2) {
                window.setGravity(Gravity.CENTER); //可设置dialog的位置
                window.getDecorView().setPadding(0, 0, 0, 0); //消除边距
                window.getDecorView().setBackgroundColor(0xFFFFFFFF); // 一定要设置背景才能填满屏幕
                window.setWindowAnimations(android.R.style.Animation_Dialog);

                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;   //设置宽度充满屏幕
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                window.setAttributes(lp);
            }
        }

        if (tabTitle != null) {
            dialog.setTitle(tabTitle);
        }
        dialog.setCancelable(isClosable);
        dialog.setCanceledOnTouchOutside(isClosable);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        // 最大化显示对话框
        if (showMode == 1) {
            // 一定要在 show() 之后才有用
            // 当 contentView 的布局参数有 match_parent 的时候，如果不指定 dialog 的大小，会导致严重的布局性能问题
            // decorView 的 wrap_content 会和 contentView 的 match_parent 冲突。
            final Dialog dialog = getDialog();
            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.width = displayMetrics.widthPixels;   //设置宽度充满屏幕
                    lp.height = displayMetrics.heightPixels;
                    window.setAttributes(lp);
                }
            }
        }
    }

    public void finish() {
        final Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof TabMgrFragment) {
            ((TabMgrFragment) parentFragment).deleteFragment(ClosableTabFragment.this);
        } else if (tabContainer != null) {
            tabContainer.removeFragment(ClosableTabFragment.this);
        }

        if (eventClose != null) {
            eventClose.postEvent(null);
        }
    }

    /**
     * Show confirm dialog ?
     *
     * @return true - need confirm, false - close directly
     */
    protected boolean onShowConfirmClose() {
        return confirmClose;
    }
}
