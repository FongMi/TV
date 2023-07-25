package com.xunlei.downloadlib;

import android.util.Base64;
import android.util.Log;

import com.xunlei.downloadlib.android.XLUtil;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class XLAppKeyChecker {

    private static final String TAG = XLAppKeyChecker.class.getSimpleName();
    private final String mAppKey;
    private short mAppId = 0;

    public XLAppKeyChecker(String str) {
        this.mAppKey = str;
    }

    public boolean verify() {
        try {
            AppKeyEntity keyEntity = getKeyEntity();
            String packageName = "com.android.providers.downloads";
            if (!verifyAppKeyMD5(keyEntity, packageName)) {
                Log.i(TAG, "appkey MD5 invalid.");
                return false;
            } else if (!verifyAppKeyExpired(keyEntity)) {
                return true;
            } else {
                Log.i(TAG, "appkey expired.");
                return false;
            }
        } catch (KeyFormatException unused) {
            return false;
        }
    }

    private boolean verifyAppKeyMD5(AppKeyEntity appKeyEntity, String str) {
        String str2 = appKeyEntity.getmRawItems() + ";" + str;
        Log.i(TAG, "totalContent:" + str2);
        String replace = XLUtil.getMd5(str2).toLowerCase().replace('b', '^').replace('9', 'b');
        Log.i(TAG, "keyEntity getMD5 MD5:" + appKeyEntity.getMD5());
        return replace.compareTo(appKeyEntity.getMD5()) == 0;
    }

    private boolean verifyAppKeyExpired(AppKeyEntity appKeyEntity) {
        Date expired = appKeyEntity.getmItemsEntity().getExpired();
        if (expired == null) return false;
        return expired.before(Calendar.getInstance().getTime());
    }

    public String getSoAppKey() {
        return XLUtil.generateAppKey("com.android.providers.downloads", this.mAppId, (byte) 1);
    }

    private AppKeyEntity getKeyEntity() throws KeyFormatException {
        String[] split = this.mAppKey.split("==");
        if (split.length == 2) {
            AppKeyEntity appKeyEntity = new AppKeyEntity();
            appKeyEntity.setMD5(split[1]);
            String replace = split[0].replace('^', '=');
            String str = new String(Base64.decode(replace.substring(2, replace.length() - 2), 0), StandardCharsets.UTF_8);
            appKeyEntity.setmRawItems(str);
            Log.i(TAG, "items:" + str);
            appKeyEntity.setmItemsEntity(getRawItemsEntity(str));
            return appKeyEntity;
        } else {
            Log.i(TAG, "keyPair length invalid");
            throw new KeyFormatException();
        }
    }

    private RawItemsEntity getRawItemsEntity(String str) throws KeyFormatException {
        String[] split = str.split(";");
        RawItemsEntity rawItemsEntity = new RawItemsEntity();
        if (split.length < 1 || split.length > 2) {
            throw new KeyFormatException("raw item length invalid.");
        }
        try {
            short parseShort = Short.parseShort(split[0]);
            this.mAppId = parseShort;
            rawItemsEntity.setAppId(parseShort);
            if (split.length == 2) {
                try {
                    rawItemsEntity.setExpired(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(split[1]));
                } catch (ParseException unused) {
                    throw new KeyFormatException("expired field formate error.");
                }
            }
            return rawItemsEntity;
        } catch (NumberFormatException e) {
            Log.i(TAG, "appId invalid");
            e.printStackTrace();
            throw new KeyFormatException("app id format error.");
        }
    }

    public static class KeyFormatException extends Exception {

        private static final long serialVersionUID = 13923744320L;

        public KeyFormatException() {
        }

        public KeyFormatException(String str) {
            super(str);
        }
    }

    private static class AppKeyEntity {

        private String MD5;
        private RawItemsEntity mItemsEntity;
        private String mRawItems;

        private AppKeyEntity() {
            this.mRawItems = "";
            this.MD5 = "";
            this.mItemsEntity = null;
        }

        public RawItemsEntity getmItemsEntity() {
            return this.mItemsEntity;
        }

        public void setmItemsEntity(RawItemsEntity rawItemsEntity) {
            this.mItemsEntity = rawItemsEntity;
        }

        public String getMD5() {
            return this.MD5;
        }

        public void setMD5(String str) {
            this.MD5 = str;
        }

        public String getmRawItems() {
            return this.mRawItems;
        }

        public void setmRawItems(String str) {
            this.mRawItems = str;
        }
    }

    private static class RawItemsEntity {

        private short mAppId;
        private Date mExpired;

        private RawItemsEntity() {
            this.mAppId = 0;
            this.mExpired = null;
        }

        public short getAppId() {
            return this.mAppId;
        }

        public void setAppId(short s) {
            this.mAppId = s;
        }

        public Date getExpired() {
            return this.mExpired;
        }

        public void setExpired(Date date) {
            this.mExpired = date;
        }
    }
}
