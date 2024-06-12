package com.goodix.ble.libuihelper.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libuihelper.R;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"unused", "SameParameterValue", "FieldCanBeLocal", "UnusedReturnValue"})
public class EasySelectDialog<T> {

    private float density;
    private LinearLayout root;
    private RecyclerView recyclerView;
    private SelectorAdapter selectorAdapter;
    private List<? extends T> itemList;
    private Listener<T> listener;
    private Converter<T> converter;
    private AlertDialog dialog;
    private Context ctx;
    private String title;
    private String tipsForEmpty;

    public interface Listener<T> {
        void onItemSelected(int pos, T item);
    }

    public interface Converter<T> {
        void onItemToString(int pos, T item, HexStringBuilder caption, HexStringBuilder desc);
    }

    public EasySelectDialog(View v) {
        this.ctx = v.getContext();
    }

    public EasySelectDialog(Context ctx) {
        this.ctx = ctx;
    }

    public EasySelectDialog<T> setTitle(String title) {
        this.title = title;
        return this;
    }

    public EasySelectDialog<T> setTitle(int strResId) {
        this.title = ctx.getString(strResId);
        return this;
    }

    public EasySelectDialog<T> setTipsForEmpty(String tips) {
        this.tipsForEmpty = tips;
        return this;
    }

    public EasySelectDialog<T> setTipsForEmpty(int strResId) {
        this.tipsForEmpty = ctx.getString(strResId);
        return this;
    }

    public EasySelectDialog<T> setListener(Listener<T> listener) {
        this.listener = listener;
        return this;
    }

    public EasySelectDialog<T> setConverter(Converter<T> converter) {
        this.converter = converter;
        return this;
    }

    public EasySelectDialog<T> setItemList(List<? extends T> itemList) {
        this.itemList = itemList;
        return this;
    }

    public EasySelectDialog<T> setItemList(T[] itemList) {
        this.itemList = Arrays.asList(itemList);
        return this;
    }

    public EasySelectDialog<T> show() {
        density = ctx.getResources().getDisplayMetrics().density;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        if (title == null) {
            title = ctx.getString(R.string.libuihelper_please_select);
        }
        builder.setTitle(title);

        builder.setCancelable(true);

        if (itemList == null || itemList.size() == 0) {
            if (tipsForEmpty == null) {
                tipsForEmpty = ctx.getString(R.string.libuihelper_empty_list);
            }
            builder.setMessage(tipsForEmpty);
            builder.setNegativeButton(android.R.string.ok, null);
        } else {
            builder.setView(constructView(ctx));
        }

        dialog = builder.create();
        final Window window = dialog.getWindow();
        if (window != null) {
            // 解决在对话框上再弹出对话框时屏幕会闪烁的问题
            // https://blog.csdn.net/kongqwesd12/article/details/80775935
            window.setWindowAnimations(R.style.DialogNoAnim);
        }
        dialog.show();

        return this;
    }

    private View constructView(Context ctx) {
        root = new LinearLayout(ctx);
        root.setLayoutParams(layout(true, true));
        int dp6 = dp(6);
        root.setPadding(dp6, dp6, dp6, dp(32));
        root.setBackgroundColor(0xFFEFF3F7);

        selectorAdapter = new SelectorAdapter();

        recyclerView = new RecyclerView(ctx);
        recyclerView.setLayoutParams(layout(true, true));

        recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
        //recyclerView.addItemDecoration(new DividerItemDecoration(ctx, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(selectorAdapter);
        root.addView(recyclerView);

        return root;
    }

    private ViewGroup.LayoutParams layout(boolean fillWidth, boolean fillHeight) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.width = fillWidth ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = fillHeight ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        return params;
    }

    private int dp(int dp) {
        return (int) (density * dp);
    }

    private class SelectorAdapter extends RecyclerView.Adapter<SelectorAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            if (converter != null) {
                viewHolder.clearStringBuilder();
                converter.onItemToString(i, itemList.get(i), viewHolder.captionSb, viewHolder.descSb);
                if (viewHolder.captionSb.length() == 0) {
                    viewHolder.captionTv.setVisibility(View.GONE);
                } else {
                    viewHolder.captionTv.setVisibility(View.VISIBLE);
                    viewHolder.captionTv.setText(viewHolder.captionSb);
                }
                if (viewHolder.descSb.length() == 0) {
                    viewHolder.descTv.setVisibility(View.GONE);
                } else {
                    viewHolder.descTv.setVisibility(View.VISIBLE);
                    viewHolder.descTv.setText(viewHolder.descSb);
                }
            } else {
                viewHolder.captionTv.setVisibility(View.GONE);
                viewHolder.descTv.setVisibility(View.VISIBLE);
                viewHolder.descTv.setText(itemList.get(i).toString());
            }
        }

        @Override
        public int getItemCount() {
            if (itemList == null) {
                return 0;
            }
            return itemList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            private final LinearLayout root;
            private final TextView captionTv;
            private final TextView descTv;
            private final HexStringBuilder captionSb;
            private final HexStringBuilder descSb;

            ViewHolder(@NonNull View v) {
                super(new LinearLayout(v.getContext()));
                Context ctx = v.getContext();
                root = (LinearLayout) this.itemView;
                root.setLayoutParams(layout(true, false));
                int padding = dp(6);
                root.setPadding(padding, padding, padding, padding);
                {
                    CardView card = new CardView(ctx);
                    card.setLayoutParams(layout(true, false));
                    root.addView(card);
                    {
                        LinearLayout layoutInCard = new LinearLayout(ctx);
                        layoutInCard.setLayoutParams(layout(true, false));
                        layoutInCard.setPadding(padding * 2, padding, padding * 2, padding);
                        layoutInCard.setOrientation(LinearLayout.VERTICAL);
                        layoutInCard.setGravity(Gravity.CENTER_VERTICAL);
                        layoutInCard.setMinimumHeight(dp(56));
                        card.addView(layoutInCard);
                        {
                            captionTv = new TextView(ctx);
                            captionTv.setTextSize(16);
                            captionTv.setTextColor(Color.BLACK);
                            captionTv.setLayoutParams(layout(true, false));
                            layoutInCard.addView(captionTv);

                            descTv = new TextView(ctx);
                            descTv.setTextSize(12);
                            descTv.setTextColor(Color.DKGRAY);
                            descTv.setLayoutParams(layout(true, false));
                            layoutInCard.addView(descTv);
                        }
                    }
                }

                root.setOnClickListener(this);

                captionSb = new HexStringBuilder(128);
                descSb = new HexStringBuilder(512);
            }

            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    listener.onItemSelected(pos, itemList.get(pos));
                }
                dialog.dismiss();
            }

            void clearStringBuilder() {
                captionSb.clear();
                descSb.clear();
            }
        }
    }
}
