package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.fongmi.android.tv.App;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static boolean isEnterKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_SPACE || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    public static boolean isUpKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_UP || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }

    public static boolean isDownKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT;
    }

    public static boolean isLeftKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT;
    }

    public static boolean isRightKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    public static boolean isBackKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_BACK;
    }

    public static void hideSystemUI(Activity activity) {
        int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public static boolean isVideoFormat(String url) {
        return isVideoFormat(url, new HashMap<>());
    }

    public static boolean isVideoFormat(String url, Map<String, String> headers) {
        if (headers.containsKey("Accept") && headers.get("Accept").contains("image")) return false;
        if (url.contains(".js") || url.contains(".css")) return false;
        return Sniffer.RULE.matcher(url).find();
    }

    public static boolean isVip(String url) {
        List<String> hosts = Arrays.asList("iqiyi.com", "v.qq.com", "youku.com", "le.com", "tudou.com", "mgtv.com", "sohu.com", "acfun.cn", "bilibili.com", "baofeng.com", "pptv.com");
        for (String host : hosts) if (url.contains(host)) return true;
        return false;
    }

    public static String checkClan(String text) {
        if (text.contains("/localhost/")) text = text.replace("/localhost/", "/");
        if (text.startsWith("clan")) text = text.replace("clan", "file");
        return text;
    }

    public static String convert(String text) {
        if (TextUtils.isEmpty(text)) return "";
        if (text.startsWith("clan")) return checkClan(text);
        if (text.startsWith(".")) text = text.substring(1);
        if (text.startsWith("/")) text = text.substring(1);
        Uri uri = Uri.parse(Prefers.getUrl());
        if (uri.getLastPathSegment() == null) return uri.getScheme() + "://" + text;
        return uri.toString().replace(uri.getLastPathSegment(), text);
    }

    public static String getMD5(String src) {
        try {
            if (TextUtils.isEmpty(src)) return "";
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(src.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder sb = new StringBuilder(no.toString(16));
            while (sb.length() < 32) sb.insert(0, "0");
            return sb.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static String getBase64(String ext) {
        return Base64.encodeToString(ext.getBytes(), Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static int getDigit(String text) {
        try {
            if (text.startsWith("上") || text.startsWith("下")) return -1;
            if (text.contains(".")) text = text.substring(0, text.lastIndexOf("."));
            return Integer.parseInt(text.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder windowToken = view.getWindowToken();
        if (imm != null && windowToken != null) {
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }
}
