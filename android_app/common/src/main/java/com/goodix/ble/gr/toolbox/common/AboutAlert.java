package com.goodix.ble.gr.toolbox.common;

import android.app.AlertDialog;
import android.content.Context;


/**
 * Created by yuanmingwu on 17-7-5.
 */

public class AboutAlert {

    public static void showAboutAlert(Context context, String content) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.common_about);//文字
        builder.setMessage(content);
        builder.setPositiveButton(R.string.common_sure, null);
        builder.show();
    }

    public static void showAboutAlert(Context context, int stringId) {
        showAboutAlert(context, context.getString(stringId));
    }

}
