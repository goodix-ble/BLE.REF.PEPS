package com.goodix.ble.libcomx.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileAsyncWriter extends StreamAsyncWriter {
    private File outFile;
    private boolean appendable = true;
    private boolean firstRun = true;

    private SimpleDateFormat dateFormatter;
    private String fileNamePattern;
    private FileOutputStream outputStream;

    public FileAsyncWriter(File outFile) {
        this.outFile = outFile;
    }

    /**
     * Placeholder: ${date}  it will be replace with formatted date string.
     */
    public FileAsyncWriter(String filePathNameWithDate) {
        this.fileNamePattern = filePathNameWithDate;
        setDateFormatter("yyyyMMdd-HHmmss-SSS");
    }

    public File getOutFile() {
        return outFile;
    }

    public void setDateFormatter(String dateFormat) {
        if (this.dateFormatter != null) {
            this.dateFormatter.applyPattern(dateFormat);
        } else {
            this.dateFormatter = new SimpleDateFormat(dateFormat, Locale.US);
        }
    }


    @Override
    protected OutputStream onPrepareOutputStream() throws Exception {
        if (this.outputStream != null) {
            return this.outputStream;
        }

        if (this.outFile == null) {
            if (this.fileNamePattern != null && this.fileNamePattern.trim().length() > 0) {
                this.outFile = new File(this.fileNamePattern.replace("${date}", dateFormatter.format(new Date())));
            } else {
                return null;
            }
        }

        if (!outFile.exists()) {
            File parentFile = outFile.getParentFile();
            if (!parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    throw new Exception("Failed to make parent directory: " + parentFile);
                }
            }
            if (outFile.createNewFile()) {
                this.outputStream = new FileOutputStream(outFile);
            } else {
                throw new Exception("Failed to create file: " + outFile.getAbsolutePath());
            }
        } else {
            // 首次创建时，根据需求决定是否在文件末尾追加数据
            // 恢复数据流时，一定是在文件末尾追加数据
            this.outputStream = new FileOutputStream(outFile, !firstRun || appendable);
        }
        firstRun = false;
        return this.outputStream;
    }


    @Override
    protected void onCloseOutputStream(OutputStream outputStream) {
        if (this.outputStream == outputStream) {
            this.outputStream = null;
        }
    }
}
