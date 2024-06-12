package com.goodix.ble.libuihelper.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goodix.ble.libcomx.file.FileUtil;
import com.goodix.ble.libuihelper.R;
import com.goodix.ble.libuihelper.sublayout.list.MvcAdapter;
import com.goodix.ble.libuihelper.sublayout.list.MvcController;
import com.goodix.ble.libuihelper.sublayout.list.MvcViewHolder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * 内嵌文件浏览器：
 * 1、打开指定目录（初始目录）
 * 2、限定根目录，只能浏览指定根目录下的内容
 * 3、具备发送文件的功能，通过系统的分享功能发送。需要创建FileProvider来兼容安卓7.0以上的系统
 * 4、具备文件夹发送功能，压缩文件夹并发送
 * 5、能够删除文件和文件夹
 * 6、能够重命名文件和文件夹
 * 7、支持以对话框的方式显示
 * 8、能够选择目标文件
 * 9、支持横屏竖屏
 * 10、支持文件名搜索
 * A1、附加直接浏览Log文件的功能，对于".log"文件采用SimpleLogFragment来显示。
 * A2、附加HTTP二维码分享功能
 * A3、附加HTTP文件上传功能
 */
public class FileExplorerFragment extends ClosableTabFragment implements TabContainer.ITabItem, View.OnClickListener, FilenameFilter, Comparator<File> {
    private static String MENU_OPEN = "_打开";   // File Dir
    private static String MENU_SHARE = "_分享";  // File Dir
    private static String MENU_DELETE = "_删除"; // File
    private static String MENU_ZIP = "_压缩";    // File Dir
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final String ARG_READ_ONLY = "readonly";
    private static final String ARG_SEL_FILE = "canSelectFile";
    private static final String ARG_SEL_DIR = "canSelectDir";
    private static final String ARG_SEL_MULTI = "canSelectMulti";
    private static final String ARG_FILE_EXT = "fileExtension";
    private static final String ARG_ROOT_DIR = "rootDir";
    private static final String ARG_DEFAULT_DIR = "defaultDir";

    public interface CB {
        /**
         * 返回已经选择的文件列表。
         *
         * @param dlg   对话框实例
         * @param files 如果取消选择，列表为空。如果单选，列表只会包含一个元素。如果多项，列表可能包含0个或多个文件
         */
        void onSelectedFile(FileExplorerFragment dlg, @NonNull List<File> files);
    }

    /**
     * 打开指定目录（初始目录）
     * 如果没有指定，就打开外部存储的files文件夹
     */
    private File rootDir;

    /**
     * 默认目录，推荐使用的目录，用户可以切换至其他目录再使用
     */
    @Nullable
    private String defaultDir;

    private ArrayList<File> historyList = new ArrayList<>(64);

    /**
     * 指定是否以只读的方式浏览文件夹。false时，可以对文件和文件夹进行移动、重命名以及删除。
     */
    private boolean readonly;

    private boolean canSelectFile;
    private boolean canSelectDir;
    private boolean canSelectMulti;
    private String fileExtension;

    private File currentDir;
    private HashMap<String, File> selectedFiles = new HashMap<>(64);
    private ArrayList<File> fileList = new ArrayList<>(64);
    private boolean sortByLastModify = false;

    private MvcAdapter fileAdapter = new MvcAdapter(fileList, new MM(this));
    private TextView pathTv;
    private ImageButton sortBtn;
    private ImageButton backBtn;
    private Context mCtx;

    private CB callback;

    public FileExplorerFragment setRootDir(File rootDir) {
        this.rootDir = rootDir;
        getBundle().putSerializable(ARG_ROOT_DIR, rootDir);
        return this;
    }

    public FileExplorerFragment setDefaultDir(String relativePath) {
        this.defaultDir = relativePath;
        getBundle().putSerializable(ARG_DEFAULT_DIR, relativePath);
        return this;
    }

    public FileExplorerFragment setSelectionType(boolean file, boolean dir) {
        canSelectFile = file;
        canSelectDir = dir;
        Bundle arguments = getBundle();
        arguments.putBoolean(ARG_SEL_FILE, file);
        arguments.putBoolean(ARG_SEL_DIR, dir);
        return this;
    }

//    public FileExplorerFragment setMultiSelection(boolean enable) {
//        canSelectMulti = enable;
//        getBundle().putBoolean(ARG_SEL_MULTI, enable);
//        return this;
//    }

