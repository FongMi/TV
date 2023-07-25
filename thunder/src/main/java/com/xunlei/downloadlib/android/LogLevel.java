package com.xunlei.downloadlib.android;


/* compiled from: XLLog */
enum LogLevel {
    LOG_LEVEL_VERBOSE(2),
    LOG_LEVEL_DEBUG(3),
    LOG_LEVEL_INFO(4),
    LOG_LEVEL_WARN(5),
    LOG_LEVEL_ERROR(6);
    
    private final int logLevel;

    private LogLevel(int i) {
        this.logLevel = i;
    }

    static LogLevel parseLevel(String str) {
        if (str.equals("e") || str.equals("error")) {
            return LOG_LEVEL_ERROR;
        }
        if (str.equals("w") || str.equals("warn")) {
            return LOG_LEVEL_WARN;
        }
        if (str.equals("i") || str.equals("info")) {
            return LOG_LEVEL_INFO;
        }
        if (str.equals("d") || str.equals("debug")) {
            return LOG_LEVEL_DEBUG;
        }
        return LOG_LEVEL_VERBOSE;
    }

    public final int getValue() {
        return this.logLevel;
    }

    public final String toString() {
        return toString(true);
    }

    public final String toString(boolean z) {
        switch (this) {
            case LOG_LEVEL_DEBUG:
                return z ? "D" : "DEBUG";
            case LOG_LEVEL_INFO:
                return z ? "I" : "INFO";
            case LOG_LEVEL_WARN:
                return z ? "W" : "WARN";
            case LOG_LEVEL_ERROR:
                return z ? "E" : "ERROR";
            default:
                if (z) {
                    return "V";
                }
                return "VERBOSE";
        }
    }
}
