package com.xunlei.downloadlib.android;

import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/* compiled from: XLLog */
final class LogConfig {
    private final int mDestinationType;
    final String mFileName;
    final LogLevel mLevel;
    final String mLogDir;
    final long mLogSize;

    public LogConfig(String str) {
        Map parseConfigFile = parseConfigFile(str);
        long parseLong = Long.parseLong(getOrDefault(parseConfigFile, "LOG_FILE_SIZE", "0"));
        if (parseLong == 0) {
            parseLong = 20971520;
        }
        this.mLogSize = parseLong;
        this.mLevel = LogLevel.parseLevel(getOrDefault(parseConfigFile, "LOG_LEVEL", "").toLowerCase());
        this.mFileName = getOrDefault(parseConfigFile, "LOG_FILE", "");
        this.mLogDir = getOrDefault(parseConfigFile, "LOG_DIR", "");
        if (TextUtils.isEmpty(this.mFileName) || TextUtils.isEmpty(this.mLogDir)) {
            this.mDestinationType = 0;
            return;
        }
        int parseInt = Integer.parseInt(getOrDefault(parseConfigFile, "LOG_DESTINATION_TYPE", "0"));
        if (parseInt > 0) {
            this.mDestinationType = parseInt;
        } else {
            this.mDestinationType = 3;
        }
    }

    public final boolean canLogToFile() {
        return (this.mDestinationType & 1) > 0;
    }

    public final boolean canLogToLogCat() {
        return (this.mDestinationType & 2) > 0;
    }

    private String getOrDefault(Map<String, String> map, String str, String str2) {
        String str3 = (String) map.get(str);
        return str3 == null ? str2 : str3;
    }

    private Map<String, String> parseConfigFile(String str) {
        File file = new File(str);
        Map<String, String> hashMap = new HashMap();
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                String str2 = "";
                StringBuilder stringBuilder = new StringBuilder();
                StringBuilder stringBuilder2 = new StringBuilder();
                while (true) {
                    int read = fileInputStream.read();
                    if (read != -1) {
                        if (stringBuilder.length() == 0 && read == 35) {
                            do {
                                read = fileInputStream.read();
                                if (read == -1 || read == 13) {
                                    break;
                                }
                            } while (read != 10);
                        } else if (!(read == 32 || read == 9)) {
                            if (read == 61) {
                                str2 = stringBuilder.toString();
                            } else if (read == 10 || read == 13) {
                                if (stringBuilder.length() != 0) {
                                    hashMap.put(str2, stringBuilder2.toString());
                                    str2 = "";
                                    stringBuilder = new StringBuilder();
                                    stringBuilder2 = new StringBuilder();
                                }
                            } else if (str2.length() == 0) {
                                stringBuilder.append((char) read);
                            } else {
                                stringBuilder2.append((char) read);
                            }
                        }
                    } else {
                        break;
                    }
                }
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hashMap;
    }
}
