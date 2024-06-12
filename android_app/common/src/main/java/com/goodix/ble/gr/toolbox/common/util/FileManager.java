package com.goodix.ble.gr.toolbox.common.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileManager {
    private static final String TAG = "FileManager";
    private Context mContext;

    public FileManager(Context context) {
        mContext = context;
    }

     public String getRealPathFromURI(Uri contentUri) {
         String res = null;
         String[] proj = { MediaStore.Images.Media.DATA };
         Cursor cursor = mContext.getContentResolver().query(contentUri, proj, null, null, null);
         if(null!=cursor&&cursor.moveToFirst()){;
             int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
             res = cursor.getString(column_index);
             cursor.close();
         }
         return res;
     }

    @SuppressLint("NewApi")
     public String getPath(final Context context, final Uri uri) {
         // DocumentProvider
         if (DocumentsContract.isDocumentUri(context, uri)) {
             // ExternalStorageProvider
             if (isExternalStorageDocument(uri)) {
                 final String docId = DocumentsContract.getDocumentId(uri);
                 final String[] split = docId.split(":");
                 final String type = split[0];

                 if ("primary".equalsIgnoreCase(type)) {
                     return Environment.getExternalStorageDirectory() + "/" + split[1];
                 }
             }
             // DownloadsProvider
             else if (isDownloadsDocument(uri)) {
                 final String id = DocumentsContract.getDocumentId(uri);
                 final Uri contentUri = ContentUris.withAppendedId(
                         Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                 return getDataColumn(context, contentUri, null, null);
             }
             // MediaProvider
             else if (isMediaDocument(uri)) {
                 final String docId = DocumentsContract.getDocumentId(uri);
                 final String[] split = docId.split(":");
                 final String type = split[0];

                 Uri contentUri = null;
                 if ("image".equals(type)) {
                     contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                 } else if ("video".equals(type)) {
                     contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                 } else if ("audio".equals(type)) {
                     contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                 }

                 final String selection = "_id=?";
                 final String[] selectionArgs = new String[]{split[1]};

                 return getDataColumn(context, contentUri, selection, selectionArgs);
             }
         }
         // MediaStore (and general)
         else if ("content".equalsIgnoreCase(uri.getScheme())) {
             return getDataColumn(context, uri, null, null);
         }
         // File
         else if ("file".equalsIgnoreCase(uri.getScheme())) {
             return uri.getPath();
         }
         return null;
     }

    /**
      * Get the value of the data column for this Uri. This is useful for
      * MediaStore Uris, and other file-based ContentProviders.
      *
      * @param context       The context.
      * @param uri           The Uri to query.
      * @param selection     (Optional) Filter used in the query.
      * @param selectionArgs (Optional) Selection arguments used in the query.
      * @return The value of the _data column, which is typically a file path.
      */
     public String getDataColumn(Context context, Uri uri, String selection,
                                 String[] selectionArgs) {

         Cursor cursor = null;
         final String column = "_data";
         final String[] projection = {column};

         try {
             cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                     null);
             if (cursor != null && cursor.moveToFirst()) {
                 final int column_index = cursor.getColumnIndexOrThrow(column);
                 return cursor.getString(column_index);
             }
         } finally {
             if (cursor != null)
                 cursor.close();
         }
         return null;
     }

    /**
      * @param uri The Uri to check.
      * @return Whether the Uri authority is ExternalStorageProvider.
      */
     public boolean isExternalStorageDocument(Uri uri) {
         return "com.android.externalstorage.documents".equals(uri.getAuthority());
     }

     /**
      * @param uri The Uri to check.
      * @return Whether the Uri authority is DownloadsProvider.
      */
     public boolean isDownloadsDocument(Uri uri) {
         return "com.android.providers.downloads.documents".equals(uri.getAuthority());
     }

     /**
      * @param uri The Uri to check.
      * @return Whether the Uri authority is MediaProvider.
      */
     public boolean isMediaDocument(Uri uri) {
         return "com.android.providers.media.documents".equals(uri.getAuthority());
     }

    public void writeFileData(String fileName, String content) {
        try {
            FileOutputStream fos = mContext.openFileOutput(fileName, mContext.MODE_PRIVATE);
            byte[] bytes = content.getBytes();
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] readFromSD(String filename) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                FileInputStream input = new FileInputStream(filename);
                int fileSize = input.available();
                Log.i(TAG, "file size = " + fileSize);
                byte[] temp = new byte[fileSize];

                while (input.read(temp) > 0) {
                }
                input.close();

                return temp;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public byte[] readPrivateFileData(String fileName) {
        byte[] buffer = null;

        try {
            FileInputStream fis = mContext.openFileInput(fileName);
            if (fis == null) {
                Log.i(TAG, "file not exist");
                return null;
            }
            int length = fis.available();
            buffer = new byte[length];
            fis.read(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buffer;
    }
}
