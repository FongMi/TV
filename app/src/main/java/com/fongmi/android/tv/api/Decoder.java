package com.fongmi.android.tv.api;

import android.util.Base64;

import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Json;
import com.google.common.io.BaseEncoding;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Decoder {

    public static String getJson(String url) throws Exception {
        String key = url.contains(";") ? url.split(";")[2] : "";
        url = url.contains(";") ? url.split(";")[0] : url;
        String data = getData(url);
        if (Json.valid(data)) return data;
        if (data.isEmpty()) throw new Exception();
        if (data.contains("**")) data = base64(data);
        if (data.startsWith("2423")) data = cbc(data);
        if (key.length() > 0) data = ecb(data, key);
        return data;
    }

    public static String getExt(String ext) {
        try {
            return base64(getData(ext.substring(4)));
        } catch (Exception ignored) {
            return "";
        }
    }

    public static File getSpider(String jar, String md5) {
        try {
            File file = FileUtil.getJar(jar);
            if (md5.length() > 0 && FileUtil.equals(jar, md5)) return file;
            String data = extract(getData(jar.substring(4)));
            if (data.isEmpty()) return FileUtil.getJar(jar);
            return FileUtil.write(file, Base64.decode(data, Base64.DEFAULT));
        } catch (Exception ignored) {
            return FileUtil.getJar(jar);
        }
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
        return new String(cipher.doFinal(decodeHex(data)), "UTF-8");
    }

    private static String cbc(String data) throws Exception {
        int indexKey = data.indexOf("2324") + 4;
        String key = new String(decodeHex(data.substring(0, indexKey)), "UTF-8");
        key = key.replace("$#", "").replace("#$", "");
        int indexIv = data.length() - 26;
        String iv = data.substring(indexIv).trim();
        iv = new String(decodeHex(iv), "UTF-8");
        SecretKeySpec keySpec = new SecretKeySpec(padEnd(key), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(padEnd(iv));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        data = data.substring(indexKey, indexIv).trim();
        byte[] encryptDataBytes = decodeHex(data);
        byte[] decryptData = cipher.doFinal(encryptDataBytes);
        return new String(decryptData, "UTF-8");
    }

    private static String base64(String data) {
        String extract = extract(data);
        if (extract.isEmpty()) return data;
        return new String(Base64.decode(extract, Base64.DEFAULT));
    }

    private static String extract(String data) {
        Matcher matcher = Pattern.compile("[A-Za-z0-9]{8}\\*\\*").matcher(data);
        return matcher.find() ? data.substring(data.indexOf(matcher.group()) + 10) : "";
    }

    private static byte[] padEnd(String key) throws UnsupportedEncodingException {
        return (key + "0000000000000000".substring(key.length())).getBytes("UTF-8");
    }

    private static byte[] decodeHex(String s) {
        return BaseEncoding.base16().decode(s.toUpperCase());
    }
}
