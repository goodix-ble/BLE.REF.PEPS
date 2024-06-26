package com.goodix.ble.libcomx.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    public static boolean delete(File target) throws IOException {
        if (target == null) {
            return false;
        }

        // delete sub files
        if (target.isDirectory()) {
            final File[] subFiles = target.listFiles();
            if (subFiles != null) {
                for (File file : subFiles) {
                    if (!delete(file)) {
                        throw new IOException("Failed to delete: " + file.getAbsolutePath());
                    }
                }
            }
        }

        // Rename and delete
        final File renamedFile = new File(target.getParentFile(), "_" + target.getName());
        if (target.renameTo(renamedFile)) {
            return renamedFile.delete();
        } else {
            return target.delete();
        }
    }

    public static void zip(File srcFile, File outFile) throws IOException {
        ZipOutputStream out = null;

        try {
            out = new ZipOutputStream(new FileOutputStream(outFile));

            zip(out, "", srcFile);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void zip(ZipOutputStream out, String parentEntry, File srcFile) throws IOException {
        FileInputStream in = null;
        try {
            if (srcFile.isDirectory()) {
                File[] subFiles = srcFile.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        zip(out, parentEntry + srcFile.getName() + "/", subFile);
                    }
                }
            } else if (srcFile.isFile()) {
                // 压缩文件
                byte[] buffer = new byte[4096];
                int readSize;
                in = new FileInputStream(srcFile);
                //实例代表一个条目内的ZIP归档
                ZipEntry entry = new ZipEntry(parentEntry + srcFile.getName());
                out.putNextEntry(entry);
                while ((readSize = in.read(buffer)) != -1) {
                    out.write(buffer, 0, readSize);
                }
                out.closeEntry();
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
