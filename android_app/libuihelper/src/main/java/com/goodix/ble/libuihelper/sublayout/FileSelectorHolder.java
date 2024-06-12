package com.goodix.ble.libuihelper.sublayout;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libuihelper.R;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("unused")
public class FileSelectorHolder implements ISubLayoutHolder<FileSelectorHolder>, View.OnClickListener {
    public static final int EVT_SELECTED = 236;

    @Nullable
    public View root;
    @Nullable
    public TextView captionTv;
    @Nullable
    public TextView fileNameTv;
    @Nullable
    public TextView fileInfoTv;
    @Nullable
    public Button selectBtn;
    @Nullable
    public Button actionBtn;

    @Nullable
    private FragmentActivity hostActivity;
    @Nullable
    private Fragment hostFragment;

    public Uri defaultUri;
    public String[] mimeTypes;
    public String defaultName;
    public Uri selectedFileUri;
    public String selectedFileName;
    public String selectedMimeType;
    public long selectedFileSize;

    private boolean writeFile = false;
    private Event<Boolean> eventSelected;

    public FileSelectorHolder setHost(FragmentActivity hostActivity) {
        this.hostActivity = hostActivity;
        return this;
    }

    public FileSelectorHolder setHost(Fragment hostFragment) {
        this.hostFragment = hostFragment;
        return this;
    }


    /**
     * true - if any file is selected; false - if cancelled or encountered error.
     */
    public Event<Boolean> evtSelected() {
        if (eventSelected == null) {
            synchronized (this) {
                if (eventSelected == null) {
                    eventSelected = new Event<>(this, EVT_SELECTED);
                }
            }
        }
        return eventSelected;
    }

    public FileSelectorHolder setMimeType(String mimeType) {
        if (this.mimeTypes == null || this.mimeTypes.length != 1) {
            this.mimeTypes = new String[1];
        }
        this.mimeTypes[0] = mimeType;
        return this;
    }

    public FileSelectorHolder setMimeType(String... mimeTypes) {
        this.mimeTypes = mimeTypes;
        return this;
    }

    public FileSelectorHolder setWriteFile(boolean writeFile) {
        this.writeFile = writeFile;
        return this;
    }

    public FileSelectorHolder setDefaultUri(Uri defaultUri) {
        this.defaultUri = defaultUri;
        return this;
    }

    /**
     * Support placeholder:
     * {timestamp} : replaced by the value of {@link System#currentTimeMillis()};
     * {datetime} : replaced by the value of Date in the format of "yyyyMMdd-HHmmss-SSS";
     */
    public FileSelectorHolder setDefaultName(String defaultName) {
        this.defaultName = defaultName;
        return setWriteFile(true);
    }

    public FileSelectorHolder inflate(@LayoutRes int resource, ViewGroup container) {
        root = LayoutInflater.from(container.getContext()).inflate(resource, container, false);
        container.addView(root);
        attachView(root);
        return this;
    }

    public void clearSelection() {
        selectedFileUri = null;
        selectedFileName = null;
        if (fileNameTv != null) {
            fileNameTv.setText(R.string.libuihelper_no_selected_file);
        }

        Event<Boolean> event = this.eventSelected;
        if (event != null) {
            event.postEvent(false);
        }
    }

    @Nullable
    public String getSelectedFileName() {
        return selectedFileName;
    }

    @Nullable
    public Uri getSelectedFileUri() {
        return selectedFileUri;
    }

