package com.fongmi.android.tv.utils;

import com.github.catvod.crawler.SpiderDebug;

import org.json.JSONObject;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    public static String rightPading(String key, String replace, int Length) {
        String strReturn = "";
        String strtemp = "";
        int curLength = key.trim().length();
        if (key != null && curLength > Length) {
            strReturn = key.trim().substring(0, Length);
        } else if (key != null && curLength == Length) {
            strReturn = key.trim();
        } else {
            for (int i = 0; i < (Length - curLength); i++) {
                strtemp = strtemp + replace;
            }
            strReturn = key.trim() + strtemp;
        }
        return strReturn;
    }

    public static String ECB(String data, String key) {
        try {
            key = rightPading(key, "0", 16);
            byte[] data2 = toBytes(data);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(data2));
        } catch (Exception exception) {
            SpiderDebug.log(exception);
        }
        return null;
    }

    public static String CBC(String src, String KEY, String IV) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(IV.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            return new String(cipher.doFinal(toBytes(src)));
        } catch (Exception exception) {
            SpiderDebug.log(exception);
        }
        return null;
    }

    public static byte[] toBytes(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = Integer.valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }

    public static boolean isJson(String content) {
        try {
            new JSONObject(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
