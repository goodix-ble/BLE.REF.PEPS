package com.goodix.ble.gr.toolbox.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;

import com.goodix.ble.gr.toolbox.common.R;

/**
 * Created by yuanmingwu on 18-8-22.
 */

public class AppUtils {

    public static void setImageViewColor(ImageView view, Context context) {
        Drawable drawable = view.getDrawable().mutate();
        Drawable temp = DrawableCompat.wrap(drawable);
        ColorStateList colorStateList = ColorStateList.valueOf(getThemeAccentColor(context));
        DrawableCompat.setTintList(temp, colorStateList);
        view.setImageDrawable(temp);
    }

    public static void setImageViewColor(ImageView view) {
        setImageViewColor(view, getThemeAccentColor(view.getContext()));
    }

    public static void setImageViewColor(ImageView view, int color) {
        Drawable drawable = view.getDrawable().mutate();
        Drawable temp = DrawableCompat.wrap(drawable);
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        DrawableCompat.setTintList(temp, colorStateList);
        view.setImageDrawable(temp);
    }


    public static Drawable setDrawableColor(Drawable drawable, Context context) {
        Drawable temp = DrawableCompat.wrap(drawable);
        ColorStateList colorStateList = ColorStateList.valueOf(getThemeAccentColor(context));
        DrawableCompat.setTintList(temp, colorStateList);
        return temp;
    }

    public static void setImageSource(ImageView view, int resId, Context context) {
        view.setImageResource(resId);
        setImageViewColor(view, context);
    }


    public static int getThemeAccentColor(Context context) {
        TypedArray typedArray = context.getResources().obtainTypedArray(R.array.colorAccentArray);
        int[] colors = new int[typedArray.length()];
        for (int i = 0; i < typedArray.length(); i++) {
            colors[i] = typedArray.getColor(i, 0);
        }
        typedArray.recycle();
        int themeIndex = SettingSave.getInstance(context).getSelectTheme();
        return colors[themeIndex];
    }

    public static int getThemeId(Context context) {
        TypedArray typedArray;
        if (context.getClass().getSimpleName().equals("MainActivity")) {
            typedArray = context.getResources().obtainTypedArray(R.array.defineAPPStyles);
        } else {
            typedArray = context.getResources().obtainTypedArray(R.array.defineActivityStyles);
        }

        int[] theme = new int[typedArray.length()];
        for (int i = 0; i < typedArray.length(); i++) {
            theme[i] = typedArray.getResourceId(i, 0);
        }
        typedArray.recycle();
        int themeIndex = SettingSave.getInstance(context).getSelectTheme();
        return theme[themeIndex];
    }


    public static void restartApp(Activity activity, Class<?> homeClass) {
        Intent intent = new Intent(activity, homeClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        // 杀掉进程
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public static String getSelectThemeString(Context context) {
        TypedArray typedArray = context.getResources().obtainTypedArray(R.array.theme_select);
        String[] string = new String[typedArray.length()];
        for (int i = 0; i < typedArray.length(); i++) {
            string[i] = typedArray.getString(i);
        }
        typedArray.recycle();
        int themeIndex = SettingSave.getInstance(context).getSelectTheme();
        return string[themeIndex];
    }

    static Toast toast;

    public static void showCenterToast(Context context, String string) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, string, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    public static void hideSoftInputFromWindow(View v) {
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }
}
