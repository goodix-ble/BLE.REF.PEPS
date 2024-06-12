package com.goodix.ble.libuihelper.input;

import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.util.HexStringBuilder;

@SuppressWarnings({"unused"})
public class SeekBarHelper {
    public static final int EVT_VALUE_CHANGED = 974;
    private SeekBar seekBar;
    @Nullable
    private TextView valueTv;

    private Event<Integer> valueChangedEvent = new Event<>();

    private int valueMin = 0;
    private int valueMax = 100;
    private boolean invertDirection = false;

    private String valueFormat = null;
    private HexStringBuilder valueStrBuffer = null;
    private IFormatter formatter = null;

    public SeekBarHelper attachView(SeekBar seekBar, @Nullable TextView valueTv) {
        this.seekBar = seekBar;
        this.valueTv = valueTv;

        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        if (valueTv != null) {
            setFormat("%d");
        }

        return this;
    }

    public SeekBarHelper attachView(ViewGroup container, @IdRes int seekBar, @IdRes int valueTv) {
        return attachView(container.findViewById(seekBar), container.findViewById(valueTv));
    }

    public void setFormat(String format) {
        if (format == null) return;

        if (valueStrBuffer == null) {
            valueStrBuffer = new HexStringBuilder(32);
        }
        this.valueFormat = format;
        updateTextView(getValue());
    }

    public void setFormat(int resId) {
        setFormat(seekBar.getContext().getResources().getString(resId));
    }

    public void setFormat(IFormatter formatter) {
        if (formatter == null) return;

        if (valueStrBuffer == null) {
            valueStrBuffer = new HexStringBuilder(32);
        }
        this.formatter = formatter;
        updateTextView(getValue());
    }

    public void setRange(int min, int max) {
        int orgValue = getValue();

        if (min > max) {
            valueMax = min;
            valueMin = max;
        } else {
            valueMin = min;
            valueMax = max;
        }
        seekBar.setMax(valueMax - valueMin);

        // 修改范围后，及时同步SeekBar显示上的修改
        setValue(orgValue);
    }

    public void setInvertDirection(boolean invert) {
        int orgValue = getValue();

        invertDirection = invert;

        // 修改后，及时同步SeekBar显示上的修改
        setValue(orgValue);
    }

    public void setValue(int newVal) {
        if (newVal < valueMin) newVal = valueMin;
        if (newVal > valueMax) newVal = valueMax;
        updateSeekBar(newVal);
    }

    public void setEnabled(boolean enable) {
        seekBar.setEnabled(enable);
    }

    private void updateSeekBar(int newVal) {
        int offset = newVal - valueMin;
        if (invertDirection) {
            int width = valueMax - valueMin;
            seekBar.setProgress(width - offset);
        } else {
            seekBar.setProgress(offset);
        }
    }

    public int getValue() {
        int offset = seekBar.getProgress();
        if (invertDirection) {
            int width = valueMax - valueMin;
            offset = width - offset;
        }
        return offset + valueMin;
    }

    public Event<Integer> evtValueChanged() {
        return valueChangedEvent;
    }

    @NonNull
    public SeekBar getSeekBar() {
        return seekBar;
    }

    @Nullable
    public TextView getValueTv() {
        return valueTv;
    }

    private void updateTextView(int value) {
        if (valueTv != null) {
            if (formatter != null) {
                valueStrBuffer.clear();
                formatter.format(this, value, valueStrBuffer);
                valueTv.setText(valueStrBuffer);
            } else if (valueFormat != null) {
                valueStrBuffer.clear();
                valueStrBuffer.format(valueFormat, value);
                valueTv.setText(valueStrBuffer);
            }
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int value = getValue();
            updateTextView(value);
            valueChangedEvent.postEvent(SeekBarHelper.this, EVT_VALUE_CHANGED, value);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    public interface IFormatter {
        void format(SeekBarHelper helper, int value, HexStringBuilder out);
    }
}
