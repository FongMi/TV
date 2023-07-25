package com.xunlei.downloadlib.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XLUtil {
    public static final int IMEI_LEN = 15;
    public static int NETWORKSUBTYPE = -1;
    public static int NETWORKTYPE = -1;
    public static boolean PRINT_LOG = false;
    public static String SSID = null;
    private static final String TAG = "XLUtil";
    public static boolean isGetIMEI = false;
    public static boolean isGetMAC = false;
    private static boolean isLoadAndParseFile = false;
    public static String mAPNName = null;
    private static String mIMEI = null;
    private static String mIdentifyFileName = "Identify2.txt";
    private static String mMAC = null;
    private static String mOSVersion = null;
    private static String mPeerId = null;
    public static final int mProductId = 10101;

    public enum GUID_TYPE {
        DEFAULT,
        JUST_IMEI,
        JUST_MAC,
        ALL
    }

    public static class GuidInfo {
        public String mGuid = null;
        public GUID_TYPE mType = GUID_TYPE.DEFAULT;
    }

    public enum NetWorkCarrier {
        UNKNOWN,
        CMCC,
        CU,
        CT
    }

    XLUtil() {
    }

    public static long getCurrentUnixTime() {
        return System.currentTimeMillis() / 1000;
    }

    public static String getPeerid(Context context) {
        if (!isLoadAndParseFile) {
            loadAndParseFile(context, mIdentifyFileName);
        }
        String str = mPeerId;
        if (str != null) {
            return str;
        }
        if (!isGetMAC) {
            mMAC = getMAC(context);
        }
        if (mMAC == null || !isGetMAC) {
            if (!isGetIMEI) {
                mIMEI = getIMEI(context);
            }
            if (mIMEI != null && isGetIMEI) {
                mPeerId = mIMEI + "V";
            }
        } else {
            mPeerId = mMAC + "004V";
        }
        if (mPeerId != null) {
            saveFile(context, mIdentifyFileName);
        }
        return mPeerId;
    }

    public static String getMAC(Context context) {
//        return "000000000000";
        String str;
        if (!isLoadAndParseFile) {
            loadAndParseFile(context, mIdentifyFileName);
        }
        if (isGetMAC && (str = mMAC) != null) {
            return str;
        }
        String wifiMacAddress = getWifiMacAddress();
        if (TextUtils.isEmpty(wifiMacAddress)) {
            return null;
        }
        String upperCase = wifiMacAddress.replaceAll(":", "").replaceAll(",", "").replaceAll("[.]", "").toUpperCase();
        isGetMAC = true;
        mMAC = upperCase;
        saveFile(context, mIdentifyFileName);
        return upperCase;
    }

    @SuppressLint({"NewApi"})
    public static String getWifiMacAddress() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.getName().equalsIgnoreCase("wlan0")) {
                    byte[] hardwareAddress = networkInterface.getHardwareAddress();
                    if (hardwareAddress == null) {
                        return null;
                    }
                    StringBuilder sb = new StringBuilder();
                    int length = hardwareAddress.length;
                    for (int i = 0; i < length; i++) {
                        sb.append(String.format("%02X:", Byte.valueOf(hardwareAddress[i])));
                    }
                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("WrongConstant")
    public static String getIMEI(Context context) {
//        return "000000000000000";
        TelephonyManager telephonyManager;
        String str;
        if (!isLoadAndParseFile) {
            loadAndParseFile(context, mIdentifyFileName);
        }
        if (isGetIMEI && (str = mIMEI) != null) {
            return str;
        }
        String str2 = null;
        if (!(context == null || (telephonyManager = (TelephonyManager) context.getSystemService("phone")) == null)) {
            try {
                str2 = telephonyManager.getDeviceId();
                if (str2 != null) {
                    if (str2.length() < 15) {
                        int length = 15 - str2.length();
                        while (true) {
                            int i = length - 1;
                            if (length <= 0) {
                                break;
                            }
                            str2 = str2 + "M";
                            length = i;
                        }
                    }
                    isGetIMEI = true;
                    mIMEI = str2;
                    saveFile(context, mIdentifyFileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return str2;
    }

    public static GuidInfo generateGuid(Context context) {
        GuidInfo guidInfo = new GuidInfo();
        GUID_TYPE guid_type = GUID_TYPE.DEFAULT;
        if (!isGetIMEI) {
            mIMEI = getIMEI(context);
        }
        if (!isGetIMEI) {
            mIMEI = "000000000000000";
        } else {
            guid_type = GUID_TYPE.JUST_IMEI;
        }
        if (!isGetMAC) {
            mMAC = getMAC(context);
        }
        if (!isGetMAC) {
            mMAC = "000000000000";
        } else if (guid_type == GUID_TYPE.JUST_IMEI) {
            guid_type = GUID_TYPE.ALL;
        } else {
            guid_type = GUID_TYPE.JUST_MAC;
        }
        guidInfo.mGuid = mIMEI + "_" + mMAC;
        guidInfo.mType = guid_type;
        return guidInfo;
    }

    public static String getIdentifyContent() {
        String str;
        String str2 = "";
        if (!TextUtils.isEmpty(mPeerId)) {
            str2 = str2 + "peerid=" + mPeerId + "\n";
            str = str2 + "peerid=" + mPeerId + ";";
        } else {
            str = str2;
        }
        if (isGetMAC && !TextUtils.isEmpty(mMAC)) {
            str = str + "MAC=" + mMAC + ";";
            str2 = str2 + "MAC=" + mMAC + "\n";
        }
        if (!isGetIMEI || TextUtils.isEmpty(mIMEI)) {
            return str2;
        }
        String str3 = str + "IMEI=" + mIMEI;
        return str2 + "IMEI=" + mIMEI;
    }

    private static void parseIdentify(String str) {
        if (str == null) {
            XLLog.e(TAG, "parseIdentify, item invalid");
            return;
        }
        String[] split = str.split("=");
        if (split.length == 2) {
            if (split[0].compareTo("peerid") == 0) {
                if (split[1].trim().length() != 0) {
                    mPeerId = split[1];
                }
                String str2 = mPeerId;
                if (str2 != null && str2.compareTo("null") == 0) {
                    mPeerId = null;
                }
            } else if (split[0].compareTo("MAC") == 0) {
                if (split[1].trim().length() != 0) {
                    mMAC = split[1];
                }
                String str3 = mMAC;
                if (str3 == null || str3.compareTo("null") == 0) {
                    mMAC = null;
                } else {
                    isGetMAC = true;
                }
            } else if (split[0].compareTo("IMEI") == 0) {
                if (split[1].trim().length() != 0) {
                    mIMEI = split[1];
                }
                String str4 = mIMEI;
                if (str4 == null || str4.compareTo("null") == 0) {
                    mIMEI = null;
                } else {
                    isGetIMEI = true;
                }
            }
        }
    }

    private static void loadAndParseFile(Context context, String str) {
        String[] split;
        XLLog.i(TAG, "loadAndParseFile start");
        isLoadAndParseFile = true;
        if (context == null || str == null) {
            XLLog.e(TAG, "loadAndParseFile end, parameter invalid, fileName:" + str);
            return;
        }
        String readFromFile = readFromFile(context, str);
        if (readFromFile == null) {
            XLLog.i(TAG, "loadAndParseFile end, fileContext is empty");
            return;
        }
        for (String str2 : readFromFile.split("\n")) {
            parseIdentify(str2);
        }
        XLLog.i(TAG, "loadAndParseFile end");
    }

    private static void saveFile(Context context, String str) {
        if (context == null || str == null) {
            XLLog.e(TAG, "saveFile, parameter invalid, fileName:" + str);
            return;
        }
        writeToFile(context, getIdentifyContent(), mIdentifyFileName);
    }

    public static String getOSVersion(Context context) {
        if (mOSVersion == null) {
            mOSVersion = "SDKV = " + Build.VERSION.RELEASE;
            mOSVersion += "_MANUFACTURER = " + Build.MANUFACTURER;
            mOSVersion += "_MODEL = " + Build.MODEL;
            mOSVersion += "_PRODUCT = " + Build.PRODUCT;
            mOSVersion += "_FINGERPRINT = " + Build.FINGERPRINT;
            mOSVersion += "_CPU_ABI = " + Build.CPU_ABI;
            mOSVersion += "_ID = " + Build.ID;
        }
        return mOSVersion;
    }

    public static void killProcess() {
        Process.killProcess(Process.myPid());
    }

    @SuppressLint("WrongConstant")
    public static String getAPNName(Context context) {
        if (context != null) {
            return ((ConnectivityManager) context.getSystemService("connectivity")).getNetworkInfo(0).getExtraInfo();
        }
        return null;
    }

    public static String getSSID(Context context) {
        WifiInfo connectionInfo;
        if (context == null) {
            XLLog.e(TAG, "getSSID, context invalid");
            return null;
        }
        @SuppressLint("WrongConstant") WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        if (wifiManager == null || (connectionInfo = wifiManager.getConnectionInfo()) == null) {
            return null;
        }
        return connectionInfo.getSSID();
    }

    public static String getBSSID(Context context) {
        if (context == null) {
            XLLog.e(TAG, "getBSSID, context invalid");
            return null;
        }
        @SuppressLint("WrongConstant") WifiManager wifiManager = (WifiManager) context.getSystemService("wifi");
        if (wifiManager != null) {
            try {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    return connectionInfo.getBSSID();
                }
            } catch (Exception unused) {
            }
        }
        return null;
    }

    public static int getNetworkType(Context context) {
        NetworkInfo activeNetworkInfo;
        int i;
        if (context == null) {
            XLLog.e(TAG, "getNetworkType, context invalid");
            return 0;
        }
        @SuppressLint("WrongConstant") ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null || (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) == null) {
            return 0;
        }
        int type = activeNetworkInfo.getType();
        if (type == 1) {
            return 9;
        }
        if (type != 0) {
            return 5;
        }
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
                return 0;
        }
        return i;
    }

    public static void writeToFile(Context context, String str, String str2) {
        if (context == null || str == null || str2 == null) {
            XLLog.e(TAG, "writeToFile, Parameter invalid, fileName:" + str2);
            return;
        }
        try {
            FileOutputStream openFileOutput = context.openFileOutput(str2, 0);
            try {
                openFileOutput.write(str.getBytes(StandardCharsets.UTF_8));
                openFileOutput.close();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (FileNotFoundException e3) {
            e3.printStackTrace();
        }
    }

    public static String readFromFile(Context context, String str) {
        String str2 = null;
        if (context == null || str == null) {
            XLLog.e(TAG, "readFromFile, parameter invalid, fileName:" + str);
            return null;
        }
        try {
            FileInputStream openFileInput = context.openFileInput(str);
            byte[] bArr = new byte[256];
            try {
                int read = openFileInput.read(bArr);
                if (read > 0) {
                    str2 = new String(bArr, 0, read, StandardCharsets.UTF_8);
                }
                openFileInput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException unused) {
            XLLog.i(TAG, str + " File Not Found");
        }
        return str2;
    }

    public static int writeStringToFile(String str, String str2) {
        if (str2 == null || str == null) {
            XLLog.e(TAG, "writeStringToFile, parameter invalid, path:" + str2);
            return -1;
        }
        try {
            File file = new File(str2);
            if (file.exists()) {
                return 0;
            }
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
            outputStreamWriter.write(str);
            outputStreamWriter.close();
            return 0;
        } catch (Exception e) {
            XLLog.e(TAG, "writeStringToFile error:" + e.getMessage());
            return -1;
        }
    }

    public static String readStringFromFile(String str) {
        if (str == null) {
            XLLog.e(TAG, "readStringFromFile, path invalid");
            return null;
        }
        File file = new File(str);
        if (file.isDirectory()) {
            return null;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            String str2 = "";
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    str2 = str2 + readLine;
                } else {
                    fileInputStream.close();
                    return str2;
                }
            }
        } catch (Exception e) {
            XLLog.e(TAG, "readStringFromFile error:" + e.getMessage());
            return null;
        }
    }

    public static void deleteFile(String str) {
        if (str == null) {
            XLLog.e(TAG, "deleteFile, path invalid");
            return;
        }
        File file = new File(str);
        if (file.exists()) {
            file.delete();
        }
    }

    public static String getMd5(String str) {
        if (str == null) {
            XLLog.e(TAG, "getMd5, key invalid");
            return null;
        }
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
        if (str == null) {
            XLLog.e(TAG, "generateAppKey, appName invalid");
            return null;
        }
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
        if (str == null) {
            XLLog.e(TAG, "parseJSONString, JSONString invalid");
            return null;
        }
        HashMap hashMap = new HashMap();
        try {
            JSONObject jSONObject = new JSONObject(str);
            Iterator<String> keys = jSONObject.keys();
            while (keys.hasNext()) {
                String next = keys.next();
                hashMap.put(next, jSONObject.getString(next));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hashMap;
    }

    @SuppressLint("WrongConstant")
    public static NetWorkCarrier getNetWorkCarrier(Context context) {
        TelephonyManager telephonyManager;
        if (!(context == null || (telephonyManager = (TelephonyManager) context.getSystemService("phone")) == null)) {
            try {
                String subscriberId = telephonyManager.getSubscriberId();
                if (!subscriberId.startsWith("46000")) {
                    if (!subscriberId.startsWith("46002")) {
                        if (subscriberId.startsWith("46001")) {
                            return NetWorkCarrier.CU;
                        }
                        if (subscriberId.startsWith("46003")) {
                            return NetWorkCarrier.CT;
                        }
                    }
                }
                return NetWorkCarrier.CMCC;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return NetWorkCarrier.UNKNOWN;
    }
}
