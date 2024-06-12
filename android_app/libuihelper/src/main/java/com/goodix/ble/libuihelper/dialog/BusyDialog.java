package com.goodix.ble.libuihelper.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StringRes;

@SuppressWarnings({"WeakerAccess", "unused"})
public class BusyDialog extends AbsDelayedDialog {

    protected final TextView tipsTv;

    public BusyDialog(Context ctx) {
        LinearLayout ll = new LinearLayout(ctx);
        int dp12 = (int) (ctx.getResources().getDisplayMetrics().density * 12);
        ll.setPadding(dp12, dp12, dp12, dp12);
        ll.setGravity(Gravity.CENTER);
        ll.addView(new ProgressBar(ctx));
        ll.addView(tipsTv = new TextView(ctx));

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setCancelable(false);
        builder.setView(ll);
        dialog = builder.create();
    }

    public void setTips(@StringRes int resId) {
        tipsTv.setText(resId);
    }

    public void setTips(CharSequence msg) {
        tipsTv.setText(msg);
    }
}
