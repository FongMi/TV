package com.xunlei.downloadlib.android;

import android.os.Environment;

import java.io.File;

public class XLLog {
    private static LogConfig mLogConfig = null;
    private static XLLogInternal mXLLogInternal;


    public static boolean init(String str) {

        return init();
    }

    public static synchronized boolean init() {
        synchronized (XLLog.class) {
            if (mLogConfig == null) {
                File file = new File(Environment.getExternalStorageDirectory().getPath(), "xunlei_ds_log.ini");
                if (file.exists()) {
                    LogConfig logConfig = new LogConfig(file.getPath());
                    mLogConfig = logConfig;
                    if (logConfig.canLogToFile()) {
                        mXLLogInternal = new XLLogInternal(mLogConfig);
                    }
                }
            }
        }
        return true;
    }

    public static void i(String str, String str2) {
        log(LogLevel.LOG_LEVEL_INFO, str, str2);

    }

    public static void d(String str, String str2) {
        log(LogLevel.LOG_LEVEL_DEBUG, str, str2);
    }

    public static void w(String str, String str2) {
        log(LogLevel.LOG_LEVEL_WARN, str, str2);
    }

    public static void e(String str, String str2) {
        log(LogLevel.LOG_LEVEL_ERROR, str, str2);
    }

    public static void w(String str, String str2, Throwable th) {
        log(LogLevel.LOG_LEVEL_WARN, str, str2 + ": " + th);
    }

    public static void v(String str, String str2) {
        d(str, str2);
    }

    public static void wtf(String str, String str2, Throwable th) {
        log(LogLevel.LOG_LEVEL_WARN, str, str2 + ": " + th);
    }

    public static boolean canbeLog(LogLevel logLevel) {
        return mXLLogInternal != null;
    }

    static void log(LogLevel logLevel, String str, String str2) {
//        Log.println(LogLevel.LOG_LEVEL_ERROR.getValue(), str+"-XL", str2);
//        Object obj = null;
//        if (logLevel == LogLevel.LOG_LEVEL_ERROR || (mLogConfig != null && mLogConfig.canLogToLogCat())) {
//            obj = 1;
//        }
//        if (obj != null || mXLLogInternal != null) {
//            String formatMessage = formatMessage(logLevel, str, str2);
//            if (obj != null) {
//                Log.println(logLevel.getValue(), str, formatMessage);
//            }
//            if (mXLLogInternal != null) {
//                mXLLogInternal.log(logLevel, str, formatMessage);
//            }
//        }
    }

    public static void printStackTrace(Throwable th) {
        if (mXLLogInternal != null) {
            mXLLogInternal.printStackTrace(th);
        }
    }

    private static String formatMessage(LogLevel logLevel, String str, String str2) {
        StringBuilder stringBuilder = new StringBuilder(str2 + "\t");
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length > 5) {
                StackTraceElement stackTraceElement = stackTrace[5];
                stringBuilder.append("[" + stackTraceElement.getFileName() + ":");
                stringBuilder.append(stackTraceElement.getLineNumber() + "]");
            } else {
                stringBuilder.append("[stack=" + stackTrace.length + "]");
            }
        } catch (Exception e) {
        }
        return stringBuilder.toString();
    }
}
