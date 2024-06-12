package com.goodix.ble.gr.toolbox.common.test;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.util.ToastUtil;

public class BaseTestActivity extends AppCompatActivity {
    private boolean showVersion = true;

    public void showVersion(boolean show) {
        this.showVersion = show;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.ActivityThemeBlue);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorAccentBlue));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (showVersion) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                MenuItem item = menu.add(versionName);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        ToastUtil.dialog(this, R.string.libuihelper_confirm_close)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
