package com.goodix.ble.gr.toolbox.common.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class SettingSave {

    private final String SP_NAME = "setting";
    private final String TAG_LANGUAGE = "language_select";
    private final String TAG_THEME = "theme_select";
    private static volatile SettingSave instance;

    private final SharedPreferences mSharedPreferences;

    private Locale systemCurrentLocal;


    public SettingSave(Context context) {
        mSharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            systemCurrentLocal = LocaleList.getDefault().get(0);
        } else {
            systemCurrentLocal = Locale.getDefault();
        }
    }


    public void saveLanguage(int select) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putInt(TAG_LANGUAGE, select);
        edit.commit();
    }

    public void saveTheme(int select) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putInt(TAG_THEME, select);
        edit.commit();
    }

    public int getSelectLanguage() {
        return mSharedPreferences.getInt(TAG_LANGUAGE, 0);
    }


    public int getSelectTheme() {
        return mSharedPreferences.getInt(TAG_THEME, 0);
    }

    public Locale getSystemCurrentLocal() {
        return systemCurrentLocal;
    }

    public void setSystemCurrentLocal(Locale local) {
        systemCurrentLocal = local;
    }

    public static SettingSave getInstance(Context context) {
        if (instance == null) {
            synchronized (SettingSave.class) {
                if (instance == null) {
                    instance = new SettingSave(context);
                }
            }
        }
        return instance;
    }

}
