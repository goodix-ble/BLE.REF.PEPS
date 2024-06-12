package com.goodix.ble.libuihelper.sublayout.list;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libcomx.util.CallUtil;

import java.util.ArrayList;
import java.util.List;

public class MvcAdapter extends RecyclerView.Adapter<MvcViewHolder> {
    private ArrayList<MvcController> controllerList = new ArrayList<>();
    private MvcController prototype; // 这个也会是list中的第一个元素

    @Nullable
    private DiffCallback diffCallback;

    public MvcAdapter(List items, MvcController prototype) {
        setup(items, prototype);
    }

    public MvcAdapter(MvcController prototype) {
        this.prototype = prototype;
    }

    public void dispose() {
        for (MvcController controller : controllerList) {
            controller.onDestroy();
        }
        controllerList.clear();
    }

    public MvcController getPrototype() {
        return prototype;
    }

    public MvcController getItem(int pos) {
        return controllerList.get(pos);
    }

    /**
     * 初始化多重视图绑定器：
     * 1、 将原始数据拷贝一份
     * 2、 建立Adapter并初始化RecyclerView
     */
    private void setup(List items, MvcController prototype) {
        this.prototype = prototype;
        controllerList.ensureCapacity(items.size());
        controllerList.clear();

        // 有数据时才处理
        if (prototype != null && !items.isEmpty()) {

            // 先使用原型作为第一个元素，
            prototype.create(items.get(0), 0);
            controllerList.add(prototype);

            // 其余的实例通过原型来创建
            for (int i = 1; i < items.size(); i++) {
                MvcController controller = prototype.onClone();
                controller.create(items.get(i), i);
                controllerList.add(controller);
            }
        }
    }

    @NonNull
    @Override
    public MvcViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MvcViewHolder mvcViewHolder = prototype.onCreateView(parent, viewType);
        Log.v("MvcList", "Create View Holder : @" + mvcViewHolder.hashCode() + "  " + CallUtil.trace(3));
        return mvcViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MvcViewHolder holder, int position) {
        if (holder.bindController != null) {
            if (holder.bindController == controllerList.get(position)) {
                Log.v("MvcList", "    - Need not to bind " + holder.bindController.position + " <--> " + position + "  @" + holder.hashCode());
                return;
            }

            //Log.v("MvcList", "    - unbind " + holder.bindController.position + " --> " + position + "  @" + holder.hashCode());
            //noinspection unchecked
            holder.bindController.attach(false, holder);
        }
        //Log.v("MvcList", "    + bind " + holder.bindController.position + " <-- " + position + "  @" + holder.hashCode());
        //noinspection unchecked
        controllerList.get(position).attach(true, holder);
    }

    @Override
    public int getItemCount() {
        return controllerList.size();
    }

    public void update(List items) {
        if (items == null) {
            // 清除所有controller
            int count = controllerList.size();
            for (MvcController controller : controllerList) {
                controller.onDestroy();
            }
            controllerList.clear();
            if (count > 0) {
                notifyItemRangeRemoved(0, count);
            }
            return;
        }

        // 保留存在的，销毁不要的，添加没有的
        ArrayList<MvcController> oldList = new ArrayList<>(controllerList); // 留着用来Diff
        ArrayList<MvcController> destroyList = new ArrayList<>(controllerList); // 用来临时存储，在这里面没有移除的元素都会 destroy

        controllerList.clear();
        controllerList.ensureCapacity(items.size());

        for (int i = 0; i < items.size(); i++) {
            Object thatItem = items.get(i);
            MvcController thatCtrl = null;

            // 先找已经存在的，如果有，后面会把它移动到列表中
            for (int k = 0; k < destroyList.size(); k++) {
                MvcController controller = destroyList.get(k);
                if (controller.isSameItem(thatItem)) {
                    thatCtrl = controller;
                    destroyList.remove(k); // 效率差
                    break;
                }
            }

            // 没有找到就创建一个新的 Controller
            if (thatCtrl == null) {
                thatCtrl = prototype.onClone();
                thatCtrl.create(items.get(i), i);
            } else {
                thatCtrl.position = i; // 更新它的位置
            }

            // 添加到最终的列表中
            controllerList.add(thatCtrl);
        }

        // 对比变更情况，实时视觉效果
        if (diffCallback == null) {
            diffCallback = new DiffCallback();
        }
        diffCallback.oldList = oldList;
        diffCallback.newList = controllerList;
        DiffUtil.calculateDiff(diffCallback, true)
                .dispatchUpdatesTo(this);

        // 销毁已经不需要的Controller
        for (MvcController controller : destroyList) {
            controller.onDestroy();
        }
    }

    static class DiffCallback extends DiffUtil.Callback {
        ArrayList<MvcController> oldList;
        ArrayList<MvcController> newList;

        @Override
        public int getOldListSize() {
            return oldList == null ? 0 : oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList == null ? 0 : newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // 直接判断是否是同一个Controller
            return newList.get(newItemPosition) == oldList.get(oldItemPosition);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // 每个Item都是实时更新的，不需要 update 这个操作来更新内容
            //return controllerList.get(oldItemPosition).isItemUpdated(newList.get(newItemPosition));
            return true;
        }
    }
}
