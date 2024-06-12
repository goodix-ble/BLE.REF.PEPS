package com.goodix.ble.libuihelper.input.slider;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class SlideToInputHelper implements View.OnTouchListener {

    private float startX;
    private float startY;

    private float screenDensity;
    private double silentDistance = 5; // dpi
    private double unitDistance = 100; // dpi
    private double unitValue = 50;

    private int slideState = STATE_IDLE;
    private int preNotifiedValue = 0;
    private boolean isHorizontalSlide = false; // 默认垂直方向
    private boolean dispatchTouchEvent = false;
    private SlideListener listener;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARE = 1;
    private static final int STATE_SLIDING = 2;

    public interface SlideListener {
        /**
         * 用于通知开始滑动了。
         * 可以在这个回调里记录原始值。
         * 并且可以在设置控件的状态，提示用户已经开始滑动输入了。
         */
        void onSlideToInputStarted();

        /**
         * 用于通知当前滑动所对应的增量，增量值可以为负数，表示减少原始值。
         * 在这个回调里将原始值和增量值叠加，得到一个绝对值，用于实时显示。
         * 并且可以在设置控件的状态，提示用户已经开始滑动输入了。
         */
        void onSlideToInputSliding(int delta);

        /**
         * 用于通知滑动结束了。
         * 在这个回调里将原始值和增量值叠加，得到最终的值。
         * 并且可以在设置控件的状态，提示用户已经结束滑动输入了。
         */
        void onSlideToInputFinished(int delta);
    }

    public SlideToInputHelper attach(View target) {
        target.setOnTouchListener(this);
        screenDensity = target.getContext().getResources().getDisplayMetrics().density;
        if (screenDensity == 0) {
            screenDensity = 1;
        }
        return this;
    }

    /**
     * 设置在一个距离范围内，不产生滑动事件。并且这个距离内不计算滑动值。
     *
     * @param dp 距离单位：dp
     */
    public SlideToInputHelper setSilentDistance(double dp) {
        this.silentDistance = dp;
        return this;
    }

    /**
     * 设置滑动多少距离为一个单位距离，会用这个单位距离将滑动距离归一化到0-1的范围
     * 不包括静默距离。
     * 例如，静默距离30dp，单位距离100dp，单位值为50，
     * 那么，滑动到130dp的距离时，{@link SlideListener#onSlideToInputSliding}回调中的参数就是50
     * 注意：不能为零
     *
     * @param dp 距离单位：dp
     */
    public SlideToInputHelper setUnitDistance(double dp) {
        this.unitDistance = dp;
        if (dp == 0) {
            throw new IllegalArgumentException("Unit distance can NOT be zero.");
        }
        return this;
    }

    /**
     * 滑动一个单位距离时的增量值
     * 注意：不能为零
     */
    public SlideToInputHelper setUnitValue(int value) {
        this.unitValue = value;
        if (value == 0) {
            throw new IllegalArgumentException("Unit value can NOT be zero.");
        }
        return this;
    }

    public SlideToInputHelper setListener(SlideListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * 启用向目标控件发送触摸事件的功能，一般需要点击事件的可以启用
     */
    public SlideToInputHelper dispatchTouchEvent() {
        this.dispatchTouchEvent = true;
        return this;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float deltaX = event.getX() - startX;
        float deltaY = event.getY() - startY;

        if (dispatchTouchEvent && slideState != STATE_SLIDING) {
            v.onTouchEvent(event); // 为让子控件也能响应点击动作
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();

                slideState = STATE_PREPARE;
                preNotifiedValue = 0;
                isHorizontalSlide = false;

                // 禁止父布局消耗滑动事件。针对控件处于一个滑动布局中的情况。
                v.getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / screenDensity;
                // 再静默距离内，delta就为零，超出静默距离，就从1开始
                if (dist > silentDistance) {
                    // 判断是否开始滑动
                    if (slideState == STATE_PREPARE) {
                        if (listener != null) {
                            listener.onSlideToInputStarted();
                        }
                        slideState = STATE_SLIDING;
                        // 决定方向
                        if (Math.abs(deltaX) > Math.abs(deltaY)) {
                            isHorizontalSlide = true;
                        }
                    }

                    // 计算变化值
                    double unit = (dist - silentDistance) / unitDistance;
                    int deltaValue = (int) (unit * unit * unitValue) + 1;
                    if (isHorizontalSlide) {
                        if (deltaX < 0) {
                            deltaValue = 0 - deltaValue;
                        }
                    } else {
                        if (deltaY > 0) {
                            deltaValue = 0 - deltaValue;
                        }
                    }
                    //Log.e("--------------", "onTouch: dist = " + dist + "     angle = " + 0 + "     deltaValue = " + deltaValue);

                    // 通知
                    if (listener != null && preNotifiedValue != deltaValue) {
                        preNotifiedValue = deltaValue;
                        listener.onSlideToInputSliding(deltaValue);
                    }
                } else {
                    // 进入静默区域时，如果是滑动状态，需要通知一个零出去
                    if (slideState == STATE_SLIDING) {
                        if (listener != null && preNotifiedValue != 0) {
                            preNotifiedValue = 0;
                            listener.onSlideToInputSliding(0);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // 如果进入了滑动状态，在抬手时，确定输入量
                if (slideState == STATE_SLIDING) {
                    if (listener != null) {
                        listener.onSlideToInputFinished(preNotifiedValue);
                    }
                }
                break;
        }
        return true;
    }
}
