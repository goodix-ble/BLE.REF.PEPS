package com.goodix.ble.gr.toolbox.common.base;

import android.app.Application;

import com.orhanobut.hawk.Hawk;

public class BaseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Hawk.init(this).build();
    }

}