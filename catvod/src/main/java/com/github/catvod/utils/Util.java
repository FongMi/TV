package com.github.catvod.utils;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Base64;

import com.github.catvod.Init;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class Util {

    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    public static String base64(String s) {
        return base64(s.getBytes());
    }

    public static String base64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static String basic(Uri uri) {
        return "Basic " + base64(uri.getUserInfo());
    }

    public static byte[] hex2byte(String s) {
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = Integer.valueOf(s.substring(i * 2, i * 2 + 2), 16).byteValue();
        return bytes;
    }

    public static boolean equals(String name, String md5) {
        return md5(Path.jar(name)).equalsIgnoreCase(md5);
    }

    public static String md5(String src) {
        try {
            if (TextUtils.isEmpty(src)) return "";
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(src.getBytes());
            BigInteger no = new BigInteger(1, bytes);
            StringBuilder sb = new StringBuilder(no.toString(16));
            while (sb.length() < 32) sb.insert(0, "0");
            return sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String md5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int count;
            while ((count = fis.read(byteArray)) != -1) digest.update(byteArray, 0, count);
            fis.close();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean containOrMatch(String text, String regex) {
        try {
            return text.contains(regex) || text.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getIp() {
        try {
            WifiManager manager = (WifiManager) Init.context().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int address = manager.getConnectionInfo().getIpAddress();
            if (address != 0) return Formatter.formatIpAddress(address);
            return getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

    private static String getHostAddress() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface interfaces = en.nextElement();
            for (Enumeration<InetAddress> addresses = interfaces.getInetAddresses(); addresses.hasMoreElements(); ) {
                InetAddress inetAddress = addresses.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        return "";
    }
}
