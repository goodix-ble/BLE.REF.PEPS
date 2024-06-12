package com.goodix.ble.libuihelper.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libcomx.ILogger;
import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.sublayout.list.IMvcListView;
import com.goodix.ble.libuihelper.sublayout.list.MvcAdapter;
import com.goodix.ble.libuihelper.sublayout.list.MvcController;
import com.goodix.ble.libuihelper.sublayout.list.MvcViewHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 通用的基于Tab管理的Fragment容器，负责Tab的管理和内容的切换。
 * <p>
 * 功能：
 * # 锁定：锁定后，不可以选择其它的Tab
 * #
 */
@SuppressWarnings({"WeakerAccess"})
public class TabContainer {
    /**
     * 持有TabContainer的Activity或者Fragment可以实现该接口，这样，TabFragment就可以找到TabContainer并添加Fragment
     * 在TabFragment中，可以调用{@link }
     */
    public interface ITabHost {
        @Nullable
        TabContainer getTabContainer();
    }

    /**
     * TabFragment显示的内容，具备了Tab的引用，可以管理Tab标签的内容。
     */
    public interface ITabItem {

        /**
         * 被添加到Tab列表中，并且分配了Tab对象时调用，子类可以保存引用，并且修改Tab要显示的内容。
         * 这个Tab对象在 {@link #onUnbindTab} 被调用前都为有效状态。
         * 例如，修改Tab让其显示正确的标题。
         * 例如，在需要的时候，动态修改Tab的文字。
         */
        View onBindTab(TabContainer tabContainer, Context ctx, ViewGroup parent, int position);

        void onUnbindTab(View tab, int position);

        /**
         * 当该选择状态发生变化时被调用
         *
         * @param selected true - 表示目前已经是选中状态了
         *                 false - 表示目前已经是未选中状态了
         */
        void onSelectionChanged(boolean selected);
    }

    private static final String TAG = "TabContainer";

    private final String STATE_TAG_ITEM_FRAGMENT_LIST = "itemFragmentList";
    private final String STATE_TAG_CLASS = "class";
    private final String STATE_TAG_IS_ADDED = "isAdded";
    private final String STATE_TAG_SELECTED_TAB = "selectedTabPosition";
    private final String STATE_TAG_WHO = "mWho";

    // 如果元素实现 ITabItem 接口，就在对应的时机调用接口中的方法
    @NonNull
    private final ArrayList<Fragment> itemFragmentList = new ArrayList<>(32);
    @NonNull
    private final MvcAdapter tabAdapter = new MvcAdapter(itemFragmentList, new TabCtrl());
    @Nullable
    private TabCtrl selectedTab;

    private FragmentManager fragmentManager;
    private Context mCtx;

    @Nullable
    private IMvcListView tabContainerView;
    @Nullable
    private FrameLayout contentContainerView;

    @Nullable
    private ILogger logger;

    //private Handler handler;
    public static final int EVT_SELECT_TAB = 945;
    private Event<Integer> eventSelectTab = new Event<>(this, EVT_SELECT_TAB);
    private boolean forbidSwitch = false;

    public TabContainer(Context ctx, FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        this.mCtx = ctx;
    }

