package com.goodix.ble.libuihelper.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.sublayout.ValueEditorHolder;

@SuppressWarnings({"unused", "WeakerAccess", "FieldCanBeLocal", "UnusedReturnValue"})
public class EasyInputDialog implements View.OnClickListener {

    private Context ctx;
    private AlertDialog dialog;
    private float density;

    private LinearLayout root;
    private LinearLayout inputLine;

    public final TextView titleTv;
    public final TextView captionTv;
    public final TextView prefixTv;
    public final EditText valueEd;
    public final TextView tipTv;
    public final Button okBtn;
    public final Button cancelBtn;
    public final ValueEditorHolder valueHolder;

    public EasyInputDialog(View v) {
        this(v.getContext());
    }

    @SuppressLint("InflateParams")
    public EasyInputDialog(Context ctx) {
        this.ctx = ctx;

        root = (LinearLayout) LayoutInflater.from(ctx).inflate(R.layout.libuihelper_dialog_input, null);
        titleTv = root.findViewById(R.id.libuihelper_dialog_input_title_tv);
        captionTv = root.findViewById(R.id.libuihelper_dialog_input_caption_tv);
        prefixTv = root.findViewById(R.id.libuihelper_dialog_input_prefix_tv);
        valueEd = root.findViewById(R.id.libuihelper_dialog_input_value_ed);
        tipTv = root.findViewById(R.id.libuihelper_dialog_input_tip_tv);
        okBtn = root.findViewById(R.id.libuihelper_dialog_input_ok_btn);
        cancelBtn = root.findViewById(R.id.libuihelper_dialog_input_cancel_btn);

        valueHolder = new ValueEditorHolder().attachView(valueEd);
        valueHolder.hexPrefixTv = prefixTv;
        valueHolder.captionTv = captionTv;
        valueHolder.actionBtn = okBtn;
        valueHolder.root = root;

        // 默认不显示其它的标签
        titleTv.setVisibility(View.GONE);
        captionTv.setVisibility(View.GONE);
        prefixTv.setVisibility(View.GONE);
        tipTv.setVisibility(View.GONE);

        // 按钮默认支持关闭对话框的功能
        okBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    public EasyInputDialog setTitle(String title) {
        setTextView(titleTv, title);
        return this;
    }

    public EasyInputDialog setTitle(int strResId) {
        return setTitle(ctx.getString(strResId));
    }

    public EasyInputDialog setCaption(String caption) {
        setTextView(captionTv, caption);
        return this;
    }

    public EasyInputDialog setCaption(int strResId) {
        return setCaption(ctx.getString(strResId));
    }

    public EasyInputDialog setPrefix(String prefix) {
        setTextView(prefixTv, prefix);
        return this;
    }

    public EasyInputDialog setPrefix(int strResId) {
        return setPrefix(ctx.getString(strResId));
    }

    public EasyInputDialog setHint(String hint) {
        if (hint != null) {
            valueEd.setHint(hint);
        }
        return this;
    }

    public EasyInputDialog setHint(int strResId) {
        return setHint(ctx.getString(strResId));
    }

    public EasyInputDialog setTip(String tip) {
        setTextView(tipTv, tip);
        return this;
    }

    public EasyInputDialog setTip(int strResId) {
        return setTip(ctx.getString(strResId));
    }

    public EasyInputDialog setError(String error) {
        setTextView(tipTv, error);
        tipTv.setTextColor(Color.RED);
        return this;
    }

    public EasyInputDialog hideKeyboard() {
        if (valueEd != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.hideSoftInputFromWindow(valueEd.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        return this;
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public AlertDialog show() {
        density = ctx.getResources().getDisplayMetrics().density;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setView(root);
        builder.setCancelable(true);

        dialog = builder.create();
        final Window window = dialog.getWindow();
        if (window != null) {
            // 解决在对话框上再弹出对话框时屏幕会闪烁的问题
            // https://blog.csdn.net/kongqwesd12/article/details/80775935
            window.setWindowAnimations(R.style.DialogNoAnim);
        }
        dialog.show();

        return dialog;
    }

    public ValueEditorHolder getValue() {
        return valueHolder;
    }

    private void setTextView(TextView tipTv, String tip) {
        if (tip != null) {
            tipTv.setText(tip);
            tipTv.setVisibility(View.VISIBLE);
        } else {
            tipTv.setVisibility(View.GONE);
        }
    }

//    private View createView(Context ctx) {
//        final int dp6 = dp(6);
//
//        root = new LinearLayout(ctx);
//        root.setLayoutParams(layout(true, true));
//        root.setPadding(dp6, dp6, dp6, dp(32));
//        root.setBackgroundColor(0xFFEFF3F7);
//        root.setOrientation(LinearLayout.VERTICAL);
//
//        captionTv = new TextView(ctx);
//        captionTv.setLayoutParams(layout(true, false));
//
//        inputLine = new LinearLayout(ctx);
//        inputLine.setLayoutParams(layout(true, false));
//        inputLine.setPadding(dp6, dp6, dp6, dp6);
//        inputLine.setOrientation(LinearLayout.HORIZONTAL);
//
//        prefixTv = new TextView(ctx);
//        prefixTv.setLayoutParams(layout(false, false));
//
//        inputEd = new EditText(ctx);
//        inputEd.setTextSize(prefixTv.getTextSize());
//        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
//        lp.weight = 1f;
//        inputEd.setLayoutParams(lp);
//
//        root.addView(captionTv);
//        root.addView(inputLine);
//        {
//            inputLine.addView(prefixTv);
//            inputLine.addView(inputEd);
//        }
//
//        return root;
//    }
//
//    private ViewGroup.LayoutParams layout(boolean fillWidth, boolean fillHeight) {
//        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        params.width = fillWidth ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
//        params.height = fillHeight ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
//        return params;
//    }
//
//    private int dp(int dp) {
//        return (int) (density * dp);
//    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
