package com.xunlei.downloadlib.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class XLUtil {

    public static String getMAC() {
        return "020000000000";
    }

    public static String getIMEI() {
        return "000000000000000";
    }

    public static String getPeerId() {
        return getIMEI() + "V";
    }

    public static GuidInfo generateGuid() {
        GuidInfo guidInfo = new GuidInfo();
        GuidType guid_type = GuidType.DEFAULT;
        guidInfo.mGuid = getIMEI() + "_" + getMAC();
        guidInfo.mType = guid_type;
        return guidInfo;
    }

    public static String getBSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo != null) return connectionInfo.getBSSID();
        return null;
    }

    public static int getNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return 0;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return 0;
        }
        int type = activeNetworkInfo.getType();
        if (type == 1) {
            return 9;
        }
        if (type != 0) {
            return 5;
        }
        int i;
        switch (activeNetworkInfo.getSubtype()) {
            case 1:
            case 2:
            case 4:
            case 7:
            case 11:
                return 2;
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 12:
            case 14:
            case 15:
                return 3;
            case 13:
                i = 4;
                break;
            default:
                i = 0;
                break;
        }
        return i;
    }

    public static String getMd5(String str) {
        try {
            char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            MessageDigest instance = MessageDigest.getInstance("MD5");
            byte[] bytes = str.getBytes();
            instance.update(bytes, 0, bytes.length);
            byte[] digest = instance.digest();
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(cArr[(b >> 4) & 15]);
                sb.append(cArr[(b) & 15]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return str;
        }
    }

    public static String generateAppKey(String str, short s, byte b) {
        int length = str.length();
        int i = length + 1;
        byte[] bArr = new byte[(i + 2 + 1)];
        byte[] bytes = str.getBytes();
        System.arraycopy(bytes, 0, bArr, 0, bytes.length);
        bArr[length] = 0;
        bArr[i] = (byte) (s & 255);
        bArr[length + 2] = (byte) ((s >> 8) & 255);
        bArr[length + 3] = b;
        return new String(Base64.encode(bArr, 0)).trim();
    }

    public static Map<String, Object> parseJSONString(String str) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return new Gson().fromJson(str, type);
    }

    public static NetWorkCarrier getNetWorkCarrier(Context context) {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String subscriberId = manager.getSubscriberId();
            if (subscriberId.startsWith("46000") || subscriberId.startsWith("46002")) return NetWorkCarrier.CMCC;
            if (subscriberId.startsWith("46001")) return NetWorkCarrier.CU;
            if (subscriberId.startsWith("46003")) return NetWorkCarrier.CT;
            return NetWorkCarrier.UNKNOWN;
        } catch (Exception e) {
            return NetWorkCarrier.UNKNOWN;
        }
    }

    public enum GuidType {
        DEFAULT,
    }

    public enum NetWorkCarrier {
        UNKNOWN,
        CMCC,
        CU,
        CT
    }

    public static class GuidInfo {
        public String mGuid = null;
        public GuidType mType = GuidType.DEFAULT;
    }
}