    @Nullable
    public static TabContainer obtain(Fragment tabFragment) {
        if (tabFragment != null) {
            final Activity activity = tabFragment.getActivity();
            if (activity instanceof ITabHost) {
                return ((ITabHost) activity).getTabContainer();
            }
            final Fragment parent = tabFragment.getParentFragment();
            if (parent instanceof ITabHost) {
                return ((ITabHost) parent).getTabContainer();
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void setContainerView(RecyclerView tabContainerView, FrameLayout contentContainerView) {
        setContainerView(new TabOnRecyclerView(tabContainerView), contentContainerView);
    }

    public void setContainerView(IMvcListView tabContainerView, @NonNull FrameLayout contentContainerView) {
        this.tabContainerView = tabContainerView;
        this.contentContainerView = contentContainerView;

        if (tabContainerView != null) {
            tabContainerView.setAdapter(tabAdapter);
        }

        // 重新出发一次视图更新
        if (selectedTab != null) {
            selectedTab.notifyContainerChanged();
        }
    }

    public void setLogger(@Nullable ILogger logger) {
        this.logger = logger;
    }

    public int addFragment(Fragment fragment) {
        int position = itemFragmentList.indexOf(fragment);
        if (position < 0) {
            // 没有找到就创建一个
            position = itemFragmentList.size();
            itemFragmentList.add(fragment);
            tabAdapter.update(itemFragmentList); // 通知创建Controller
        }
        return position;
    }

    /**
     * return an exist fragment. If absent, create and add a new fragment
     */
    @SuppressWarnings("unused")
    public <T extends Fragment> T addFragment(Class<T> clazz) {
        T fragment = getFragment(clazz);
        if (fragment == null) {
            try {
                fragment = clazz.newInstance();
                addFragment(fragment);
            } catch (IllegalAccessException | java.lang.InstantiationException e) {
                e.printStackTrace();
            }
        }
        return fragment;
    }

    @SuppressWarnings("unused")
    public void openFragment(Fragment fragment) {
        // 切换到它的位置
        setSelection(addFragment(fragment));
    }

    @SuppressWarnings("unused")
    public <T extends Fragment> T openFragment(Class<T> clazz) {
        T fragment = addFragment(clazz);
        int position = itemFragmentList.indexOf(fragment);
        if (position >= 0) {
            setSelection(position);
        }
        return fragment;
    }

    public void openFragment(int position) {
        final int itemCount = tabAdapter.getItemCount();
        if (position < 0) {
            position += itemCount;
        }
        if (position < 0) {
            position = 0;
        }
        if (position >= itemCount) {
            position = itemCount - 1;
        }
        if (itemCount > 0) {
            setSelection(position);
        }
    }

    @SuppressWarnings("unused")
    public void removeFragment(Fragment fragment) {
        // 判断是否是有效的Fragment
        int position = itemFragmentList.indexOf(fragment);
        if (position >= 0) {
            // 先选举到下一个项后才开始移除工作
            // 判断是否要选举下一个被选择的对象
            if (selectedTab != null) {
                if (selectedTab.getFragment() == fragment) {
                    // 关闭的就是当前项，选举下一个被选择的项
                    final int itemCount = tabAdapter.getItemCount();
                    // 至少要有2个项才选举，删除后还剩一个
                    if (itemCount > 1) {
                        final int curPos = selectedTab.getPosition();
                        // 如果是最后一个，就用前一个
                        if (curPos == itemCount - 1) {
                            setSelection(curPos - 1);
                        } else {
                            // 否则就用后一个
                            setSelection(curPos + 1);
                        }
                    } else {
                        // 选择为空
                        selectedTab = null;
                    }
                    //} else {
                    // 只要不是当前选择的项，都会自动更新position，保持选择的状态不变
                    // 不影响当前已选择
                    // 不处理
                }
                //} else {
                // 本来就没有已选择的项，所以不需要选举
            }

            // 最后才移除
            itemFragmentList.remove(position);
            tabAdapter.update(itemFragmentList); // 用update让adapter销毁移除掉的Controller
        }
    }

    @Nullable
    public <T extends Fragment> T getFragment(Class<T> clazz) {
        for (Fragment fragment : itemFragmentList) {
            if (fragment.getClass().equals(clazz)) {
                //noinspection unchecked
                return (T) fragment;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    @NonNull
    public <T extends Fragment> List<T> getFragments(@Nullable Class<T> clazz, @Nullable List<T> out) {
        if (out == null) {
            out = new ArrayList<>(itemFragmentList.size());
        }
        for (Fragment fragment : itemFragmentList) {
            if (clazz == null || fragment.getClass().equals(clazz)) {
                //noinspection unchecked
                out.add((T) fragment);
            }
        }
        return out;
    }

    @SuppressWarnings("unused")
    public void restoreInstanceState(@Nullable Bundle savedInstanceState) {
        // restore previous fragments
        if (savedInstanceState != null) {
            final ClassLoader classLoader = getClass().getClassLoader();
            if (classLoader != null) {
                // 看看有没有暂存的Tab列表
                final Parcelable[] savedStateArray = savedInstanceState.getParcelableArray(STATE_TAG_ITEM_FRAGMENT_LIST);
                if (savedStateArray != null) {
                    final FragmentManager fm = fragmentManager;
                    final List<Fragment> existFragments = fm.getFragments();
                    final HashMap<String, Fragment> existFragmentsMap = new HashMap<>(existFragments.size());

                    // 获取fragment本身具有的UUID
                    Field mWho = null;
                    try {
                        mWho = Fragment.class.getDeclaredField("mWho");
                        mWho.setAccessible(true);
                    } catch (NoSuchFieldException ignored) {
                    }
                    for (Fragment f : existFragments) {
                        String privateId = "" + f.getId() + f.getTag();
                        if (mWho != null) {
                            try {
                                privateId = (String) mWho.get(f);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        existFragmentsMap.put(privateId, f);
                        if (logger != null) {
                            logger.v(TAG, "restoreInstanceState(): Found existFragment: " + privateId + "  " + f.getTag());
                        }
                    }

                    for (Parcelable parcelable : savedStateArray) {
                        Bundle bundle = (Bundle) parcelable;

                        // 读取保存的Fragment的信息
                        final String className = bundle.getString(STATE_TAG_CLASS);
                        final boolean isAdded = bundle.getBoolean(STATE_TAG_IS_ADDED, false);
                        //SavedState savedState = bundle.getParcelable(STATE_TAG_SAVED_STATE);
                        Fragment fragment = null;

                        // 如果是已经添加的，就在系统提供的Fragment列表中找
                        if (isAdded) {
                            final String privateId = bundle.getString(STATE_TAG_WHO);
                            if (privateId != null) {
                                fragment = existFragmentsMap.remove(privateId);
                            }
                        } else {
                            // 根据信息创建
                            if (className != null) {
                                try {
                                    final Class<?> fragmentClass = classLoader.loadClass(className);
                                    if (fragmentClass != null) {
                                        fragment = (Fragment) fragmentClass.newInstance();
                                    }
                                } catch (ClassNotFoundException | IllegalAccessException | java.lang.InstantiationException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (fragment != null) {
                            addFragment(fragment);
                            if (logger != null) {
                                logger.v(TAG, "restoreInstanceState(): addFragment: " + fragment);
                            }
                        }
                    }
                }
            }

            int selectPos = savedInstanceState.getInt(STATE_TAG_SELECTED_TAB, 0);
            setSelection(selectPos);
        }
    }

    @SuppressWarnings("unused")
    public void saveInstanceState(@NonNull Bundle outState) {
        // 对于已经添加到 FragmentManager 的 Fragment，保存它们的状态，然后将它们从 FragmentManager 中移除，避免 FragmentActivity 的默认行为
        // 对于没有添加到 FragmentManager 的 Fragment，直接保存列表就可以了。
        // 保存当前选择的项
        final FragmentManager fm = fragmentManager;

        Bundle[] savedStateArray = new Bundle[itemFragmentList.size()];

        // 获取fragment本身具有的UUID
        Field mWho = null;
        try {
            mWho = Fragment.class.getDeclaredField("mWho");
            mWho.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }

        for (int i = 0; i < itemFragmentList.size(); i++) {
            final Fragment fragment = itemFragmentList.get(i);
            final boolean added = fragment.isAdded();

            Bundle bundle = savedStateArray[i] = new Bundle();

            bundle.putString(STATE_TAG_CLASS, fragment.getClass().getName());
            bundle.putBoolean(STATE_TAG_IS_ADDED, added);

            if (logger != null) {
                logger.v(TAG, "onSaveInstanceState: " + fragment + ", isAdded() = " + added);
            }

            // 如果是已经添加的，系统会为它保存状态
            if (added) {
                //final SavedState savedState = fm.saveFragmentInstanceState(fragment);
                //bundle.putParcelable(STATE_TAG_SAVED_STATE, savedState);
                String privateId = "" + fragment.getId() + fragment.getTag();
                if (mWho != null) {
                    try {
                        privateId = (String) mWho.get(fragment);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                bundle.putString(STATE_TAG_WHO, privateId); // 各存储一份儿 ID ，便于恢复的时候进行匹配
                if (logger != null) {
                    logger.v(TAG, "onSaveInstanceState: " + fragment.getClass().getSimpleName() + ", privateId = " + privateId);
                }
            }
        }
        if (selectedTab != null) {
            outState.putInt(STATE_TAG_SELECTED_TAB, selectedTab.getPosition());
        }
        outState.putParcelableArray(STATE_TAG_ITEM_FRAGMENT_LIST, savedStateArray);
    }

    @SuppressWarnings("unused")
    public void destroy() {
        //deselectTab(selectedTab);
        selectedTab = null;
        // 移除与TAB的关联
        tabAdapter.dispose();
    }

    public int getSelection() {
        TabCtrl selectedTab = this.selectedTab;
        if (selectedTab != null) {
            return selectedTab.getPosition();
        } else {
            return -1;
        }
    }

    public void setSelection(int position) {
        if (logger != null) {
            logger.d(TAG, "<---------- setSelection(): tab = [" + position + "] ---------->");
        }

        if (position >= tabAdapter.getItemCount() || position < 0) {
            return;
        }

        final TabCtrl nextTab = (TabCtrl) tabAdapter.getItem(position);

        // 先对之前选择的项取消选择
        // 考虑幂等性
        if (selectedTab != null && selectedTab != nextTab) {
            deselectTab(selectedTab);
        }

        selectedTab = nextTab;

        nextTab.setSelected(true);

        if (tabContainerView != null) {
            tabContainerView.scrollToPosition(position, true);
        }
    }

    @SuppressWarnings("unused")
    public void clearSelection() {
        // 考虑幂等性
        if (selectedTab != null) {
            // 取消全部选择
            deselectTab(selectedTab);
        }
        selectedTab = null;
    }

    public void setForbidSwitch(boolean forbidSwitch) {
        this.forbidSwitch = forbidSwitch;
    }

    /**
     * Tab选择事件，该事件会发射在选择之前，
     * 可以通过在该事件的响应函数中阻止切换。
     * 通过{@link #setForbidSwitch(boolean)}设置禁用切换来阻止切换。
     */
    public Event<Integer> evtSelectTab() {
        return eventSelectTab;
    }

    private void deselectTab(TabCtrl tab) {
        // 删除Tab时，如果被删除的Tab时选中的，那么也会调用这个函数。此时Tab的position为-1
        if (tab != null) {
            tab.setSelected(false);
        }
    }

    private ViewGroup createTabViewWrapper() {
        ViewGroup.LayoutParams lp;
        if (tabContainerView != null) {
            lp = tabContainerView.generateItemLayoutParams(0);
        } else {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final FrameLayout frameLayout = new FrameLayout(mCtx);
        frameLayout.setLayoutParams(lp);
        final int padding = (int) (mCtx.getResources().getDisplayMetrics().density * 6);
        frameLayout.setPadding(padding, padding, padding, padding);
        return frameLayout;
    }

    /**
     * 当Tab被点击时，通过该方法请求获得选择，可能会被拒绝。
     * 例如，容器被锁定了，请求就一定会被拒绝。
     */
    private void requestSelection(int pos) {
        eventSelectTab.postEvent(pos);
        if (!forbidSwitch) {
            setSelection(pos);
        }
    }

    class TabCtrl extends MvcController<Fragment, TabViewHolder> implements View.OnClickListener {
        @Nullable
        ITabItem tabItem;
        boolean isSelected = false;
        boolean isAdded = false; // Fragment.isAdded() 具有延迟性，所以做个标记，提高实时性，但是就不允许外部修改自己的添加状态

        // 每个Tab自己保存和管理自己的View，不参与共享
        View tabView;

        void setSelected(boolean selected) {
            // 先考虑幂等性
            if (isSelected == selected) {
                return;
            }
            isSelected = selected;

            // 切换Fragment
            notifyContainerChanged();

            // 最后更新状态
            if (tabView != null) {
                tabView.setSelected(selected);
            }
            if (tabItem != null) {
                tabItem.onSelectionChanged(selected);
            }
        }

        void notifyContainerChanged() {
            if (contentContainerView != null) {
                final ILogger log = logger;
                if (isSelected) {
                    if (log != null) {
                        log.v(TAG, "select tab = [" + getPosition() + "]");
                    }
                    // 已添加就show出来，否则添加
                    if (isAdded) {
                        if (log != null) {
                            log.v(TAG, ">>>>> try show: " + item.getClass().getSimpleName() + " @" + item.hashCode());
                        }
                        fragmentManager.beginTransaction().show(item).commit();
                    } else {
                        if (log != null) {
                            log.v(TAG, ">>>>> try add: " + item.getClass().getSimpleName() + " @" + item.hashCode());
                        }
                        fragmentManager.beginTransaction().add(contentContainerView.getId(), item).commit();
                        isAdded = true;
                    }
                } else {
                    // 直接hide掉，能到这个分支，说明前面的条件已经执行过了
                    if (log != null) {
                        log.v(TAG, "deselect tab = [" + getPosition() + "]");
                        log.v(TAG, ">>>>> try hide: " + item.getClass().getSimpleName() + " @" + item.hashCode());
                    }
                    if (isAdded) {
                        fragmentManager.beginTransaction().hide(item).commit();
                    }
                }
            }
        }

        int getPosition() {
            return position;
        }

        public Fragment getFragment() {
            return item;
        }

        @Override
        public MvcController<Fragment, TabViewHolder> onClone() {
            return new TabCtrl();
        }

        @Override
        protected void onCreate(int position, Fragment item) {
            if (item instanceof ITabItem) {
                tabItem = (ITabItem) item;
            }
            // 在需要创建Tab的标题的时候才调用 onBindTab()
            isAdded = item.isAdded();
        }

        @Override
        public MvcViewHolder onCreateView(@NonNull ViewGroup parent, int viewType) {
            return new TabViewHolder(createTabViewWrapper());
        }

        @Override
        protected void onAttach(int position, Fragment item, TabViewHolder tabViewHolder) {
            View tabView = this.tabView;
            if (tabView == null) {
                final Context ctx = tabViewHolder.wrapper.getContext();
                // 如果Fragment能自己管理标签就自己管理
                if (tabItem != null) {
                    tabView = tabItem.onBindTab(TabContainer.this, ctx, tabViewHolder.wrapper, position);
                }
                if (tabView == null) {
                    tabView = LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_1, tabViewHolder.wrapper, false);
                    ((TextView) tabView).setTextColor(ContextCompat.getColorStateList(ctx, R.color.libuihelper_selectable_dark_gray));
                    ((TextView) tabView).setMaxLines(1);
                    ((TextView) tabView).setEllipsize(TextUtils.TruncateAt.END);
                    String name = item.getTag();
                    if (name == null) {
                        name = item.getClass().getSimpleName();
                    }
                    ((TextView) tabView).setText(name);
                }
                this.tabView = tabView;
                this.tabView.setSelected(isSelected);
            }

//            StringBuilder sb = new StringBuilder(1024);
//            sb.append("attach wrapper: @").append(tabViewHolder.wrapper.hashCode());
//            if (tabViewHolder.wrapper.getChildCount() > 0) {
//                sb.append(" child=").append(tabViewHolder.wrapper.getChildCount()).append(", [0]=@");
//                sb.append(tabViewHolder.wrapper.getChildAt(0).hashCode());
//            }
//            sb.append("  tabView: @").append(tabView.hashCode());
//            sb.append("  pos: ").append(position);
//            sb.append("  posHolder: ").append(tabViewHolder.getAdapterPosition());
//            sb.append("  posTab: ").append(this.position);
//            Log.i("TabContainer", sb.toString());

            tabViewHolder.wrapper.addView(tabView);
            tabViewHolder.wrapper.setOnClickListener(this);
        }

        @Override
        protected void onDetach(int position, Fragment item, TabViewHolder tabViewHolder) {
            tabViewHolder.wrapper.setOnClickListener(null);
            tabViewHolder.wrapper.removeView(tabView);

//            StringBuilder sb = new StringBuilder(1024);
//            sb.append("detach wrapper: @").append(tabViewHolder.wrapper.hashCode());
//            if (tabViewHolder.wrapper.getChildCount() > 0) {
//                sb.append(" child=").append(tabViewHolder.wrapper.getChildCount()).append(", [0]=@");
//                sb.append(tabViewHolder.wrapper.getChildAt(0).hashCode());
//            }
//            sb.append("  tabView: @").append(tabView.hashCode());
//            sb.append("  pos: ").append(position);
//            sb.append("  posHolder: ").append(tabViewHolder.getAdapterPosition());
//            sb.append("  posTab: ").append(this.position);
//            Log.i("TabContainer", sb.toString());
        }

        @Override
        public void onDestroy() {
            // 如果创建了TabView就通知解除绑定
            if (tabItem != null && this.tabView != null) {
                tabItem.onUnbindTab(tabView, position);
            }

            // 已添加就移除掉
            if (item.isAdded()) {
                if (logger != null) {
                    logger.v(TAG, ">>>>> try remove: " + item.getClass().getSimpleName() + " @" + item.hashCode());
                }
                fragmentManager.beginTransaction().remove(item).commit();
                isAdded = false;
            }

            // 最后移除tabView
            if (tabView != null) {
                final ViewParent parent = tabView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(tabView);
                }
            }
        }

        @Override
        public void onClick(View v) {
            requestSelection(position);
        }
    }

    static class TabViewHolder extends MvcViewHolder {
        final ViewGroup wrapper;

        public TabViewHolder(@NonNull ViewGroup wrapper) {
            super(wrapper);
            this.wrapper = wrapper;
        }
    }
}
