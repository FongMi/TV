package com.xunlei.downloadlib.android;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

/* compiled from: XLLog */
final class XLLogInternal {
    private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private File mFile;
    private final Handler mHandler;
    private final LogConfig mLogConfig;
    private int mNext;
    private int mRun;

    XLLogInternal(LogConfig logConfig) {
        this.mLogConfig = logConfig;
        HandlerThread handlerThread = new HandlerThread("DownloadLib-XLLog");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
    }

    final void log(final LogLevel logLevel, final String str, String str2) {
        final String appendHeader = appendHeader(logLevel, str, str2);
        if (logLevel.getValue() >= this.mLogConfig.mLevel.getValue() && this.mLogConfig.canLogToFile()) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    XLLogInternal.this.logfile(logLevel, str, appendHeader);
                }
            });
        }
    }

    public final void printStackTrace(final Throwable th) {
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    Writer fileWriter = new FileWriter(XLLogInternal.this.getLogFile(), true);
                    Writer bufferedWriter = new BufferedWriter(fileWriter);
                    th.printStackTrace(new PrintWriter(bufferedWriter));
                    bufferedWriter.write("\n");
                    bufferedWriter.close();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static String appendHeader(LogLevel logLevel, String str, String str2) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DATEFORMAT.format(new Date()) + ": " + logLevel.toString());
        stringBuilder.append("/" + str + "(" + Thread.currentThread().getId() + "):\t");
        stringBuilder.append(str2);
        stringBuilder.append("\r\n");
        return stringBuilder.toString();
    }

    private void logfile(LogLevel logLevel, String str, String str2) {
        try {
            Writer fileWriter = new FileWriter(getLogFile(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(str2);
            bufferedWriter.newLine();
            bufferedWriter.close();
            fileWriter.close();
        } catch (Exception e) {
        }
    }

    private File getLogFile() {
        if ("mounted".equalsIgnoreCase(Environment.getExternalStorageState())) {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + this.mLogConfig.mLogDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            while (this.mFile == null) {
                this.mFile = new File(file.getPath() + File.separator + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".R" + this.mRun + ".0." + this.mLogConfig.mFileName);
                if (!this.mFile.exists()) {
                    break;
                }
                this.mRun++;
                this.mFile = null;
            }
            if (getLogFileSize() >= this.mLogConfig.mLogSize) {
                this.mNext++;
                this.mFile = new File(file.getPath() + File.separator + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".R" + this.mRun + "." + this.mNext + "." + this.mLogConfig.mFileName);
                this.mFile.delete();
            }
        }
        return this.mFile;
    }

    private long getLogFileSize() {
        long j = -1;
        if (this.mFile == null) {
            return -1;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(this.mFile);
            j = (long) fileInputStream.available();
            fileInputStream.close();
            return j;
        } catch (Exception e) {
            return j;
        }
    }
}
