package com.goodix.ble.libuihelper.sublayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libuihelper.R;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class ValueSpinnerHolder<DT> implements ISubLayoutHolder<ValueSpinnerHolder>, AdapterView.OnItemSelectedListener {
    public static final int EVT_ITEM_SELECTED = 813;

    public View root;
    public Spinner valueSpinner;
    private ValueSpinnerAdapter adapter = new ValueSpinnerAdapter();
    @Nullable
    public TextView captionTv;
    @Nullable
    public Button actionBtn;

    @Nullable
    private Event<DT> eventItemSelected;

    public void setItemView(int itemViewLayoutResId, int itemViewTextViewResId) {
        adapter.itemViewLayoutResId = itemViewLayoutResId;
        adapter.itemViewTextViewResId = itemViewTextViewResId;
    }

    public Event<DT> evtItemSelected() {
        if (eventItemSelected == null) {
            synchronized (this) {
                if (eventItemSelected == null) {
                    eventItemSelected = new Event<>(this, EVT_ITEM_SELECTED);
                }
            }
        }
        return eventItemSelected;
    }

    public ValueSpinnerHolder<DT> inflate(LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int resource) {
        View view = inflater.inflate(resource, container, false);
        attachView(view);
        if (container != null) {
            container.addView(view);
        }
        return this;
    }

    @Override
    public ValueSpinnerHolder<DT> attachView(View root) {
        this.root = root;
        if (root instanceof Spinner) {
            valueSpinner = (Spinner) root;
        } else {
            captionTv = root.findViewById(R.id.sublayout_caption_tv);
            valueSpinner = root.findViewById(R.id.sublayout_spinner);
            actionBtn = root.findViewById(R.id.sublayout_action_btn);
            // 如果没有找到，就尝试遍历子元素，获取可用的View
            if (captionTv == null || valueSpinner == null || actionBtn == null) {
                // 只寻找View容器里面的第一层级的子元素
                if (root instanceof ViewGroup) {
                    ViewGroup container = (ViewGroup) root;
                    for (int i = 0; i < container.getChildCount(); i++) {
                        final View child = container.getChildAt(i);
                        if (captionTv == null && (child instanceof TextView)) {
                            captionTv = (TextView) child;
                        }
                        if (valueSpinner == null && (child instanceof Spinner)) {
                            valueSpinner = (Spinner) child;
                        }
                        if (actionBtn == null && (child instanceof Button)) {
                            actionBtn = (Button) child;
                        }
                    }
                }
            }
        }

        if (valueSpinner != null) {
            valueSpinner.setAdapter(adapter);
            valueSpinner.setOnItemSelectedListener(this);
        }

        return this;
    }

    @Override
    public ValueSpinnerHolder<DT> setEnabled(boolean enabled) {
        root.setEnabled(enabled);
        return this;
    }

    @Override
    public ValueSpinnerHolder<DT> setVisibility(int visibility) {
        root.setVisibility(visibility);
        return this;
    }


    @Override
    public ValueSpinnerHolder<DT> setCaption(int resId) {
        if (captionTv != null) {
            captionTv.setText(resId);
        }
        return this;
    }

    @Override
    public ValueSpinnerHolder<DT> setCaption(CharSequence txt) {
        if (captionTv != null) {
            captionTv.setText(txt);
        }
        return this;
    }

    @Override
    public ValueSpinnerHolder<DT> setOnClickListener(View.OnClickListener l) {
        if (actionBtn != null) {
            actionBtn.setOnClickListener(l);
        }
        return this;
    }

    @Override
    public ValueSpinnerHolder<DT> noButton() {
        if (actionBtn != null) {
            actionBtn.setVisibility(View.GONE);
        }
        return this;
    }

    public DT getValue() {
        int pos = valueSpinner.getSelectedItemPosition();
        if (pos < 0) {
            pos = 0;
        }
        //noinspection unchecked
        return (DT) adapter.items.get(pos).value;
    }

    public int getValuePosition() {
        return valueSpinner.getSelectedItemPosition();
    }

    public ValueSpinnerHolder<DT> setValue(DT value) {
        for (int i = 0; i < adapter.items.size(); i++) {
            if (value == null) {
                if (adapter.items.get(i).value == null) {
                    valueSpinner.setSelection(i);
                    break;
                }
            } else {
                if (value == adapter.items.get(i).value || value.equals(adapter.items.get(i).value)) {
                    valueSpinner.setSelection(i);
                    break;
                }
            }
        }
        return this;
    }

    public ValueSpinnerHolder<DT> setValues(DT[] values) {
        adapter.items.ensureCapacity(values.length);

        int existSize = adapter.getCount();

        // 复用或添加 Item
        for (int i = 0; i < values.length; i++) {
            ItemHolder item;
            if (i < existSize) {
                item = adapter.items.get(i);
            } else {
                item = new ItemHolder();
                item.captionStr = values[i].toString();
                adapter.items.add(item);
            }
            item.itemId = i;
            item.value = values[i];
        }

        // 去除多余的
        if (adapter.getCount() > values.length) {
            adapter.items.subList(values.length, adapter.getCount()).clear();
        }
        adapter.notifyDataSetChanged();
        return this;
    }

    public ValueSpinnerHolder<DT> setValueCaptions(@StringRes int... captions) {
        adapter.items.ensureCapacity(captions.length);

        int existSize = adapter.getCount();

        // 复用或添加 Item
        for (int i = 0; i < captions.length; i++) {
            ItemHolder item;
            if (i < existSize) {
                item = adapter.items.get(i);
            } else {
                item = new ItemHolder();
                item.value = i;
                adapter.items.add(item);
            }
            item.itemId = i;
            item.captionResId = captions[i];
        }

        // 去除多余的
        if (adapter.getCount() > captions.length) {
            adapter.items.subList(captions.length, adapter.getCount()).clear();
        }
        adapter.notifyDataSetChanged();
        return this;
    }

    public ValueSpinnerHolder<DT> setValueCaptions(String... captions) {
        adapter.items.ensureCapacity(captions.length);

        int existSize = adapter.getCount();

        // 复用或添加 Item
        for (int i = 0; i < captions.length; i++) {
            ItemHolder item;
            if (i < existSize) {
                item = adapter.items.get(i);
            } else {
                item = new ItemHolder();
                item.value = i;
                adapter.items.add(item);
            }
            item.itemId = i;
            item.captionStr = captions[i];
        }

        // 去除多余的
        if (adapter.getCount() > captions.length) {
            adapter.items.subList(captions.length, adapter.getCount()).clear();
        }
        adapter.notifyDataSetChanged();
        return this;
    }

    public ValueSpinnerHolder<DT> addValue(@StringRes int captionResId, DT value) {
        ItemHolder item = new ItemHolder();
        item.itemId = adapter.items.size();
        item.captionResId = captionResId;
        item.value = value;
        adapter.items.add(item);
        adapter.notifyDataSetChanged();
        return this;
    }

    public ValueSpinnerHolder<DT> addValue(String caption, DT value) {
        ItemHolder item = new ItemHolder();
        item.itemId = adapter.items.size();
        item.captionStr = caption;
        item.value = value;
        adapter.items.add(item);
        adapter.notifyDataSetChanged();
        return this;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (eventItemSelected != null) {
            //noinspection unchecked
            eventItemSelected.postEvent((DT) adapter.items.get(position).value);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    static class ValueSpinnerAdapter extends BaseAdapter {
        ArrayList<ItemHolder> items = new ArrayList<>();

        @LayoutRes
        private int itemViewLayoutResId = 0;

        @IdRes
        private int itemViewTextViewResId = 0;

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).itemId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ItemHolder item = items.get(position);
            TextView captionTv = null;

            if (convertView != null) {
                Object tmp = convertView.getTag();
                if (tmp == null) {
                    tmp = convertView;
                }
                if (tmp instanceof TextView) {
                    captionTv = (TextView) tmp;
                }
            } else {
                if (itemViewLayoutResId != 0) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(itemViewLayoutResId, parent, false);
                    captionTv = convertView.findViewById(itemViewTextViewResId);
                    convertView.setTag(captionTv); // 方便快速获取

                    // 如果 TextView 的 ResId是无效的，就尝试使用根View
                    if (captionTv == null && (convertView instanceof TextView)) {
                        captionTv = (TextView) convertView;
                    }
                } else {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_spinner_item, parent, false);
                    captionTv = (TextView) convertView;
                    //convertView = new TextView(parent.getContext());
                }
            }

            if (captionTv != null) {
                if (item.captionResId != 0) {
                    captionTv.setText(item.captionResId);
                } else {
                    captionTv.setText(item.captionStr);
                }
            }

            return convertView;
        }
    }

    static class ItemHolder {
        // 这个两个caption，二选其一，都赋值的情况下，优先使用captionResId
        @StringRes
        public int captionResId = 0;
        public String captionStr;

        public long itemId;
        public Object value;
    }
}
