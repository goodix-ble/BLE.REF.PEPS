package com.goodix.ble.libuihelper.sublayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.goodix.ble.libuihelper.R;


@SuppressWarnings({"WeakerAccess", "unused"})
public class GenericSelectHolder implements ISubLayoutHolder<GenericSelectHolder> {

    public View root;
    public TextView captionTv;
    public TextView nameTv;
    public TextView valueTv;
    public Button selectBtn;
    public Button actionBtn;

    public GenericSelectHolder inflate(LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int resource) {
        View view = inflater.inflate(resource, container, false);
        attachView(view);
        if (container != null) {
            container.addView(view);
        }
        return this;
    }

    @Override
    public GenericSelectHolder attachView(View root) {
        this.root = root;
        captionTv = root.findViewById(R.id.sublayout_caption_tv);
        nameTv = root.findViewById(R.id.sublayout_name_tv);
        valueTv = root.findViewById(R.id.sublayout_value_tv);
        selectBtn = root.findViewById(R.id.sublayout_select_btn);
        actionBtn = root.findViewById(R.id.sublayout_action_btn);
        return this;
    }

    @Override
    public GenericSelectHolder setEnabled(boolean enabled) {
        root.setEnabled(enabled);
        if (selectBtn != null) {
            selectBtn.setEnabled(enabled);
        }
        if (actionBtn != null) {
            actionBtn.setEnabled(enabled);
        }
        return this;
    }

    @Override
    public GenericSelectHolder setVisibility(int visibility) {
        root.setVisibility(visibility);
        return this;
    }

    @Override
    public GenericSelectHolder setCaption(CharSequence text) {
        if (captionTv != null) {
            captionTv.setText(text);
        }
        return this;
    }

    @Override
    public GenericSelectHolder setCaption(int strResId) {
        if (captionTv != null) {
            captionTv.setText(strResId);
        }
        return this;
    }

    @Override
    public GenericSelectHolder noButton() {
        if (selectBtn != null) {
            selectBtn.setVisibility(View.GONE);
        }
        if (actionBtn != null) {
            actionBtn.setVisibility(View.GONE);
        }
        return this;
    }

    public GenericSelectHolder setNameValue(String name, String value) {
        if (nameTv != null) {
            nameTv.setText(name);
        }
        if (valueTv != null) {
            if (value != null) {
                valueTv.setText(value);
            } else {
                valueTv.setVisibility(View.INVISIBLE);
            }
        }
        return this;
    }

    @Override
    public GenericSelectHolder setOnClickListener(View.OnClickListener l) {
        if (selectBtn != null) {
            selectBtn.setOnClickListener(l);
        }
        if (actionBtn != null) {
            actionBtn.setOnClickListener(l);
        }
        return this;
    }
}
