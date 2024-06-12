package com.goodix.ble.gr.toolbox.common.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.goodix.ble.gr.toolbox.common.R;
import com.goodix.ble.gr.toolbox.common.ui.DottedProgressBar;
import com.goodix.ble.gr.toolbox.common.util.AppUtils;

@SuppressWarnings("WeakerAccess")
public class ConnStateDialog implements View.OnClickListener {

    private final AlertDialog alertDialog;
    private final Button reconnectBtn;
    private final DottedProgressBar progressBar;
    private final TextView statusText;
    private final View buttonBarLayout;
    private final ImageView connectIv;
    private final View connectStateLayout;
    private final View findServiceLayout;
    private final ImageView service0Iv;
    private final ImageView service1Iv;
    private final Button cancelBtn;

    private Context mCtx;

    public ConnStateDialog(Context ctx) {
        mCtx = ctx;
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        @SuppressLint("InflateParams")
        View root = LayoutInflater.from(ctx).inflate(R.layout.item_alert_connect, null);
        builder.setView(root);
        builder.setCancelable(false);
        alertDialog = builder.create();

        reconnectBtn = root.findViewById(R.id.re_connect);
        progressBar = root.findViewById(R.id.connect_progress_bar);
        statusText = root.findViewById(R.id.connect_status_text);
        buttonBarLayout = root.findViewById(R.id.button_line);
        connectIv = root.findViewById(R.id.img_disconnect);
        connectStateLayout = root.findViewById(R.id.img_connect_line);
        findServiceLayout = root.findViewById(R.id.find_service_line);
        service0Iv = root.findViewById(R.id.img_service0);
        service1Iv = root.findViewById(R.id.img_service1);

        service1Iv.setImageResource(R.mipmap.ic_service);
        AppUtils.setImageViewColor(service1Iv, mCtx);

        cancelBtn = root.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(this);
    }

    public Button getReconnectBtn() {
        return reconnectBtn;
    }

    public Button getCancelBtn() {
        return cancelBtn;
    }

    public void showConnecting() {
        findServiceLayout.setVisibility(View.INVISIBLE);
        connectStateLayout.setVisibility(View.VISIBLE);
        connectIv.setVisibility(View.INVISIBLE);
        progressBar.startProgress();
        cancelBtn.setVisibility(View.VISIBLE);
        reconnectBtn.setVisibility(View.INVISIBLE);
        statusText.setText(R.string.common_connecting);

        alertDialog.show();
    }

    public void showDiscoveringService() {
        findServiceLayout.setVisibility(View.VISIBLE);
        connectStateLayout.setVisibility(View.INVISIBLE);
        progressBar.stopProgress();
        cancelBtn.setVisibility(View.INVISIBLE);
        reconnectBtn.setVisibility(View.INVISIBLE);
        statusText.setText(R.string.common_finding_service);

        alertDialog.show();
    }

    public void showDisconnected() {
        findServiceLayout.setVisibility(View.INVISIBLE);
        connectStateLayout.setVisibility(View.VISIBLE);
        connectIv.setVisibility(View.VISIBLE);
        progressBar.stopProgress();
        cancelBtn.setVisibility(View.VISIBLE);
        reconnectBtn.setVisibility(View.VISIBLE);
        statusText.setText(R.string.common_disconnected);

        alertDialog.show();
    }

    public void showNotSupported() {
        findServiceLayout.setVisibility(View.INVISIBLE);
        connectStateLayout.setVisibility(View.VISIBLE);
        connectIv.setVisibility(View.VISIBLE);
        progressBar.stopProgress();
        cancelBtn.setVisibility(View.VISIBLE);
        reconnectBtn.setVisibility(View.VISIBLE);
        statusText.setText(R.string.common_disconnected);
        //reconnectBtn.setText(R.string.common_sure);
        statusText.setText(R.string.profile_no_services_found);

        alertDialog.show();
    }


    public void close() {
        if (alertDialog != null) {
            progressBar.stopProgress();
            alertDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        close();
    }
}
