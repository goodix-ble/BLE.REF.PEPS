package com.goodix.ble.libcomx.file;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件可以一直持有，或者写入时持有
 * 默认文件命名为：时间+“_”+拆分序号
 * 支持自定义文件名，使用回调方式
 * 支持文件拆分
 */
public class DelayedFileWriter extends DelayedStreamWriter {
    private File outFileOrDir;

    private boolean appendable = true;
    private boolean seperateFile = false;
    private long maxFileSize = 1024 * 1024; // 1MB
    private long fileFirstCreateTime; // 第一个文件开始创建文件的时间
    private int fileCnt; // 已创建文件数量
    private SimpleDateFormat dateFormatter;
    private String fileNamePattern;

    public DelayedFileWriter(File outFileOrDir) {
        this.outFileOrDir = outFileOrDir;
    }

    /**
     * ${date}
     * ${idx}
     */
    public DelayedFileWriter(String filePathNamePattern) {
        this.fileNamePattern = filePathNamePattern;
        setDateFormatter("yyyyMMdd-HHmmss-SSS");
    }

    public File getOutFileOrDir() {
        return outFileOrDir;
    }

    public void setDateFormatter(String dateFormat) {
        if (this.dateFormatter != null) {
            this.dateFormatter.applyPattern(dateFormat);
        } else {
            this.dateFormatter = new SimpleDateFormat(dateFormat, Locale.US);
        }
    }


    @Override
    protected void onStartThread() throws Exception {
        if (this.fileNamePattern != null && this.fileNamePattern.trim().length() > 0) {
            if (fileFirstCreateTime == 0) {
                fileFirstCreateTime = System.currentTimeMillis();
            }
            this.outFileOrDir = new File(this.fileNamePattern.replace("@{date}", dateFormatter.format(new Date())).replace("@{idx}", String.valueOf(fileCnt)));
            if (this.outputStream != null) {
                fileCnt++;
            }
        }

        if (!outFileOrDir.exists()) {
            File parentFile = outFileOrDir.getParentFile();
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    throw new Exception("Failed to make parent directory: " + parentFile);
                }
            }
            if (outFileOrDir.createNewFile()) {
                this.outputStream = new FileOutputStream(outFileOrDir);
            } else {
                throw new Exception("Failed to create file: " + outFileOrDir.getAbsolutePath());
            }
        } else {
            this.outputStream = new FileOutputStream(outFileOrDir, appendable);
        }
    }
}
