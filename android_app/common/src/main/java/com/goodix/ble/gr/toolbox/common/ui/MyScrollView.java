package com.goodix.ble.gr.toolbox.common.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by yuanmingwu on 18-9-20.
 */

public class MyScrollView  extends ScrollView {

    private OnScrollChangeListener scrollChangeListener = null;

    public MyScrollView(Context context) {
        super(context);
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setScrollViewListener(OnScrollChangeListener onScrollChangeListener){
        this.scrollChangeListener = onScrollChangeListener;
    }
    @Override
    protected void onScrollChanged(int x,int y,int oldx,int oldy){
        super.onScrollChanged(x,y,oldx,oldy);
        if(scrollChangeListener != null){
            scrollChangeListener.onScrollChanged(this,x,y,oldx,oldy);
        }
    }

    public interface OnScrollChangeListener{
        void onScrollChanged(MyScrollView scrollView, int x, int y, int oldx, int oldy);
    }
}
