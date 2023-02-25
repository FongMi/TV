package com.fongmi.android.tv.utils;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.server.Server;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.permissionx.guolindev.PermissionX;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
        if (fullscreen) Utils.hideSystemUI(activity);
        else Utils.showSystemUI(activity);
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

    public static void toggleFab(int dy, FloatingActionButton fab) {
        if (Math.abs(dy) < 50) return;
        if (dy > 0) Utils.hideFab(fab);
        else Utils.showFab(fab);
    }

    public static void showFab(FloatingActionButton fab) {
        if (fab.getVisibility() == View.INVISIBLE) fab.show();
    }

    public static void hideFab(FloatingActionButton fab) {
        if (fab.getVisibility() != View.VISIBLE) return;
        fab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                fab.setVisibility(View.INVISIBLE);
            }
        });
    }

    public static boolean hasPIP() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && App.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    public static void enterPIP(Activity activity, Rect sourceRectHint, Rational rational) {
        try {
            if (!hasPIP() || activity.isInPictureInPictureMode()) return;
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            builder.setAspectRatio(rational).build();
            builder.setSourceRectHint(sourceRectHint);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setSeamlessResizeEnabled(true);
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isVideoFormat(String url) {
        return isVideoFormat(url, new HashMap<>());
    }

    public static boolean isVideoFormat(String url, Map<String, String> headers) {
        if (Sniffer.CUSTOM.matcher(url).find()) return true;
        if (headers.containsKey("Accept") && headers.get("Accept").startsWith("image")) return false;
        if (url.contains("url=http") || url.contains(".js") || url.contains(".css") || url.contains(".html")) return false;
        return Sniffer.RULE.matcher(url).find();
    }

    public static boolean isAutoRotate() {
        return Settings.System.getInt(App.get().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    public static boolean hasPermission(FragmentActivity activity) {
        return PermissionX.isGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static String checkProxy(String url) {
        if (url.startsWith("proxy://")) return url.replace("proxy://", Server.get().getAddress("proxy?"));
        return url;
    }

    public static String checkClan(String text) {
        if (text.contains("/localhost/")) text = text.replace("/localhost/", "/");
        if (text.startsWith("clan")) text = text.replace("clan", "file");
        return text;
    }

    public static String convert(String text) {
        return text.startsWith("file") ? Server.get().getAddress(text) : text;
    }

    public static String convert(String baseUrl, String text) {
        if (TextUtils.isEmpty(text)) return "";
        if (text.startsWith("clan")) return checkClan(text);
        if (text.startsWith(".")) text = text.substring(1);
        if (text.startsWith("/")) text = text.substring(1);
        String last = Uri.parse(baseUrl).getLastPathSegment();
        if (last == null) return Uri.parse(baseUrl).getScheme() + "://" + text;
        int index = baseUrl.lastIndexOf(last);
        return baseUrl.substring(0, index) + text;
    }

    public static String getMd5(String src) {
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

    public static String getUUID() {
        return Settings.Secure.getString(App.get().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getBase64(String ext) {
        return Base64.encodeToString(ext.getBytes(), Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static String substring(String text) {
        return substring(text, 1);
    }

    public static String substring(String text, int num) {
        if (text != null && text.length() > num) return text.substring(0, text.length() - num);
        return text;
    }

    public static CharSequence getClipText() {
        return ((ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE)).getText();
    }

    public static long format(SimpleDateFormat format, String src) {
        try {
            return format.parse(src).getTime();
        } catch (Exception e) {
            return 0;
        }
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