    public FileExplorerFragment setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        getBundle().putString(ARG_FILE_EXT, fileExtension);
        return this;
    }

    public FileExplorerFragment setReadonly(boolean readonly) {
        this.readonly = readonly;
        getBundle().putBoolean(ARG_READ_ONLY, readonly);
        return this;
    }

    public FileExplorerFragment setCallback(CB callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle bundle = getBundle();
        canSelectFile = bundle.getBoolean(ARG_SEL_FILE, false);
        canSelectDir = bundle.getBoolean(ARG_SEL_DIR, false);
        canSelectMulti = bundle.getBoolean(ARG_SEL_MULTI, false);
        fileExtension = bundle.getString(ARG_FILE_EXT, null);
        readonly = bundle.getBoolean(ARG_READ_ONLY, false);
        rootDir = (File) bundle.getSerializable(ARG_ROOT_DIR);
        defaultDir = bundle.getString(ARG_DEFAULT_DIR);

        MENU_OPEN = getString(R.string.libuihelper_open);
        MENU_SHARE = getString(R.string.libuihelper_share);
        MENU_DELETE = getString(R.string.libuihelper_delete);
        MENU_ZIP = getString(R.string.libuihelper_zip);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mCtx = inflater.getContext();
        final View root = inflater.inflate(R.layout.libuihelper_fragment_file_explorer, container, false);

        RecyclerView fileRv = root.findViewById(R.id.libuihelper_fragment_file_explorer_file_rv);
        fileRv.setLayoutManager(new LinearLayoutManager(mCtx));
        fileRv.setAdapter(fileAdapter);

        pathTv = root.findViewById(R.id.libuihelper_fragment_file_explorer_path_tv);
        sortBtn = root.findViewById(R.id.libuihelper_fragment_file_explorer_sort_btn);
        backBtn = root.findViewById(R.id.libuihelper_fragment_file_explorer_backward_btn);

        sortBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);

        if (rootDir == null) {
            rootDir = mCtx.getExternalFilesDir(null);
        } else {
            // 判断是否是访问的sdcard，如果是，则需要进行权限判断，并根据需要申请权限
            final boolean noReadSdcard = ContextCompat.checkSelfPermission(mCtx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
            final boolean noWriteSdcard = ContextCompat.checkSelfPermission(mCtx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
            if (noReadSdcard || noWriteSdcard) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 998);
            }
        }


        if (rootDir != null) {
            if (defaultDir != null) {
                final File defaultDir = new File(rootDir, this.defaultDir);
                if (defaultDir.exists()) {
                    historyList.add(rootDir);
                    openFile(defaultDir);
                } else {
                    openFile(rootDir);
                }
            } else {
                openFile(rootDir);
            }
        }

        if (callback == null) {
            final FragmentActivity activity = getActivity();
            if (activity instanceof CB) {
                callback = (CB) activity;
            } else {
                final Fragment parentFragment = getParentFragment();
                if (parentFragment instanceof CB) {
                    callback = (CB) parentFragment;
                }
            }
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        final Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();

                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = (int) (displayMetrics.widthPixels * 0.8); // 宽度
                lp.height = (int) (displayMetrics.heightPixels * 0.8); // 高度
                window.setAttributes(lp);
            }
        }

        selectedFiles.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            refreshList();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void openFile(File dst) {
        if (dst == null) {
            return;
        }

        if (dst.isDirectory()) {
            if (currentDir != null) {
                historyList.add(currentDir);
            }
            currentDir = dst;

            pathTv.setText(dst.getAbsolutePath());

            refreshList();

        } else if (dst.isFile()) {
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(mCtx, mCtx.getPackageName() + ".FileProvider", dst);
            } else {
                uri = Uri.fromFile(dst);
            }
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setDataAndType(uri, getMIME(dst));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.libuihelper_please_select)));
        }
    }

    private void backward() {
        if (!historyList.isEmpty()) {
            File dst = historyList.remove(historyList.size() - 1);
            if (dst != null) {
                currentDir = dst;
                pathTv.setText(dst.getAbsolutePath());
                refreshList();
            }
        }
    }

    private void refreshList() {
        File dst = currentDir;
        if (dst.isDirectory()) {
            fileList.clear();
            final File[] files = dst.listFiles(this);
            if (files != null) {
                Collections.addAll(fileList, files);
            }
            if (fileList.size() > 1) {
                Collections.sort(fileList, this);
            }
            fileAdapter.update(fileList);
        }

        backBtn.setVisibility(historyList.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }

    private String getMIME(File dst) {
        String name = dst.getName();
        String type = "*/*";
        int startPos = name.lastIndexOf('.');
        if (startPos < 0) {
            return type;
        } else {
            if (name.lastIndexOf(".png") == startPos) {
                type = "image/png";
            } else if (name.lastIndexOf(".jpg") == startPos) {
                type = "image/jpeg";
            } else if (name.lastIndexOf(".jpeg") == startPos) {
                type = "image/jpeg";
            } else if (name.lastIndexOf(".bmp") == startPos) {
                type = "image/bmp";
            } else if (name.lastIndexOf(".txt") == startPos) {
                type = "text/plain";
            } else if (name.lastIndexOf(".log") == startPos) {
                type = "text/plain";
            } else if (name.lastIndexOf(".json") == startPos) {
                type = "text/plain";
            } else if (name.lastIndexOf(".csv") == startPos) {
                type = "application/vnd.ms-excel";
            } else if (name.lastIndexOf(".zip") == startPos) {
                type = "application/octet-stream";
            } else if (name.lastIndexOf(".apk") == startPos) {
                type = "application/vnd.android.package-archive";
            }
        }
        return type;
    }

    @Override
    public void onClick(View v) {
        if (v == backBtn) {
            backward();
        }
        if (v == sortBtn) {
            sortByLastModify = !sortByLastModify;
            refreshList();
            if (sortByLastModify) {
                DrawableCompat.setTint(sortBtn.getDrawable(), ContextCompat.getColor(v.getContext(), R.color.libuihelper_selectable_dark_gray));
            } else {
                TypedValue typedValue = new TypedValue();
                v.getContext().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
                DrawableCompat.setTint(sortBtn.getDrawable(), typedValue.data);
            }
        }
    }

    @Override
    public boolean accept(File dir, String name) {
        // 如果只选文件夹，那么就只显示文件夹
        if (canSelectDir && !canSelectFile) {
            return new File(dir, name).isDirectory();
        }
        // 有后缀名过滤时，只显示文件夹和指定后缀名的文件
        if (fileExtension != null && new File(dir, name).isFile()) {
            final int dot = name.lastIndexOf('.');
            if (dot != -1) {
                return name.lastIndexOf(fileExtension) == dot;
            }
        }
        return true;
    }

    @Override
    public int compare(File o1, File o2) {
        if (sortByLastModify) {
            return Long.compare(o2.lastModified(), o1.lastModified());
        }
        return o1.getName().compareToIgnoreCase(o2.getName());
    }

    private void onFileClicked(File target) {
        if (target == null) {
            return;
        }

        if (canSelectFile || canSelectDir) {
            if (!canSelectMulti) {
                selectedFiles.clear();
            }

            // 如果是为了选择文件，那么就返回选择的文件
            final String path = target.getAbsolutePath();
            if (target.isDirectory()) {
                if (canSelectDir) {
                    if (selectedFiles.containsKey(path)) {
                        selectedFiles.remove(path);
                    } else {
                        selectedFiles.put(path, target);
                    }
                } else {
                    openFile(target);
                }
            } else if (target.isFile()) {
                if (canSelectFile) {
                    if (selectedFiles.containsKey(path)) {
                        selectedFiles.remove(path);
                    } else {
                        selectedFiles.put(path, target);
                    }
                } else {
                    openFile(target);
                }
            }

            if (!canSelectMulti && !selectedFiles.isEmpty()) {
                dismiss();
                if (callback != null) {
                    callback.onSelectedFile(this, new ArrayList<>(selectedFiles.values()));
                }
            }

        } else {
            // 如果是浏览模式，就打开文件
            openFile(target);
        }
    }

    private Bundle getBundle() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            arguments = new Bundle();
            setArguments(arguments);
        }
        return arguments;
    }

    static class MM extends MvcController<File, MMVH> implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private ArrayList<String> menuItems = new ArrayList<>(8);
        private FileExplorerFragment host;
        private String lastModifiedDatetime;
        private String fileDesc;

        MM(FileExplorerFragment host) {
            this.host = host;
        }

        @Override
        public MvcController<File, MMVH> onClone() {
            return new MM(host);
        }

        @Override
        protected void onCreate(int position, File item) {
            menuItems.add(MENU_OPEN);
            menuItems.add(MENU_SHARE);
            if (!host.readonly) {
                menuItems.add(MENU_ZIP);
                menuItems.add(MENU_DELETE);
            }

            lastModifiedDatetime = DATE_FORMAT.format(new Date(item.lastModified()));
            if (item.isDirectory()) {
                final String[] subFiles = item.list(host);
                fileDesc = host.getString(R.string.libuihelper_files, (subFiles != null ? subFiles.length : 0));
            } else {
                fileDesc = calcFileSize();
            }
        }

        @Override
        public MvcViewHolder onCreateView(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.libuihelper_item_file_info, parent, false);
            return new MMVH(view);
        }

        @Override
        protected void onAttach(int position, File file, MMVH holder) {
            holder.nameTv.setText(file.getName());
            holder.sizeTv.setText(fileDesc);
            holder.dateTv.setText(lastModifiedDatetime);

            if (host.canSelectMulti) {
                if ((file.isDirectory() && host.canSelectDir) || (file.isFile() && host.canSelectFile)) {
                    holder.selectCb.setVisibility(View.VISIBLE);
                    holder.selectCb.setChecked(host.selectedFiles.containsKey(file.getAbsolutePath()));
                } else {
                    holder.selectCb.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.selectCb.setVisibility(View.GONE);
            }

            holder.selectCb.setOnClickListener(this);
            holder.itemView.setOnClickListener(this);
            holder.menuBtn.setOnClickListener(this);
        }

        @Override
        protected void onDetach(int position, File item, MMVH holder) {
            holder.selectCb.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);
            holder.menuBtn.setOnClickListener(null);
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public void onClick(View v) {
            if (holder == null) {
                return;
            }

            if (v == holder.itemView || v == holder.selectCb) {
                host.onFileClicked(item);
                if (host.canSelectMulti) {
                    holder.selectCb.setChecked(host.selectedFiles.containsKey(item.getAbsolutePath()));
                }
            }

            if (v == holder.menuBtn) {
                if (!menuItems.isEmpty()) {
                    final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    for (String menuItem : menuItems) {
                        popupMenu.getMenu().add(menuItem);
                    }
                    popupMenu.setOnMenuItemClickListener(this);
                    popupMenu.show();
                }
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            String menuTitle = menuItem.getTitle().toString();
            if (MENU_SHARE.equals(menuTitle)) {
                shareInclusively();
            } else if (MENU_OPEN.equals(menuTitle)) {
                host.openFile(item);
            } else if (MENU_DELETE.equals(menuTitle)) {
                try {
                    if (FileUtil.delete(item)) {
                        host.refreshList();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (MENU_ZIP.equals(menuTitle)) {
                try {
                    FileUtil.zip(item, new File(item.getParentFile(), item.getName() + ".zip"));
                    host.refreshList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        private void shareInclusively() {
            // 如果是文件夹，就对每个文件单独处理
            final ArrayList<File> files = new ArrayList<>();
            final ArrayList<Uri> fileUris = new ArrayList<>();
            if (item.isDirectory()) {
                // 只查找一级
                final File[] subFiles = item.listFiles(host);
                if (subFiles != null) {
                    Collections.addAll(files, subFiles);
                }
            } else {
                files.add(item);
            }

            // 获取URI
            for (File file : files) {
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(host.mCtx, host.mCtx.getPackageName() + ".FileProvider", file);
                } else {
                    uri = Uri.fromFile(file);
                }
                fileUris.add(uri);
            }

            // 发送
            if (!fileUris.isEmpty()) {
                Intent intent = new Intent();
                if (fileUris.size() == 1) {
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType(host.getMIME(files.get(0)));
                    intent.putExtra(Intent.EXTRA_STREAM, fileUris.get(0));
                } else {
                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    intent.setType("*/*");
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
                }
                host.startActivity(Intent.createChooser(intent, host.getString(R.string.libuihelper_please_select)));
            }
        }

        String calcFileSize() {
            final long size = item.length();
            if (size < 10 * 1024) {
                return size + " B";
            } else if (size < 1024 * (1024)) {
                return size / (1024) + " KB";
            } else {
                return size / (1024 * 1024) + " MB";
            }
        }
    }

    static class MMVH extends MvcViewHolder {

        final TextView nameTv;
        final TextView sizeTv;
        final TextView dateTv;
        final CheckBox selectCb;
        final Button menuBtn;

        MMVH(@NonNull View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.libuihelper_item_file_info_name_tv);
            sizeTv = itemView.findViewById(R.id.libuihelper_item_file_info_size_tv);
            dateTv = itemView.findViewById(R.id.libuihelper_item_file_info_date_tv);

            selectCb = itemView.findViewById(R.id.libuihelper_item_file_info_select_cb);
            menuBtn = itemView.findViewById(R.id.libuihelper_item_file_info_menu_btn);
        }
    }
}
