package com.goodix.ble.gr.toolbox.common.base;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.util.ToastUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class FragmentWrapperActivity extends BaseActivity {

    private long lastBackPressed = 0;

    protected Class<? extends Fragment> fragmentClass;
    protected Fragment fragmentInstance;
    protected boolean doubleExit = true;
    protected boolean confirmExit = false;

    private static final String EXT_DOUBLE_EXIT = "doubleExit";
    private static final String EXT_CONFIRM_EXIT = "confirmExit";
    private static final String EXT_CLASS = "fragmentClass";
    private static final String EXT_ARG = "fragmentArguments";

    public static void start(Activity ctx, Class<? extends Fragment> fragmentClass, boolean doubleExit, boolean confirmExit) {
        start(ctx, fragmentClass, doubleExit, confirmExit, null);
    }

    public static void start(Activity ctx, Class<? extends Fragment> fragmentClass, boolean doubleExit, boolean confirmExit, @Nullable Bundle arg) {
        if (ctx == null) return;

        Intent intent = new Intent(ctx, FragmentWrapperActivity.class);
        intent.putExtra(EXT_CLASS, fragmentClass);
        intent.putExtra(EXT_DOUBLE_EXIT, doubleExit);
        intent.putExtra(EXT_CONFIRM_EXIT, confirmExit);
        if (arg != null) {
            intent.putExtra(EXT_ARG, arg);
        }

        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setId(android.R.id.content);
        root.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        try {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra(EXT_CLASS)) {
                //getClassLoader().loadClass(intent.getStringExtra(EXT_CLASS));
                //noinspection unchecked
                fragmentClass = (Class<? extends Fragment>) intent.getSerializableExtra(EXT_CLASS);
                doubleExit = intent.getBooleanExtra(EXT_DOUBLE_EXIT, true);
                confirmExit = intent.getBooleanExtra(EXT_CONFIRM_EXIT, false);
            }

            if (fragmentInstance == null) {
                if (fragmentClass != null) {
                    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                        if (fragmentClass.equals(fragment.getClass())) {
                            fragmentInstance = fragment;
                            break;
                        }
                    }

                    if (fragmentInstance == null) {
                        fragmentInstance = fragmentClass.newInstance();
                    }
                }
            }

            if (fragmentInstance instanceof AbstractBleFragment) {
                ActionBar actionBar = getSupportActionBar();
                AbstractBleFragment bleFragment = (AbstractBleFragment) fragmentInstance;
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    int strResId = bleFragment.getToolBarTitle();
                    if (strResId != 0) {
                        actionBar.setTitle(strResId);
                    } else {
                        String title = bleFragment.getTabTitle();
                        if (title == null) {
                            title = fragmentInstance.getTag();
                        }
                        if (title == null) {
                            title = fragmentClass.getSimpleName().replaceFirst("Fragment$", "");
                        }
                        actionBar.setTitle(title);
                    }
                }
            }

            if (intent != null) {
                if (intent.hasExtra(EXT_ARG)) {
                    fragmentInstance.setArguments(intent.getBundleExtra(EXT_ARG));
                } else {
                    fragmentInstance.setArguments(intent.getExtras());
                }
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragmentInstance)
                    .commitNow();
        } catch (Throwable e) {
            TextView errTv = new TextView(this);
            errTv.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            errTv.setTextColor(Color.BLACK);
            errTv.setTextSize(16f);
            errTv.setTextIsSelectable(true);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            PrintStream ps = new PrintStream(byteArrayOutputStream);
            e.printStackTrace(ps);
            errTv.setText(new String(byteArrayOutputStream.toByteArray()));

            int margin = (int) (getResources().getDisplayMetrics().density * 12);
            root.setPadding(margin, margin, margin, margin);
            root.addView(errTv);
        }
    }

    @Override
    public void onBackPressed() {
        if (confirmExit) {
            showConfirmExit();

        } else {
            if (doubleExit) {
                long now = System.currentTimeMillis();
                if (now - lastBackPressed > 2000) {
                    lastBackPressed = now;
                    ToastUtil.info(this, R.string.common_exit_tip).show();
                    return;
                }
            }

            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (confirmExit) {
                showConfirmExit();
            } else {
                this.finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showConfirmExit() {
        ToastUtil.dialog(this, R.string.libuihelper_confirm_close)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
