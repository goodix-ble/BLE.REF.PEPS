package com.goodix.ble.libuihelper.sublayout.list;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.goodix.ble.libcomx.util.CallUtil;

/**
 * 定义用于将数据和逻辑绑定起来的元素
 */
public abstract class MvcController<DT, VH extends MvcViewHolder> {
    protected int position;
    protected DT item;
    protected boolean isShow;

    @Nullable
    protected VH holder;

    void create(Object item, int position) {
        //noinspection unchecked
        this.item = (DT) item;
        this.position = position;
        onCreate(position, this.item);
    }

    void setShow(boolean show) {
        isShow = show;
    }

    void attach(boolean attach, VH vh) {

        if (attach) {
            // 绑定前不应该有绑定
            VH prvHolder = this.holder;
            if (prvHolder != null) {
                Log.e("MvcList", "    - already bond: posCtrl: " + this.position + "  posHolder: " + prvHolder.getAdapterPosition() + "  holder: @" + prvHolder.hashCode());
                if (prvHolder.bindController != this) {
                    Log.e("MvcList", "    - error bond: this: @" + this.hashCode() + "  prvHolder.bindController: @" + prvHolder.bindController.hashCode());
                    //noinspection unchecked
                    prvHolder.bindController.attach(false, prvHolder); // 结束不良绑定
                }
                this.attach(false, this.holder);
            }
            // 建立双向绑定
            if (vh == null) {
                Log.e("MvcList", "    - holder is null: " + CallUtil.trace(5));
            } else {
                Log.v("MvcList", "    + bind " + this.position + " <-- " + vh.getAdapterPosition() + "  @" + vh.hashCode());
                this.holder = vh; // 建立双向绑定
                vh.bindController = this; // 建立双向绑定
                onAttach(position, item, vh);
                setShow(true);
            }
        } else {
            setShow(false);
            if (holder != null) {
                if (holder != vh && vh != null) {
                    Log.e("MvcList", "    - error detach: this.holder: @" + holder.hashCode() + "  that.holder: @" + vh.hashCode());
                    // 如果不一致时，大家都解除绑定
                    if (vh.bindController != null) {
                        //noinspection unchecked
                        vh.bindController.attach(false, vh);
                    }
                }
                Log.v("MvcList", "    - unbind " + this.position + " --> " + holder.getAdapterPosition() + "  @" + holder.hashCode());
                onDetach(position, item, holder);
                holder.bindController = null; // 解除双向绑定
            }
            holder = null; // 解除双向绑定
        }
    }

    protected abstract void onCreate(int position, DT item);

    /**
     * 创建一个新的Controller实例，可以将一些必要的参数传递给新实例。
     * 例如：主类在原型实例上设置点击事件的回调，原型复制新实例时，就可以将这个回调拷贝给新实例。
     * 以此类推，还可以复制更多的数据。
     */
    public MvcController<DT, VH> onClone() {
        try {
            //noinspection unchecked
            return this.getClass().newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 作为一个工厂方法，创建与之相关的ViewHolder
     */
    public abstract MvcViewHolder onCreateView(@NonNull ViewGroup parent, int viewType);

    /**
     * 在这个回调里把数据和VH绑定起来，设置View的内容
     */
    protected abstract void onAttach(int position, DT item, VH vh);

    /**
     * 在这个回调里解除数据与VH的绑定，清空View的内容，移除与View相关的监听
     */
    protected abstract void onDetach(int position, DT item, VH vh);

    /**
     * 用于销毁数据和释放资源
     */
    public abstract void onDestroy();

    public boolean isSameItem(Object thatItem) {
        return item.equals(thatItem);
    }
}
