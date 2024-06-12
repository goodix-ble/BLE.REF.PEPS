package com.goodix.ble.libuihelper.sublayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.goodix.ble.libuihelper.R;

import java.util.ArrayList;
import java.util.List;

public class ValueRadioHolder implements ISubLayoutHolder<ValueRadioHolder> {

    public View root;
    public TextView captionTv;
    public RadioGroup radioGroup;
    public Button actionBtn;
    public List<RadioButton> optionList = new ArrayList<>(8);
    private View.OnClickListener clickListener;

//    public interface Listener {
//        void onOptionSelected(ValueRadioHolder holder, RadioButton btn, Object tag);
//    }

    public ValueRadioHolder inflate(LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int resource) {
        View view = inflater.inflate(resource, container, false);
        attachView(view);
        if (container != null) {
            container.addView(view);
        }
        return this;
    }

    @Override
    public ValueRadioHolder attachView(View target) {
        this.root = target;
        if (target != null) {
            captionTv = target.findViewById(R.id.sublayout_caption_tv);
            radioGroup = target.findViewById(R.id.sublayout_value_radio_group);
            actionBtn = target.findViewById(R.id.sublayout_action_btn);
        }
        return this;
    }

    @Override
    public ValueRadioHolder setEnabled(boolean enabled) {
        root.setEnabled(enabled);
        return this;
    }

    @Override
    public ValueRadioHolder setVisibility(int visibility) {
        root.setVisibility(visibility);
        return this;
    }

    @Override
    public ValueRadioHolder setOnClickListener(View.OnClickListener l) {
        actionBtn.setOnClickListener(l);
        clickListener = l;
        for (RadioButton opt : optionList) {
            opt.setOnClickListener(l);
        }
        return this;
    }

    @Override
    public ValueRadioHolder setCaption(CharSequence text) {
        captionTv.setText(text);
        return this;
    }

    @Override
    public ValueRadioHolder setCaption(int resId) {
        captionTv.setText(resId);
        return this;
    }

    @Override
    public ValueRadioHolder noButton() {
        actionBtn.setVisibility(View.GONE);
        return this;
    }

    public ValueRadioHolder setVerticalLayout() {
        radioGroup.setOrientation(LinearLayout.VERTICAL);
        return this;
    }

    public ValueRadioHolder addOption(CharSequence text, Object tag) {
        RadioButton radio = new RadioButton(root.getContext());

        radio.setText(text);
        radio.setTag(tag);

        if (clickListener != null) {
            radio.setOnClickListener(clickListener);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT);
        if (radioGroup.getOrientation() == LinearLayout.HORIZONTAL) {
            lp.width = 0;
            lp.weight = 1;
        }
        radioGroup.addView(radio, lp);

        if (optionList.size() == 0) {
            radio.setChecked(true);
        }
        optionList.add(radio);

        return this;
    }


    public ValueRadioHolder setSelection(int pos) {
        if (pos < optionList.size() && pos >= 0) {
            optionList.get(pos).setChecked(true);
        }
        return this;
    }

    public int getSelectedPosition() {
        for (int i = 0; i < optionList.size(); i++) {
            if (optionList.get(i).isChecked()) {
                return i;
            }
        }
        return 0;
    }

    public Object getSelectedTag() {
        for (int i = 0; i < optionList.size(); i++) {
            if (optionList.get(i).isChecked()) {
                return optionList.get(i).getTag();
            }
        }
        return 0;
    }
}
