package com.goodix.ble.libuihelper.sublayout;

import androidx.annotation.Nullable;
import android.view.View;

public interface ISubLayoutHolder<T extends ISubLayoutHolder> {

    T attachView(@Nullable View target);

    T setEnabled(boolean enabled);

    T setVisibility(int visibility);

    T setOnClickListener(View.OnClickListener l);

    T setCaption(CharSequence text);

    T setCaption(int resId);

    T noButton();
}
