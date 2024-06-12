package com.goodix.ble.gr.toolbox.common.base;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.goodix.ble.gr.toolbox.common.util.AppUtils;
import com.goodix.ble.gr.toolbox.common.util.LocalManageUtil;

/**
 * Created by yuanmingwu on 18-8-22.
 */

public class BaseActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(AppUtils.getThemeId(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(AppUtils.getThemeAccentColor(this));
        }

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocalManageUtil.setLocal(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
