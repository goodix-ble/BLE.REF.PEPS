package com.goodix.ble.libuihelper.config;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

@SuppressWarnings("unused")
public interface IConfigItem {

    /**
     * This method will be called before {@link IConfigItem#getContentView()}
     */
    void onCreate(Activity host, ViewGroup container);

    void onCreate(Fragment host, ViewGroup container);

    /**
     * return a view to be added to container layout.
     * return same instance during every calling.
     */
    View getContentView();

    /**
     * return the name of the item for human readable.
     */
    String getName();

    void onResume();

    void onPause();

    /**
     * Be similar to Fragment. Called by the host activity or fragment.
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);

    void onDestroy();
}
