package com.goodix.ble.libuihelper.input;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SeekBarAndCheckBoxHelper extends SeekBarHelper {

    @Nullable
    private CheckBox enableCb;

    public SeekBarAndCheckBoxHelper(@NonNull SeekBar seekBar) {
        this(seekBar, null, null);
    }

    public SeekBarAndCheckBoxHelper(@NonNull SeekBar seekBar, @Nullable TextView valueTv) {
        this(seekBar, valueTv, null);
    }

    public SeekBarAndCheckBoxHelper(@NonNull final SeekBar seekBar, @Nullable TextView valueTv, @Nullable CheckBox enableCb) {
        super.attachView(seekBar, valueTv);
        this.enableCb = enableCb;

        if (enableCb != null) {
            enableCb.setOnCheckedChangeListener(checkedChangeListener);
            checkedChangeListener.onCheckedChanged(enableCb, enableCb.isChecked());
        }
    }

    @Override
    public void setEnabled(boolean enable) {
        if (enable) {
            if (enableCb != null) {
                enableCb.setEnabled(true);
                checkedChangeListener.onCheckedChanged(enableCb, enableCb.isChecked());
            } else {
                super.setEnabled(true);
            }
        } else {
            if (enableCb != null) {
                enableCb.setEnabled(false);
            }
            super.setEnabled(false);
        }
    }

    public boolean isChecked() {
        return enableCb == null || enableCb.isChecked();
    }

    @Nullable
    public CheckBox getCheckBox() {
        return enableCb;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private CompoundButton.OnCheckedChangeListener checkedChangeListener = (buttonView, isChecked) -> SeekBarAndCheckBoxHelper.super.setEnabled(isChecked);
}
