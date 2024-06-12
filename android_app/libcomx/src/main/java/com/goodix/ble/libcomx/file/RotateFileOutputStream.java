package com.goodix.ble.libcomx.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RotateFileOutputStream extends OutputStream {
    public long fileSize; // 1MB
    public boolean splitBytes = true;

    private final boolean append;
    private final File[] rotateFiles;
    private final File outputFile;
    private FileOutputStream fos;

    private long writtenSize;

    /**
     * if this option is true, when {@link #write(byte[])} or {@link #write(byte[], int, int)} is called and file is about to overflow, the bytes will be split into to files.
     * true - 文件写满时，多余的字节保存到下一个文件中，写满的文件大小精确的等于与设定的大小。
     * false - 文件写满时，多余的字节保存到当前文件，会导致文件超过设置的大小。
     */
    public void setSplitBytes(boolean splitBytes) {
        this.splitBytes = splitBytes;
    }

    public RotateFileOutputStream(File file, int rotateCount, int rotateSize) throws IOException {
        this(file, rotateCount, rotateSize, true);
    }

    public RotateFileOutputStream(File file, int rotateCount, int rotateSize, boolean append) throws IOException {
        this.append = append;
        this.fileSize = rotateSize;
        this.outputFile = file;

        if (rotateCount <= 0) {
            rotateCount = 4;
        }
        this.rotateFiles = new File[rotateCount + 1];

        String orgFile = file.getPath();
        String format;
        if (rotateCount < 10) {
            format = "%s.%d";
        } else if (rotateCount < 100) {
            format = "%s.%02d";
        } else {
            format = "%s.%03d";
        }

        for (int i = 0; i < rotateCount; i++) {
            rotateFiles[i] = new File(String.format(format, orgFile, (rotateCount - i)));
        }
        rotateFiles[rotateCount] = this.outputFile;

        fos = new FileOutputStream(this.outputFile, append);

        if (append && file.exists()) {
            writtenSize = file.length();
            if (writtenSize >= fileSize) {
                rotate();
            }
        } else {
            fos = new FileOutputStream(this.outputFile, append);
            writtenSize = 0;
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (writtenSize + 1 > fileSize) {
            // 要么写入下一个文件
            rotate();
            fos.write(b);
            writtenSize += 1;
        } else {
            // 要么刚好写满当前文件
            fos.write(b);
            writtenSize += 1;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write0(b, 0, b.length);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        write0(b, off, len);
    }

    public void write0(byte[] b, int off, int len) throws IOException {

        // 如果没有空间写入数据了，就先旋转一下，产生一个空文件作为当前文件
        if (writtenSize >= fileSize) {
            rotate();
        }

        if (splitBytes && writtenSize + len > fileSize) {
            int firstLen = (int) (fileSize - writtenSize);
            int lastLen = len - firstLen;

            // 先写一部分到当前文件
            fos.write(b, off, firstLen);
            writtenSize += firstLen;

            // 如果还有剩余字节要写
            if (lastLen > 0) {
                // 再写入，这样就不会产生空文件
                write0(b, off + firstLen, lastLen);
            }
        } else {
            // 全部写入当前文件
            fos.write(b, off, len);
            writtenSize += len;
        }
    }

    private void rotate() throws IOException {

        if (fos != null) {
            fos.close();
        }

        if (!rotateFiles[0].exists() || rotateFiles[0].delete()) {
            for (int i = 1; i < rotateFiles.length; i++) {
                if (rotateFiles[i].exists() && !rotateFiles[i].renameTo(rotateFiles[i - 1])) {
                    if (!rotateFiles[i].delete()) {
                        System.out.println("RotateFile: Failed to delete file: " + rotateFiles[i].getCanonicalPath());
                    }
                }
            }
        } else {
            System.out.println("RotateFile: Failed to delete file: " + rotateFiles[0].getCanonicalPath());
            return;
        }

        fos = new FileOutputStream(this.outputFile, append);
        writtenSize = 0;
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }

    @Override
    public void flush() throws IOException {
        fos.flush();
    }
}
