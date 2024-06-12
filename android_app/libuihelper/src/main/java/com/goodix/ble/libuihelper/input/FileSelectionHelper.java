package com.goodix.ble.libuihelper.input;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.goodix.ble.libuihelper.R;

import java.io.File;
import java.io.InputStream;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FileSelectionHelper implements View.OnClickListener {

    @Nullable
    private TextView fileNameTv;
    @Nullable
    private View selectBtn;
    @NonNull
    private final Activity host;
    @Nullable
    private Fragment fromFragment;
    private int requestCode;
    public File defaultPath;
    public String mimeType;
    public String title;
    public Uri selectedFileUri;
    public String selectedFileName;
    public InputStream selectedFileStream;

    public FileSelectionHelper(@NonNull Activity host) {
        this.host = host;
    }

    public FileSelectionHelper(@NonNull Activity host, @Nullable TextView fileNameTv, @Nullable View selectBtn) {
        this(host);
        this.fileNameTv = fileNameTv;
        this.selectBtn = selectBtn;
        if (selectBtn != null) {
            selectBtn.setOnClickListener(new DebouncedClickListener(this).setInterval(500));
        }
    }

    public FileSelectionHelper(@NonNull Fragment fromFragment, @Nullable TextView fileNameTv, @Nullable View selectBtn) {
        this.fromFragment = fromFragment;
        this.host = fromFragment.requireActivity();
        this.fileNameTv = fileNameTv;
        this.selectBtn = selectBtn;
        if (selectBtn != null) {
            selectBtn.setOnClickListener(new DebouncedClickListener(this).setInterval(500));
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
    public InputStream getSelectedFileStream() {
        return selectedFileStream;
    }

    @Nullable
    public InputStream openInputStream() {
        InputStream r = null;
        if (selectedFileUri != null) {
            ContentResolver resolver = host.getContentResolver();
            try {
                r = resolver.openInputStream(selectedFileUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (r != null) {
            selectedFileStream = r;
        }
        return selectedFileStream;
    }

    public boolean closeInputStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void show() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (mimeType != null) {
            intent.setType(mimeType);
        } else {
            intent.setType("*/*");
        }
        if (title != null) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (defaultPath != null) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(defaultPath));
            } else {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(host.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)));
            }
        }
        requestCode = (int) (System.currentTimeMillis() & 0xFFFF) | 0x1511;
        if (fromFragment != null) {
            fromFragment.startActivityForResult(intent, requestCode);
        } else {
            host.startActivityForResult(intent, requestCode, null);
        }
    }

    /**
     * parse result
     *
     * @return true if get file data.
     */
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        boolean ret = false;
        if (this.requestCode != 0 && requestCode == this.requestCode) {
            this.requestCode = 0;
            if (data != null) {
                final Uri uri = data.getData();
                try {
                    InputStream r = null;
                    String name = null;
                    if (uri != null) {
                        ContentResolver resolver = host.getContentResolver();
                        r = resolver.openInputStream(uri);
                        Cursor query = resolver.query(uri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
                        if (query != null) {
                            if (query.moveToNext()) {
                                name = query.getString(0);
                            }
                            query.close();
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

                    if (r != null) {
                        selectedFileUri = uri;
                        selectedFileStream = r;
                        ret = true;
                    } else {
                        selectedFileUri = null;
                        if (fileNameTv != null) {
                            fileNameTv.setText(R.string.libuihelper_err_can_not_read_file);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (fileNameTv != null) {
                        fileNameTv.setText(fileNameTv.getContext().getString(R.string.libuihelper_err_msg, e.getMessage()));
                    }
                }
            } else {
                if (selectedFileUri == null && fileNameTv != null) {
                    fileNameTv.setText(R.string.libuihelper_no_selected_file);
                }
            }
        }
        return ret;
    }

    @Override
    public void onClick(View v) {
        if (v == selectBtn) {
            show();
        }
    }

    public void setEnabled(boolean enable) {
        if (fileNameTv != null) {
            fileNameTv.setEnabled(enable);
        }
        if (selectBtn != null) {
            selectBtn.setEnabled(enable);
        }
    }
}
