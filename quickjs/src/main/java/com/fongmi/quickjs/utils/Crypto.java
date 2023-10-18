package com.fongmi.quickjs.utils;

import android.util.Base64;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

    public static String aes(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        try {
            byte[] keyBuf = key.getBytes();
            if (keyBuf.length < 16) keyBuf = Arrays.copyOf(keyBuf, 16);
            byte[] ivBuf = iv == null ? new byte[0] : iv.getBytes();
            if (ivBuf.length < 16) ivBuf = Arrays.copyOf(ivBuf, 16);
            Cipher cipher = Cipher.getInstance(mode + "Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBuf, "AES");
            if (iv == null) cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec);
            else cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ivBuf));
            byte[] inBuf = inBase64 ? Base64.decode(input.replaceAll("_", "/").replaceAll("-", "+"), Base64.DEFAULT) : input.getBytes("UTF-8");
            return outBase64 ? Base64.encodeToString(cipher.doFinal(inBuf), Base64.NO_WRAP) : new String(cipher.doFinal(inBuf), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String rsa(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        try {
            Key rsaKey = generateKey(pub, key);
            int len = getModulusLength(rsaKey);
            byte[] outBytes = new byte[0];
            byte[] inBytes = inBase64 ? Base64.decode(input.replaceAll("_", "/").replaceAll("-", "+"), Base64.DEFAULT) : input.getBytes("UTF-8");
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, rsaKey);
            int blockLen = encrypt ? len / 8 - 11 : len / 8;
            int bufIdx = 0;
            while (bufIdx < inBytes.length) {
                int bufEndIdx = Math.min(bufIdx + blockLen, inBytes.length);
                byte[] tmpInBytes = new byte[bufEndIdx - bufIdx];
                System.arraycopy(inBytes, bufIdx, tmpInBytes, 0, tmpInBytes.length);
                byte[] tmpBytes = cipher.doFinal(tmpInBytes);
                bufIdx = bufEndIdx;
                outBytes = concatArrays(outBytes, tmpBytes);
            }
            return outBase64 ? Base64.encodeToString(outBytes, Base64.NO_WRAP) : new String(outBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Key generateKey(boolean pub, String key) throws Exception {
        if (pub) key = key.replaceAll(System.lineSeparator(), "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        else key = key.replaceAll(System.lineSeparator(), "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
        return pub ? KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT))) : KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(key, Base64.DEFAULT)));
    }

    private static int getModulusLength(Key key) {
        if (key instanceof PublicKey) return ((RSAPublicKey) key).getModulus().bitLength();
        else return ((RSAPrivateKey) key).getModulus().bitLength();
    }

    private static byte[] concatArrays(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] result = new byte[aLen + bLen];
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);
        return result;
    }
}
