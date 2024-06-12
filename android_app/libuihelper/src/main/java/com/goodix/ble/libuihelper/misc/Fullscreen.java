package com.goodix.ble.libuihelper.misc;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class Fullscreen {
    /**
     * 必须要在为Activity设置内容前设置全屏状态
     */
    public static void setup(Activity act) {
        // 隐藏程序的标题栏
        act.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏系统标题栏
        final Window window = act.getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 隐藏虚拟返回键（系统导航）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final WindowManager.LayoutParams params = window.getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        // 如果有导航栏，隐藏导航栏
        if (act instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) act).getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
    }
}
