package com.goodix.ble.libuihelper.sublayout;

import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.goodix.ble.libcomx.util.HexEndian;
import com.goodix.ble.libcomx.util.HexStringParser;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.filter.HexInputFilter;
import com.goodix.ble.libuihelper.input.slider.SlideToInputHelper;

import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused", "FieldCanBeLocal", "UnusedReturnValue"})
public class ValueSliderHolder implements ISubLayoutHolder<ValueSliderHolder>
        , SeekBar.OnSeekBarChangeListener
        , TextWatcher
        , Runnable {

    @Nullable
    public View root;
    @Nullable
    public TextView captionTv;
    @Nullable
    public EditText valueEd;
    @Nullable
    public Button setBtn;
    @Nullable
    public SeekBar seekBar;

    private boolean isHexInput = false;
    private String hexFormat;
    private int curValue;
    private int valueBeforeNotify; // 用于在延时通知的时候，确保通知的值是不同于通知前的。因为有可能在通知变更前，又被用户改回了原来的值。
    private final static int MAX_HEX_SIZE = 4;
    private byte[] hexBuf = new byte[MAX_HEX_SIZE];

    private ValueSliderChangedListener valueChangedListener;
    private final static int NOTIFY_DELAY_TIME = 500; // millisecond
    private Handler delayTimer;
    private int delayTime = NOTIFY_DELAY_TIME;

    private int minValue;
    private int maxValue;

    private SlideToInputHelper slideInputHelper;

    private boolean limitEditorInput;
    private int maxEditorInputValue;

    public interface ValueSliderChangedListener {
        void OnValueSliderChanged(ValueSliderHolder slider, int value);
    }

    /**
     * 仅使用核心控件
     */
    public ValueSliderHolder attachView(EditText valueEd, SeekBar seekBar) {
        this.seekBar = seekBar;

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(this);
            // 进行基本的控件设置，让其初始可用。初始为数字输入
            seekBar.setMax(255);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                seekBar.setMin(0);
            }
        }

        return attachEditText(valueEd);
    }

    public ValueSliderHolder attachEditText(EditText valueEd) {
        this.valueEd = valueEd;

        if (valueEd != null) {
            valueEd.addTextChangedListener(this);
            valueEd.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        }

        return this;
    }

    public ValueSliderHolder attachView(ViewGroup container, @IdRes int valueEd, @IdRes int seekBar) {
        return attachView(container.findViewById(valueEd), container.findViewById(seekBar));
    }

    public ValueSliderHolder attachView(ViewGroup container, @IdRes int includeLayoutId) {
        return attachView((View) container.findViewById(includeLayoutId));
    }

    /**
     * 使用默认的根布局ID
     */
    @Override
    public ValueSliderHolder attachView(@Nullable View root) {
        this.root = root;

        if (root instanceof EditText) {
            return attachEditText((EditText) root);
        }

        if (root != null) {
            captionTv = root.findViewById(R.id.sublayout_caption_tv);
            seekBar = root.findViewById(R.id.sublayout_value_slider_seekbar);
            valueEd = root.findViewById(R.id.sublayout_value_ed);
            setBtn = root.findViewById(R.id.sublayout_set_btn);
        }

        if (captionTv == null && seekBar == null && valueEd == null && setBtn == null) {
            if (root instanceof ViewGroup) {
                ViewGroup container = (ViewGroup) root;
                for (int i = 0; i < container.getChildCount(); i++) {
                    final View child = container.getChildAt(i);
                    if (captionTv == null && child instanceof TextView) {
                        captionTv = (TextView) child;
                    } else if (seekBar == null && child instanceof SeekBar) {
                        seekBar = (SeekBar) child;
                    } else if (valueEd == null && child instanceof EditText) {
                        valueEd = (EditText) child;
                    } else if (setBtn == null && child instanceof Button) {
                        setBtn = (Button) child;
                    }
                }
            }
        }

        return attachView(valueEd, seekBar);
    }

    public ValueSliderHolder inflate(LayoutInflater inflater, @LayoutRes int resource, @Nullable ViewGroup container) {
        return this.inflate(inflater, container, resource);
    }

    public ValueSliderHolder inflate(LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int resource) {
        View view = inflater.inflate(resource, container, false);
        attachView(view);
        if (container != null) {
            container.addView(view);
        }
        return this;
    }

    @Override
    public ValueSliderHolder setEnabled(boolean enabled) {
        if (root != null) {
            root.setEnabled(enabled);
        }
        if (valueEd != null) {
            valueEd.setEnabled(enabled);
        }
        if (setBtn != null) {
            setBtn.setEnabled(enabled);
        }
        if (seekBar != null) {
            seekBar.setEnabled(enabled);
        }
        return this;
    }

    @Override
    public ValueSliderHolder setVisibility(int visibility) {
        if (root != null) {
            root.setVisibility(visibility);
        }
        return this;
    }

    @Override
    public ValueSliderHolder setOnClickListener(View.OnClickListener l) {
        if (setBtn != null) {
            setBtn.setOnClickListener(l);
        }
        return this;
    }

    /**
     * 启用十六进制输入方式
     *
     * @param size 指定需要输入的十六进制字节数，可选值为 1、2、4 。当输入为 1 时，文本框会使用 %02X 的格式
     */
    public ValueSliderHolder enableHexInput(int size) {
        if (size < 1) size = 1;
        if (size > MAX_HEX_SIZE) size = MAX_HEX_SIZE;
        hexFormat = String.format(Locale.US, "%%0%dX", size * 2);
        isHexInput = true;
        if (valueEd != null) {
            valueEd.setHint("HEX");
            valueEd.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            valueEd.setFilters(new InputFilter[]{new HexInputFilter(), new InputFilter.LengthFilter(size * 2)});
        }
        return this;
    }

    /**
     * 设置用于监听值变化事件的监听器
     *
     * @param timer 提供可以复用的实例，传入null表示不使用延时
     */
    public ValueSliderHolder setValueChangeListener(@Nullable Handler timer, ValueSliderChangedListener listener) {
        if (delayTimer != null) {
            delayTimer.removeCallbacks(this);
        }
        delayTimer = timer;
        valueChangedListener = listener;
        // delayTime = NOTIFY_DELAY_TIME; 这条语句导致 setNotifyDelay() 必须在该函数调用后才能调用。
        return this;
    }

    public ValueSliderHolder setNotifyDelay(int time) {
        delayTime = time;
        return this;
    }

    public ValueSliderHolder setRange(int min, int max) {

        if (min > max) {
            maxValue = min;
            minValue = max;
        } else {
            maxValue = max;
            minValue = min;
        }

        if (seekBar != null) {
            seekBar.setMax(maxValue - minValue);
        }

        return this;
    }

    public ValueSliderHolder setEditorMaxInputValue(int maxValue) {
        limitEditorInput = true;
        maxEditorInputValue = maxValue;
        return this;
    }

    public SlideToInputHelper setUseSlideInput() {
        if (slideInputHelper == null) {
            slideInputHelper = new SlideToInputHelper();
            if (valueEd != null) {
                slideInputHelper.attach(valueEd);
            }
            slideInputHelper.setListener(new SlideToInputHelper.SlideListener() {
                int orgValue;

                @Override
                public void onSlideToInputStarted() {
                    orgValue = getValue();
                }

                @Override
                public void onSlideToInputSliding(int delta) {
                    int value = orgValue + delta;
                    if (value < minValue) value = minValue;
                    if (value > maxValue) value = maxValue;
                    setValue(value);
                }

                @Override
                public void onSlideToInputFinished(int delta) {
                    onSlideToInputSliding(delta);
                }
            });
        }
        return slideInputHelper;
    }

    @Override
    public ValueSliderHolder setCaption(int resId) {
        if (captionTv != null) {
            captionTv.setText(resId);
        }
        return this;
    }

    @Override
    public ValueSliderHolder noButton() {
        if (setBtn != null) {
            setBtn.setVisibility(View.GONE);
        }
        return this;
    }

    @Override
    public ValueSliderHolder setCaption(CharSequence text) {
        if (captionTv != null) {
            captionTv.setText(text);
        }
        return this;
    }

    public int getValue() {
        return curValue;
    }

    public ValueSliderHolder setValue(int v) {
        // 更新文本框，会通过变更回调自动地更新滑动条
        if (valueEd != null) {
            String newValue = isHexInput ? String.format(Locale.US, hexFormat, v) : String.valueOf(v);
            // 阻止循环设置：setValue()通知外部数值变更，外部又返回来重新setValue()，此时如果这里不阻断，就会产生循环
            if (valueEd.getText().toString().compareTo(newValue) != 0) {
                valueEd.setText(newValue);
                valueEd.setSelection(valueEd.getText().length(), valueEd.getText().length());
            }
        } else {
            // 如果没有文本框就直接更新值
            updateSeekBar(v);
            updateValue(v);
        }
        return this;
    }

    private void updateSeekBar(int newValue) {
        if (seekBar != null) {
            int seekbarProgress = newValue - minValue;
            if (seekbarProgress < 0) {
                seekBar.setProgress(0);
            } else {
                seekBar.setProgress(seekbarProgress);
            }
        }
    }

    private void updateValue(int val) {
        if (val != curValue) {
            valueBeforeNotify = curValue;
            curValue = val;

            if (valueChangedListener != null) {
                if (delayTimer == null) {
                    valueChangedListener.OnValueSliderChanged(this, curValue);
                } else {
                    // 重新计时
                    delayTimer.removeCallbacks(this);
                    delayTimer.postDelayed(this, NOTIFY_DELAY_TIME);
                }
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int val = progress + minValue;
            setValue(val);
            updateValue(val);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (valueEd != null) {
            valueEd.setEnabled(false);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (valueEd != null) {
            valueEd.setEnabled(true);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (valueEd == null) {
            return;
        }
        // 可输入状态下才进行判断
        if (valueEd.isEnabled()) {
            String valStr = s.toString();
            int val = 0;
            if (isHexInput) {
                int size = HexStringParser.parse(valStr, hexBuf, 0, MAX_HEX_SIZE);
                if (size > 0) {
                    val = HexEndian.fromByte(hexBuf, 0, size, true);
                }
            } else {
                try {
                    val = Integer.valueOf(valStr);
                } catch (Exception ignored) {
                }
            }

            if (limitEditorInput) {
                if (val > maxEditorInputValue) {
                    val = maxEditorInputValue;
                    setValue(val); // 会再进入该回调
                }
            }

            // 联动更新滑动条
            updateSeekBar(val);

            updateValue(val);
        }
    }

    @Override
    public void run() {
        // handle timeout event from Handler
        if (valueChangedListener != null && valueBeforeNotify != curValue) {
            valueBeforeNotify = curValue;
            valueChangedListener.OnValueSliderChanged(this, curValue);
        }
    }
}
