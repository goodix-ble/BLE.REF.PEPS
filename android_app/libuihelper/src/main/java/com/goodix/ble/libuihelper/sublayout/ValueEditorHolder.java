package com.goodix.ble.libuihelper.sublayout;

import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libcomx.util.HexStringParser;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.input.filter.HexInputFilter;

@SuppressWarnings("WeakerAccess")
public class ValueEditorHolder implements ISubLayoutHolder<ValueEditorHolder> {
    public View root;
    public EditText valueEd;
    @Nullable
    public TextView captionTv;
    @Nullable
    public TextView hexPrefixTv;
    @Nullable
    public Button actionBtn;

    private static final int VALUE_TYPE_TXT = 0;
    private static final int VALUE_TYPE_HEX = 1;
    private static final int VALUE_TYPE_NUM = 2;
    private static final int VALUE_TYPE_FLOAT = 3;
    private int inputValueType = VALUE_TYPE_TXT;
    private int hexSize;
    private byte[] integerBuffer; // 用于整数输入
    private int actualHexSize; // 实际输入的字节数
    private boolean hasOrgHint; // 是否自带Hint

    public ValueEditorHolder inflate(LayoutInflater inflater, @LayoutRes int resource, @Nullable ViewGroup container) {
        return this.inflate(inflater, container, resource);
    }

    public ValueEditorHolder inflate(LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int resource) {
        View view = inflater.inflate(resource, container, false);
        attachView(view);
        if (container != null) {
            container.addView(view);
        }
        return this;
    }

    @Override
    public ValueEditorHolder attachView(View root) {
        this.root = root;
        if (root instanceof EditText) {
            valueEd = (EditText) root;
        } else {
            captionTv = root.findViewById(R.id.sublayout_caption_tv);
            hexPrefixTv = root.findViewById(R.id.sublayout_hex_prefix_tv);
            valueEd = root.findViewById(R.id.sublayout_value_ed);
            actionBtn = root.findViewById(R.id.sublayout_action_btn);

            if (captionTv == null && hexPrefixTv == null && valueEd == null && actionBtn == null) {
                if (root instanceof ViewGroup) {
                    ViewGroup container = (ViewGroup) root;
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final View child = container.getChildAt(i);
                        if (captionTv == null && child instanceof TextView) {
                            captionTv = (TextView) child;
                        } else if (valueEd == null && child instanceof EditText) {
                            valueEd = (EditText) child;
                        } else if (actionBtn == null && child instanceof Button) {
                            actionBtn = (Button) child;
                        }
                    }
                }
            }
        }

        if (valueEd == null) {
            valueEd = new EditText(root.getContext());
            if (root instanceof ViewGroup) {
                ((ViewGroup) root).addView(valueEd);
            }
        }