    @Nullable
    public InputStream openInputStream() {
        InputStream r = null;
        if (selectedFileUri != null && hostActivity != null) {
            ContentResolver resolver = hostActivity.getContentResolver();
            try {
                r = resolver.openInputStream(selectedFileUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return r;
    }

    @Nullable
    public OutputStream openOutputStream(boolean append) {
        OutputStream r = null;
        if (selectedFileUri != null && hostActivity != null && writeFile) {
            ContentResolver resolver = hostActivity.getContentResolver();
            try {
                r = resolver.openOutputStream(selectedFileUri, append ? "wa" : "rwt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return r;
    }

    public boolean closeStream(@Nullable Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean show() {
        if (hostActivity == null && hostFragment != null) {
            hostActivity = hostFragment.getActivity();
        }
        if (hostActivity == null) {
            if (root != null) {
                Toast.makeText(root.getContext(), "Please provide host Activity or Fragment for FileSelectorHolder.", Toast.LENGTH_LONG).show();
            }
            return false;
        }

        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");
        if (mimeTypes != null && mimeTypes.length > 0) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }

        if (defaultUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, defaultUri);
        }

        if (writeFile) {
            if (defaultName != null) {
                String name = defaultName;
                if (name.contains("{timestamp}")) {
                    name = name.replace("{timestamp}", String.valueOf(System.currentTimeMillis()));
                }
                if (name.contains("{datetime}")) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US);
                    name = name.replace("{timestamp}", sdf.format(new Date()));
                }
                intent.putExtra(Intent.EXTRA_TITLE, name);
            }

            intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }

        HolderFragment fragment = new HolderFragment();
        fragment.holder = this;
        fragment.intent = intent;
        fragment.requestCode = (int) (System.currentTimeMillis() & 0xFFFF) | 0x1511;

        hostActivity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment, getClass().getSimpleName())
                .commit();

        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    boolean handleActivityResult(@Nullable Intent data) {
        boolean ret = false;

        if (hostActivity == null) {
            return false;
        }

        if (data != null) {
            final Uri uri = data.getData();
            if (uri != null) {
                String name = null;
                try {
                    ContentResolver resolver = hostActivity.getContentResolver();
                    Cursor query = resolver.query(uri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null);
                    if (query != null) {
                        if (query.moveToNext()) {
                            name = query.getString(0);

                            long size = query.getLong(1);
                            if (fileInfoTv != null) {
                                HexStringBuilder sb = new HexStringBuilder(64);
                                sb.append(size).append("bytes");
                                fileInfoTv.setText(sb);
                            }
                            selectedFileSize = size;
                            selectedMimeType = query.getString(2);
                        }
                        query.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (fileInfoTv != null) {
                        fileInfoTv.setText(fileInfoTv.getContext().getString(R.string.libuihelper_err_msg, e.getMessage()));
                    }
                }

                selectedFileName = name;
                if (fileNameTv != null) {
                    if (name != null) {
                        fileNameTv.setText(name);
                    } else {
                        fileNameTv.setText(R.string.libuihelper_err_can_not_get_file_name);
                    }
                }

                selectedFileUri = uri;
                ret = true;
            } else {
                if (fileNameTv != null) {
                    fileNameTv.setText(R.string.libuihelper_no_selected_file);
                }
            }
        }

        Event<Boolean> event = this.eventSelected;
        if (event != null) {
            event.postEvent(ret);
        }

        return ret;
    }

    public FileSelectorHolder inflate(LayoutInflater inflater, @Nullable ViewGroup container, @LayoutRes int resource) {
        View view = inflater.inflate(resource, container, false);
        attachView(view);
        if (container != null) {
            container.addView(view);
        }
        return this;
    }

    @Override
    public FileSelectorHolder attachView(View root) {
        this.root = root;

        if (root instanceof TextView) {
            fileNameTv = (TextView) root;
            if (fileNameTv.isClickable()) {
                fileNameTv.setOnClickListener(this);
            }
        } else {
            captionTv = root.findViewById(R.id.sublayout_caption_tv);
            fileNameTv = root.findViewById(R.id.sublayout_name_tv);
            fileInfoTv = root.findViewById(R.id.sublayout_value_tv);
            selectBtn = root.findViewById(R.id.sublayout_select_btn);

            actionBtn = root.findViewById(R.id.sublayout_action_btn);
            if (actionBtn != null) {
                actionBtn.setVisibility(View.GONE);
            }

            // try to find a similar view
            if (fileNameTv == null && root instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) root;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    if (fileNameTv == null && child instanceof TextView) {
                        fileNameTv = (TextView) child;
                    } else if (selectBtn == null && child instanceof Button) {
                        selectBtn = (Button) child;
                    }
                }
            }

            setOnClickListener(this);
        }

        return this;
    }

    @Override
    public FileSelectorHolder setEnabled(boolean enabled) {
        if (root != null) {
            root.setEnabled(enabled);
        }
        if (selectBtn != null) {
            selectBtn.setEnabled(enabled);
        }
        return this;
    }

    @Override
    public FileSelectorHolder setVisibility(int visibility) {
        if (root != null) {
            root.setVisibility(visibility);
        }
        return this;
    }

    @Override
    public FileSelectorHolder setOnClickListener(View.OnClickListener l) {
        if (selectBtn != null) {
            selectBtn.setOnClickListener(l);
        }
        return this;
    }

    @Override
    public FileSelectorHolder setCaption(CharSequence text) {
        if (captionTv != null) {
            captionTv.setText(text);
        }
        return this;
    }

    @Override
    public FileSelectorHolder setCaption(int resId) {
        if (captionTv != null) {
            captionTv.setText(resId);
        }
        return this;
    }

    @Override
    public FileSelectorHolder noButton() {
        if (selectBtn != null) {
            selectBtn.setVisibility(View.GONE);
        }
        return this;
    }

    @Override
    public void onClick(View v) {
        if (selectBtn == null || selectBtn == v) {
            show();
        }
    }

    public static class HolderFragment extends Fragment {
        FileSelectorHolder holder;
        private int requestCode;
        private Intent intent;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

            if (holder == null || intent == null) {
                FragmentActivity host = getActivity();
                if (host != null) {
                    host.getSupportFragmentManager()
                            .beginTransaction()
                            .remove(this)
                            .commit();
                }
            } else {
                startActivityForResult(intent, requestCode);
            }

            return null;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            holder = null;
            intent = null;
            requestCode = 0;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == this.requestCode) {
                holder.handleActivityResult(data);
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
