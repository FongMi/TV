package com.xunlei.downloadlib.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Base64;

import java.util.Random;
import java.util.UUID;

public class XLUtil {

    public static String getMAC() {
        return random("ABCDEF0123456", 12).toUpperCase();
    }

    public static String getIMEI() {
        return random("0123456", 15);
    }

    public static String getPeerId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        uuid = uuid.substring(0, 12).toUpperCase() + "004V";
        return uuid;
    }

    private static String random(String base, int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(base.charAt(random.nextInt(base.length())));
        return sb.toString();
    }

    public static String getGuid() {
        return getIMEI() + "_" + getMAC();
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

    public enum NetWorkCarrier {
        UNKNOWN, CMCC, CU, CT
    }
}