        hasOrgHint = valueEd.getHint() != null && valueEd.getHint().length() > 0;
        return this;
    }

    @Override
    public ValueEditorHolder setEnabled(boolean enabled) {
        root.setEnabled(enabled);
        valueEd.setEnabled(enabled);
        if (actionBtn != null) {
            actionBtn.setEnabled(enabled);
        }
        return this;
    }

    @Override
    public ValueEditorHolder setVisibility(int visibility) {
        root.setVisibility(visibility);
        return this;
    }


    @Override
    public ValueEditorHolder setCaption(int resId) {
        if (captionTv != null) {
            captionTv.setText(resId);
        }
        return this;
    }

    @Override
    public ValueEditorHolder setCaption(CharSequence txt) {
        if (captionTv != null) {
            captionTv.setText(txt);
        }
        return this;
    }

    @Override
    public ValueEditorHolder setOnClickListener(View.OnClickListener l) {
        if (actionBtn != null) {
            actionBtn.setOnClickListener(l);
        }
        return this;
    }

    /**
     * 启用十六进制输入方式
     *
     * @param size 指定需要输入的十六进制字节数，可选值为 1、2、4 。当输入为 1 时，文本框会使用 %02X 的格式
     */
    public ValueEditorHolder enableHexInput(int size, boolean showPrefix) {
        if (size < 1) size = 1;
        inputValueType = VALUE_TYPE_HEX;
        hexSize = size;
        if (hexPrefixTv != null) {
            hexPrefixTv.setVisibility(showPrefix ? View.VISIBLE : View.GONE);
        }
        if (!hasOrgHint) {
            valueEd.setHint("HEX");
        }
        valueEd.setFilters(new InputFilter[]{new HexInputFilter(), new InputFilter.LengthFilter(size * 2)});
        valueEd.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        return this;
    }

    public ValueEditorHolder enableNumInput() {
        return enableNumInput(false);
    }

    public ValueEditorHolder enableNumInput(boolean hasSign) {
        inputValueType = VALUE_TYPE_NUM;
        if (hexPrefixTv != null) {
            hexPrefixTv.setVisibility(View.GONE);
        }
        if (!hasOrgHint) {
            valueEd.setHint(R.string.libuihelper_na);
        }
        valueEd.setFilters(new InputFilter[0]);
        valueEd.setInputType(hasSign ?
                (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_SIGNED)
                : EditorInfo.TYPE_CLASS_NUMBER);
        return this;
    }

    public ValueEditorHolder enableFloatInput(boolean hasSign) {
        inputValueType = VALUE_TYPE_FLOAT;
        if (hexPrefixTv != null) {
            hexPrefixTv.setVisibility(View.GONE);
        }
        if (!hasOrgHint) {
            valueEd.setHint(R.string.libuihelper_na);
        }
        valueEd.setFilters(new InputFilter[0]);
        valueEd.setInputType(hasSign ?
                (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL | EditorInfo.TYPE_NUMBER_FLAG_SIGNED)
                : EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        return this;
    }

    @Override
    public ValueEditorHolder noButton() {
        if (actionBtn != null) {
            actionBtn.setVisibility(View.GONE);
        }
        return this;
    }

    public int getValue() {
        if (inputValueType == VALUE_TYPE_HEX) {
            return HexStringParser.parseInt(valueEd.getText());
        }

        if (inputValueType == VALUE_TYPE_NUM) {
            try {
                return Integer.parseInt(valueEd.getText().toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public int getByteSize() {
        if (integerBuffer == null || integerBuffer.length != hexSize) {
            integerBuffer = new byte[hexSize];
        }

        actualHexSize = HexStringParser.parse(valueEd.getText(), integerBuffer, 0, hexSize);
        return actualHexSize;
    }

    public byte[] getByteValue() {
        int bufSize = getByteSize();
        byte[] out = new byte[bufSize];
        System.arraycopy(integerBuffer, 0, out, 0, actualHexSize);
        return out;
    }

    public float getFloatValue() {
        if (inputValueType == VALUE_TYPE_FLOAT) {
            try {
                return Float.parseFloat(valueEd.getText().toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return getValue();
    }


    public int getValue(byte[] out, int pos, int size) {
        if (size > getByteSize()) size = actualHexSize;
        System.arraycopy(integerBuffer, 0, out, pos, size);
        return size;
    }

    public ValueEditorHolder setValue(int i) {
        if (inputValueType == VALUE_TYPE_HEX) {
            valueEd.setText(new HexStringBuilder(8).put(i, hexSize));
        } else {
            valueEd.setText(String.valueOf(i));
        }
        return this;
    }

    public ValueEditorHolder setValue(CharSequence hex) {
        if (inputValueType == VALUE_TYPE_HEX) {
            valueEd.setText(hex);
        }
        return this;
    }

    public ValueEditorHolder setValue(byte[] hex) {
        if (inputValueType == VALUE_TYPE_HEX) {
            if (hex != null) {
                return setValue(hex, 0, hex.length);
            }
        }
        return this;
    }

    public ValueEditorHolder setValue(byte[] hex, int offset, int size) {
        if (inputValueType == VALUE_TYPE_HEX) {
            if (hex != null) {
                if (size > 0) {
                    valueEd.setText(new HexStringBuilder(size * 2).put(hex, offset, size));
                } else {
                    valueEd.setText("");
                }
            }
        }
        return this;
    }

    public ValueEditorHolder setValue(float i) {
        if (inputValueType == VALUE_TYPE_HEX) {
            valueEd.setText(new HexStringBuilder(8).put((int) i, hexSize));
        } else {
            valueEd.setText(String.valueOf(i));
        }
        return this;
    }
}
