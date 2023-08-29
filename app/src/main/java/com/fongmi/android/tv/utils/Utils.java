package com.fongmi.android.tv.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.util.UriUtil;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.server.Server;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;
import com.permissionx.guolindev.PermissionX;

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

    public static boolean isDigitKey(KeyEvent event) {
        return event.getKeyCode() >= KeyEvent.KEYCODE_0 && event.getKeyCode() <= KeyEvent.KEYCODE_9 || event.getKeyCode() >= KeyEvent.KEYCODE_NUMPAD_0 && event.getKeyCode() <= KeyEvent.KEYCODE_NUMPAD_9;
    }

    public static boolean isMenuKey(KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_MENU;
    }

    public static void toggleFullscreen(Activity activity, boolean fullscreen) {
        if (fullscreen) hideSystemUI(activity);
        else showSystemUI(activity);
    }

    public static void showSystemUI(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    public static void hideSystemUI(Activity activity) {
        hideSystemUI(activity.getWindow());
    }

    public static void hideSystemUI(Window window) {
        int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        window.getDecorView().setSystemUiVisibility(flags);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder windowToken = view.getWindowToken();
        if (imm == null || windowToken == null) return;
        imm.hideSoftInputFromWindow(windowToken, 0);
    }

    public static boolean isAutoRotate() {
        return Settings.System.getInt(App.get().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    public static boolean hasPermission(FragmentActivity activity) {
        return PermissionX.isGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static CharSequence getClipText() {
        return ((ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE)).getText();
    }

    public static Map<String, String> checkUa(Map<String, String> headers) {
        if (Setting.getUa().isEmpty() || headers.containsKey(HttpHeaders.USER_AGENT) || headers.containsKey(HttpHeaders.USER_AGENT.toLowerCase())) return headers;
        headers.put(HttpHeaders.USER_AGENT, Setting.getUa());
        return headers;
    }

    public static String checkClan(String text) {
        if (text.contains("/localhost/")) text = text.replace("/localhost/", "/");
        if (text.startsWith("clan")) text = text.replace("clan", "file");
        return text;
    }

    public static String convert(String baseUrl, String text) {
        if (TextUtils.isEmpty(text)) return "";
        if (text.startsWith("clan")) return checkClan(text);
        return UriUtil.resolve(baseUrl, text);
    }

    public static String convert(String url) {
        String host = Util.host(url);
        String scheme = Util.scheme(url);
        if ("file".equals(scheme)) return Server.get().getAddress(url);
        if ("local".equals(scheme)) return Server.get().getAddress(host);
        if ("proxy".equals(scheme)) return url.replace("proxy://", Server.get().getAddress("proxy?"));
        return url;
    }

    public static int getDigit(String text) {
        try {
            if (text.startsWith("上") || text.startsWith("下")) return -1;
            return Integer.parseInt(text.replaceAll("(mp4|H264|H265|720p|1080p|2160p|4k|4K)", "").replaceAll("\\D+", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
