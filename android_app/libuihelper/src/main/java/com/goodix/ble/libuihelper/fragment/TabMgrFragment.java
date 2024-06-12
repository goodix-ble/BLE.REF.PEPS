package com.goodix.ble.libuihelper.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.goodix.ble.libuihelper.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于Tab管理的Fragment容器，具有一个列表用于显示Tab，具有一个FrameLayout用于显示内容。
 */
public class TabMgrFragment extends Fragment implements TabLayout.BaseOnTabSelectedListener {

    private static final String ARG_LAYOUT = "layout";
    private final String STATE_TAG_ITEM_FRAGMENT_LIST = "itemFragmentList";
    private final String STATE_TAG_CLASS = "class";
    private final String STATE_TAG_IS_ADDED = "isAdded";
    private final String STATE_TAG_SAVED_STATE = "savedState";
    private final String STATE_TAG_ARGUMENTS = "arguments";
    private final String STATE_TAG_SELECTED_TAB = "selectedTabPosition";
    private final String STATE_TAG_PRIVATE_ID = "privateId";

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
        void onBindTab(Context ctx, TabLayout.Tab tab, int position);

        void onUnbindTab(TabLayout.Tab tab, int position);

        /**
         * 当该选择状态发生变化时被调用
         *
         * @param selected true - 表示目前已经是选中状态了
         *                 false - 表示目前已经是未选中状态了
         */
        void onSelectionChanged(boolean selected);
    }

    private static final String TAG = "TabMgrFragment";

    private TabLayout tabLayout;

    // 如果元素实现 ITabItem 接口，就在对应的时机调用接口中的方法
    private ArrayList<Fragment> itemFragmentList = new ArrayList<>(32);
    private Handler handler;

    public static TabMgrFragment newInstance(@LayoutRes int layoutRes) {
        TabMgrFragment fragment = new TabMgrFragment();
        if (layoutRes != 0) {
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT, layoutRes);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public static TabMgrFragment newInstance(FragmentManager fm, @IdRes int containerViewId, @LayoutRes int layoutRes, String tag) {
        TabMgrFragment tabMgrFragment = null;
        if (fm != null) {
            for (Fragment fragment : fm.getFragments()) {
                if (fragment instanceof TabMgrFragment) {
                    tabMgrFragment = (TabMgrFragment) fragment;
                    break;
                }
            }
            if (tabMgrFragment == null) {
                tabMgrFragment = newInstance(layoutRes);
                // 需要 commitNow() ，否则，宿主后续的 addFragment() 判断会有问题。因为没有及时的从 savedInstanceState 恢复已经保存的Fragment而出问题
                fm.beginTransaction().add(containerViewId, tabMgrFragment, tag).commitNow();
            }
        }
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT, layoutRes);
        TabMgrFragment fragment = new TabMgrFragment();
        fragment.setArguments(args);
        return tabMgrFragment;
    }

    public void addFragment(Fragment fragment) {
        int position = itemFragmentList.indexOf(fragment);
        if (position < 0) {
            // 没有找到就创建一个
            position = itemFragmentList.size();
            itemFragmentList.add(fragment);

            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.newTab();
                tabLayout.addTab(tab, position);
                if (fragment instanceof ITabItem) {
                    ((ITabItem) fragment).onBindTab(requireContext(), tab, position);
                } else {
                    if (TextUtils.isEmpty(fragment.getTag())) {
                        tab.setText(fragment.getClass().getSimpleName());
                    } else {
                        tab.setText(fragment.getTag());
                    }
                }
                handler.postDelayed(tab::select, 100);
            }
        } else {
            // 已经存在就切换到它的位置
            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.getTabAt(position);
                if (tab != null) {
                    handler.postDelayed(tab::select, 100);
                }
            }
        }
    }

    /**
     * return an exist fragment. If absent, create and add a new fragment
     */
    public <T extends Fragment> T addFragment(Class<T> clazz) {
        T fragment = getFragment(clazz);
        if (fragment == null) {
            try {
                fragment = clazz.newInstance();
                addFragment(fragment);
            } catch (IllegalAccessException | java.lang.InstantiationException e) {
                e.printStackTrace();
            }
        } else {
            // switch to it
            if (tabLayout != null) {
                int position = itemFragmentList.indexOf(fragment);
                TabLayout.Tab tab = tabLayout.getTabAt(position);
                if (tab != null) {
                    handler.postDelayed(tab::select, 100);
                }
            }
        }
        return fragment;
    }


    public void deleteFragment(Fragment fragment) {
        int position = itemFragmentList.indexOf(fragment);
        if (position >= 0) {
            if (tabLayout != null) {
//                // 如果选中的项被删除了，就尝试选中前一项
//                // TabLayout自动处理了，但这里还是自行处理一遍，所以要先改变选择，再删除
//                TabLayout.Tab tab = tabLayout.getTabAt(position);
//                if (tab != null) {
//                    TabLayout.Tab nxtTab = null;
//                    int nxtPos = position - 1;
//                    if (nxtPos >= 0) {
//                        nxtTab = tabLayout.getTabAt(nxtPos);
//                    }
//                    if (!tab.isSelected()) {
//                        nxtTab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
//                    }
//                    if (nxtTab != null) {
//                        nxtTab.select();
//                    }
//                }

                FragmentManager manager = getChildFragmentManager();
                if (manager.getFragments().contains(fragment)) {
                    Log.e("+++", ">>>>> try remove: " + fragment.getClass().getSimpleName());
                    manager.beginTransaction().remove(fragment).commit();
                }

                if (fragment instanceof ITabItem) {
                    ((ITabItem) fragment).onUnbindTab(tabLayout.getTabAt(position), position);
                }
                tabLayout.removeTabAt(position);
            }
            itemFragmentList.remove(position);
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

    @NonNull
    public <T extends Fragment> List<T> getFragments(Class<T> clazz, List<T> out) {
        if (out == null) {
            out = new ArrayList<>(itemFragmentList.size());
        }
        for (Fragment fragment : itemFragmentList) {
            if (fragment.getClass().equals(clazz)) {
                //noinspection unchecked
                out.add((T) fragment);
            }
        }
        return out;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // restore previous fragments
        if (savedInstanceState != null) {
            final ClassLoader classLoader = getClass().getClassLoader();
            if (classLoader != null) {
                // 看看有没有暂存的Tab列表
                final Parcelable[] savedStateArray = savedInstanceState.getParcelableArray(STATE_TAG_ITEM_FRAGMENT_LIST);
                if (savedStateArray != null) {
                    final FragmentManager fm = getChildFragmentManager();
                    final List<Fragment> existFragments = fm.getFragments();

                    for (Parcelable parcelable : savedStateArray) {
                        Bundle bundle = (Bundle) parcelable;

                        // 读取保存的Fragment的信息
                        final String className = bundle.getString(STATE_TAG_CLASS);
                        final boolean isAdded = bundle.getBoolean(STATE_TAG_IS_ADDED, false);
                        Bundle arguments = bundle.getBundle(STATE_TAG_ARGUMENTS);
                        //SavedState savedState = bundle.getParcelable(STATE_TAG_SAVED_STATE);
                        Fragment fragment = null;

                        // 如果是已经添加的，就在系统提供的Fragment列表中找
                        if (isAdded) {
                            final String privateId = bundle.getString(STATE_TAG_PRIVATE_ID);
                            if (privateId != null) {
                                for (Fragment existfragment : existFragments) {
                                    final Bundle arg = existfragment.getArguments();
                                    if (arg != null) {
                                        final String existPrivateId = arg.getString(STATE_TAG_PRIVATE_ID);
                                        if (privateId.equals(existPrivateId)) {
                                            fragment = existfragment;
                                            break;
                                        }
                                    }
                                }
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
                            fragment.setArguments(arguments);
                            //fragment.setInitialSavedState(savedState);
                            addFragment(fragment);
                        }
                    }

                    // 移除由Activity添加的Fragment
                    final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    for (Fragment fragment : existFragments) {
                        transaction.hide(fragment);
                    }
                    transaction.commit();
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutRes = R.layout.libuihelper_fragment_tab_mgr;
        Bundle arguments = getArguments();
        if (arguments != null) {
            layoutRes = arguments.getInt(ARG_LAYOUT, R.layout.libuihelper_fragment_tab_mgr);
        }
        View root = inflater.inflate(layoutRes, container, false);

        handler = new Handler();

        tabLayout = root.findViewById(android.R.id.tabs);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.addOnTabSelectedListener(this);

        tabLayout.removeAllTabs();
        for (Fragment fragment : itemFragmentList) {
            TabLayout.Tab tab = tabLayout.newTab();

            tabLayout.addTab(tab);

            if (fragment instanceof ITabItem) {
                ((ITabItem) fragment).onBindTab(requireContext(), tab, itemFragmentList.size());
            } else {
                if (TextUtils.isEmpty(fragment.getTag())) {
                    tab.setText(fragment.getClass().getSimpleName());
                } else {
                    tab.setText(fragment.getTag());
                }
            }
        }

        if (savedInstanceState != null) {
            final int selectedTabPos = savedInstanceState.getInt(STATE_TAG_SELECTED_TAB, 0);
            final TabLayout.Tab tab = tabLayout.getTabAt(selectedTabPos);
            if (tab != null) {
                tab.select();
            }
        }

        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState); // 什么都没做。
        // 保存当前选择的 项
        Log.e(TAG, "onSaveInstanceState: getSelectedTabPosition() = " + tabLayout.getSelectedTabPosition());
        // 对于已经添加到 FragmentManager 的 Fragment，保存它们的状态，然后将它们从 FragmentManager 中移除，避免 FragmentActivity 的默认行为
        // 对于没有添加到 FragmentManager 的 Fragment，直接保存列表就可以了。
        Log.e(TAG, "onSaveInstanceState: getFragments() = ");
        final FragmentManager fm = getChildFragmentManager();

        Bundle[] savedStateArray = new Bundle[itemFragmentList.size()];

        for (int i = 0; i < itemFragmentList.size(); i++) {
            final Fragment fragment = itemFragmentList.get(i);
            final boolean added = fragment.isAdded();
            Bundle arguments = fragment.getArguments();

            Bundle bundle = savedStateArray[i] = new Bundle();

            bundle.putString(STATE_TAG_CLASS, fragment.getClass().getName());
            bundle.putBoolean(STATE_TAG_IS_ADDED, added);
            bundle.putBundle(STATE_TAG_ARGUMENTS, arguments);

            Log.e(TAG, "onSaveInstanceState:                + " + fragment.getClass().getSimpleName() + ", isAdded() = " + added);

            // 如果是已经添加的，系统会为它保存状态
            if (added) {
                //final SavedState savedState = fm.saveFragmentInstanceState(fragment);
                //bundle.putParcelable(STATE_TAG_SAVED_STATE, savedState);
                if (arguments == null) {
                    arguments = new Bundle();
                }
                final String privateId = UUID.randomUUID().toString();
                bundle.putString(STATE_TAG_PRIVATE_ID, privateId); // 各存储一份儿 ID ，便于恢复的时候进行匹配
                arguments.putString(STATE_TAG_PRIVATE_ID, privateId);
                fragment.setArguments(arguments);
            }
        }
        outState.putInt(STATE_TAG_SELECTED_TAB, tabLayout.getSelectedTabPosition());
        outState.putParcelableArray(STATE_TAG_ITEM_FRAGMENT_LIST, savedStateArray);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除与TAB的关联
        if (tabLayout != null) {
            for (int i = 0; i < itemFragmentList.size(); i++) {
                Fragment fragment = itemFragmentList.get(i);
                TabLayout.Tab tab = tabLayout.getTabAt(i);

                if (fragment instanceof ITabItem) {
                    ((ITabItem) fragment).onUnbindTab(tab, i);
                }
            }
            tabLayout.removeAllTabs();
        }
        // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        // 移除所有的Fragment
        // final List<Fragment> fragments = getChildFragmentManager().getFragments();
        // if (!fragments.isEmpty()) {
        //     final FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        //     for (Fragment fragment : fragments) {
        //         transaction.remove(fragment);
        //     }
        //     transaction.commit();
        // }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        Log.e(TAG, "onTabSelected() called with: tab = [" + tab + "]");
        Fragment selectedFragment = itemFragmentList.get(tab.getPosition());
        if (getChildFragmentManager().getFragments().contains(selectedFragment)) {
            Log.e("+++", ">>>>> try show: " + selectedFragment.getClass().getSimpleName());
            getChildFragmentManager().beginTransaction()
                    .show(selectedFragment)
                    .commit();
        } else {
            Log.e("+++", ">>>>> try add: " + selectedFragment.getClass().getSimpleName());
            getChildFragmentManager().beginTransaction()
                    .add(android.R.id.tabcontent, selectedFragment)
                    .commit();
        }
        if (selectedFragment instanceof ITabItem) {
            ((ITabItem) selectedFragment).onSelectionChanged(true);
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        int position = tab.getPosition();
        Log.e(TAG, "onTabUnselected() called with: tab = [" + position + "]");
        // 删除Tab时，如果被删除的Tab时选中的，那么也会调用这个函数。此时Tab的position为-1
        if (position >= 0) {
            Fragment selectedFragment = itemFragmentList.get(position);
            if (getChildFragmentManager().getFragments().contains(selectedFragment)) {
                Log.e("+++", ">>>>> try hide: " + selectedFragment.getClass().getSimpleName());
                getChildFragmentManager().beginTransaction()
                        .hide(selectedFragment)
                        .commit();
            }
            if (selectedFragment instanceof ITabItem) {
                ((ITabItem) selectedFragment).onSelectionChanged(false);
            }
        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        Log.e(TAG, "onTabReselected() called with: tab = [" + tab + "]");
    }
}
