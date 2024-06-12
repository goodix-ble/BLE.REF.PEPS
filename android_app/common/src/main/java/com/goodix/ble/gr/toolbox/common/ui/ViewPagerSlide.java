package com.goodix.ble.gr.toolbox.common.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ViewPagerSlide extends ViewPager {

    private  boolean isSlide = true;

    public ViewPagerSlide(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewPagerSlide(@NonNull Context context) {
        super(context);
    }

    public void setSlide(boolean isSlide) {
        this.isSlide = isSlide;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isSlide) {
            return  isSlide;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isSlide) {
            return  isSlide;
        }
        return super.onTouchEvent(ev);
    }
}
