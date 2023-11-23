package com.github.catvod.utils;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    public static String base64(String ext) {
        return base64(ext.getBytes());
    }

    public static String base64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static String basic(Uri uri) {
        return "Basic " + base64(uri.getUserInfo());
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
}
