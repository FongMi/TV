package com.xunlei.downloadlib.parameter;

import android.util.Base64;

import com.xunlei.downloadlib.android.XLUtil;

import java.nio.charset.StandardCharsets;

public class InitParam {

    public String mAppKey;
    public String mAppVersion;
    public int mPermissionLevel;
    public int mQueryConfOnInit;
    public String mStatCfgSavePath;
    public String mStatSavePath;

    public String getSoKey() {
        String[] split = this.mAppKey.split("==");
        String replace = split[0].replace('^', '=');
        String str = new String(Base64.decode(replace.substring(2, replace.length() - 2), 0), StandardCharsets.UTF_8);
        return XLUtil.generateAppKey("com.android.providers.downloads", Short.parseShort(str.split(";")[0]), (byte) 1);
    }
}
