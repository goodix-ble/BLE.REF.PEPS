package com.goodix.ble.libuihelper.sublayout;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.goodix.ble.libuihelper.R;

public class ValueGetterHolder implements ISubLayoutHolder<ValueGetterHolder> {
    public View root;
    public TextView captionTv;
    public TextView valueTv;
    public Button getBtn;

    @Override
    public ValueGetterHolder attachView(View root) {
        this.root = root;
        captionTv = root.findViewById(R.id.sublayout_caption_tv);
        valueTv = root.findViewById(R.id.sublayout_value_tv);
        getBtn = root.findViewById(R.id.sublayout_get_btn);

        return this;
    }

    @Override
    public ValueGetterHolder setEnabled(boolean enabled) {
        root.setEnabled(enabled);
        if (getBtn != null) {
            getBtn.setEnabled(enabled);
        }
        return this;
    }

    @Override
    public ValueGetterHolder setVisibility(int visibility) {
        root.setVisibility(visibility);
        return this;
    }

    @Override
    public ValueGetterHolder setOnClickListener(View.OnClickListener l) {
        getBtn.setOnClickListener(l);
        return this;
    }

    @Override
    public ValueGetterHolder setCaption(CharSequence text) {
        captionTv.setText(text);
        return this;
    }

    @Override
    public ValueGetterHolder setCaption(int resId) {
        captionTv.setText(resId);
        return this;
    }

    @Override
    public ValueGetterHolder noButton() {
        getBtn.setVisibility(View.GONE);
        return this;
    }
}
