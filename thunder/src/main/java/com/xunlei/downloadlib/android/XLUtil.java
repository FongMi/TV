package com.xunlei.downloadlib.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    public static int getNetworkTypeComplete(Context context) {
        if (context == null) {
            return 0;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        int i = 0;
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            i = 0;
            if (activeNetworkInfo != null) {
                int type = activeNetworkInfo.getType();
                if (type == 1) {
                    i = 9;
                } else if (type == 0) {
                    switch (activeNetworkInfo.getSubtype()) {
                        case 1:
                        case 2:
                        case 4:
                        case 7:
                        case 11:
                            i = 2;
                            break;
                        case 3:
                        case 5:
                        case 6:
                        case 8:
                        case 9:
                        case 10:
                        case 12:
                        case 14:
                        case 15:
                            i = 3;
                            break;
                        case 13:
                            i = 4;
                            break;
                        default:
                            i = 0;
                            break;
                    }
                } else {
                    i = 5;
                }
            }
        }
        return i;
    }
}
