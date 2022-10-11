package com.fongmi.android.tv.api;

import android.util.Base64;

import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Decoder {

    public static String getJson(String url) throws Exception {
        String key = url.contains(";") ? url.split(";")[2] : "";
        url = url.contains(";") ? url.split(";")[0] : url;
        String data = getData(url);
        if (Json.valid(data)) return data;
        if (key.length() > 0) return ecb(data, key);
        if (data.contains("**")) data = base64(data);
        if (data.startsWith("2423")) data = cbc(data);
        return data.replace("###", "");
    }

    private static String getData(String url) throws Exception {
        if (url.startsWith("http")) return OKHttp.newCall(url).execute().body().string();
        else if (url.startsWith("file")) return FileUtil.read(url);
        throw new Exception();
    }

    private static String ecb(String data, String key) throws Exception {
        SecretKeySpec spec = new SecretKeySpec(padEnd(key), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, spec);
        return new String(cipher.doFinal(decodeHex(data)), StandardCharsets.UTF_8);
    }

    private static String cbc(String data) throws Exception {
        int indexKey = data.indexOf("2324") + 4;
        String key = new String(decodeHex(data.substring(0, indexKey)), StandardCharsets.UTF_8);
        key = key.replace("$#", "").replace("#$", "");
        int indexIv = data.length() - 26;
        String iv = data.substring(indexIv).trim();
        iv = new String(decodeHex(iv), StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(padEnd(key), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(padEnd(iv));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        data = data.substring(indexKey, indexIv).trim();
        byte[] encryptDataBytes = decodeHex(data);
        byte[] decryptData = cipher.doFinal(encryptDataBytes);
        return new String(decryptData, StandardCharsets.UTF_8);
    }

    private static String base64(String data) {
        return new String(Base64.decode(data.substring(data.indexOf("**") + 2), Base64.DEFAULT));
    }

    private static byte[] padEnd(String key) {
        return (key + "0000000000000000".substring(key.length())).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] decodeHex(String s) {
        int len = s.length() / 2;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) data[i] = Integer.valueOf(s.substring(i * 2, i * 2 + 2), 16).byteValue();
        return data;
    }
}
