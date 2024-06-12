package com.goodix.ble.libuihelper.input;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libuihelper.R;


public class EditTextCleanupHelper implements View.OnFocusChangeListener, TextWatcher, View.OnTouchListener {
    public static final int EVT_CLEAR = 586;

    /**
     * 删除按钮的引用
     */
    private Drawable clearCompoundDrawable;
    private int clearCompoundDrawableIdx;
    private boolean isRelative;

    private Rect drawableRect = new Rect();

    private EditText editText;

    /**
     * 控件是否有焦点
     */
    private boolean hasFocus;

    private Event<Void> eventClear;

    /**
     * 自动查找可用的点击图标
     */
    public EditTextCleanupHelper attach(EditText editText) {
        return attach(editText, null);
    }

    @SuppressWarnings("WeakerAccess")
    public EditTextCleanupHelper attach(EditText editText, @Nullable Drawable compoundDrawable) {
        isRelative = true;
        clearCompoundDrawableIdx = 0;
        clearCompoundDrawable = null;

        Drawable[] compoundDrawables = editText.getCompoundDrawablesRelative();

        while (true) {
            // 先找相对方位的
            for (int i = 0; i < compoundDrawables.length; i++) {
                if (compoundDrawables[i] != null) {
                    if (compoundDrawable == null || compoundDrawable == compoundDrawables[i]) {
                        clearCompoundDrawableIdx = i;
                        clearCompoundDrawable = compoundDrawables[i];
                        // break; 不退出，这样可以优先用 right/end 的图标
                    }
                }
            }

            // 相对方位里面没找到，再找绝对方位的
            if (clearCompoundDrawable == null && isRelative) {
                compoundDrawables = editText.getCompoundDrawables();
                isRelative = false;
            } else {
                break;
            }
        }

        this.editText = editText;
        init();
        return this;
    }

    public Event<Void> evtClear() {
        if (eventClear == null) {
            synchronized (this) {
                if (eventClear == null) {
                    eventClear = new Event<>(this, EVT_CLEAR);
                }
            }
        }
        return eventClear;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        //获取EditText的DrawableRight,假如没有设置我们就使用默认的图片
        if (clearCompoundDrawable == null) {
            clearCompoundDrawable = ContextCompat.getDrawable(editText.getContext(), R.drawable.ic_close_black_24dp);
            clearCompoundDrawableIdx = 2; // right/end

            if (isRelative) {
                Drawable[] compoundDrawables = editText.getCompoundDrawablesRelative();
                compoundDrawables[clearCompoundDrawableIdx] = clearCompoundDrawable;
                editText.setCompoundDrawablesRelative(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
            } else {
                Drawable[] compoundDrawables = editText.getCompoundDrawables();
                compoundDrawables[clearCompoundDrawableIdx] = clearCompoundDrawable;
                editText.setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
            }
        }
        clearCompoundDrawable.setBounds(0, 0, clearCompoundDrawable.getIntrinsicWidth(), clearCompoundDrawable.getIntrinsicHeight());

        editText.setOnTouchListener(this);

        //默认设置隐藏图标
        setClearIconVisible(false);

        //设置焦点改变的监听
        editText.setOnFocusChangeListener(this);

        //设置输入框里面内容发生改变的监听
        editText.addTextChangedListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (clearCompoundDrawable != null) {
            final int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                Rect rect = clearCompoundDrawable.getBounds();
                int height = rect.height();
                drawableRect.top = (editText.getHeight() - height) / 2;
                drawableRect.bottom = drawableRect.top + height;

                if (clearCompoundDrawableIdx == 0) { // left/start
//                    if (isRelative) {
//                        if (editText.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
//                            final int editTextWidth = editText.getWidth();
//                            drawableRect.left = editText.getPaddingStart();
//                            drawableRect.right = editText.getTotalPaddingStart();
//                        }
//                    } else {
                    drawableRect.left = editText.getPaddingLeft();
                    drawableRect.right = editText.getTotalPaddingLeft();
//                    }
                } else if (clearCompoundDrawableIdx == 2) { // right/end
                    final int editTextWidth = editText.getWidth();
                    drawableRect.left = editTextWidth - editText.getTotalPaddingRight();
                    drawableRect.right = editTextWidth - editText.getPaddingRight();
                }

                final int x = (int) event.getX();
                final int y = (int) event.getY();
                if (x < drawableRect.left || x > drawableRect.right || y < drawableRect.top || y > drawableRect.bottom) {
                    return false;
                } else {
                    if (action == MotionEvent.ACTION_UP) {
                        editText.setText("");
                        final Event<Void> eventClear = this.eventClear;
                        if (eventClear != null) {
                            eventClear.postEvent(null);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 设置清除图标的显示与隐藏，调用setCompoundDrawables为EditText绘制上去
     */
    private void setClearIconVisible(boolean visible) {


        if (isRelative) {
            Drawable[] compoundDrawables = editText.getCompoundDrawablesRelative();
            compoundDrawables[clearCompoundDrawableIdx] = visible ? clearCompoundDrawable : null;
            editText.setCompoundDrawablesRelative(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
        } else {
            Drawable[] compoundDrawables = editText.getCompoundDrawables();
            compoundDrawables[clearCompoundDrawableIdx] = visible ? clearCompoundDrawable : null;
            editText.setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], compoundDrawables[3]);
        }
    }

    /**
     * 当ClearEditText焦点发生变化的时候，判断里面字符串长度设置清除图标的显示与隐藏
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        this.hasFocus = hasFocus;
        if (hasFocus) {
            setClearIconVisible(editText.getText().length() > 0);
        } else {
            setClearIconVisible(false);
        }
    }

    /**
     * 当输入框里面内容发生变化的时候回调的方法
     */
    @Override
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (hasFocus) {
            setClearIconVisible(text.length() > 0);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
