package com.goodix.ble.gr.toolbox.common.base;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;

import com.goodix.ble.gr.toolbox.common.R;

import java.util.Objects;

public abstract class AbsSettingsActivity extends BaseActivity {

    /**
     * 返回需要显示的设置的Fragment，内部自动判断是否是 v4 的Fragment
     */
    protected abstract Object onCreateContentFragment();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_settings);

        Object fragment = onCreateContentFragment();
        if (fragment != null) {
            if (fragment instanceof Fragment) {
                getSupportFragmentManager().beginTransaction().replace(R.id.content, (Fragment) fragment).commit();
            } else if (fragment instanceof android.app.Fragment) {
                getFragmentManager().beginTransaction().replace(R.id.content, (android.app.Fragment) fragment).commit();
            }
        } else {
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
