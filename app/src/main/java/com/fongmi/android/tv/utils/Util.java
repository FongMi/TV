package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.github.catvod.utils.Shell;

import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

public class Util {

    public static void toggleFullscreen(Activity activity, boolean fullscreen) {
        if (fullscreen) hideSystemUI(activity);
        else showSystemUI(activity);
    }

    public static void hideSystemUI(Activity activity) {
        hideSystemUI(activity.getWindow());
    }

    public static void hideSystemUI(Window window) {
        WindowInsetsControllerCompat insets = WindowCompat.getInsetsController(window, window.getDecorView());
        insets.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insets.hide(WindowInsetsCompat.Type.systemBars());
    }

    public static void showSystemUI(Activity activity) {
        showSystemUI(activity.getWindow());
    }

    public static void showSystemUI(Window window) {
        WindowCompat.getInsetsController(window, window.getDecorView()).show(WindowInsetsCompat.Type.systemBars());
    }

    public static void showKeyboard(View view) {
        if (!view.requestFocus()) return;
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) view.postDelayed(() -> imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT), 250);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder windowToken = view.getWindowToken();
        if (imm == null || windowToken == null) return;
        imm.hideSoftInputFromWindow(windowToken, 0);
    }

    public static float getBrightness(Activity activity) {
        try {
            float value = activity.getWindow().getAttributes().screenBrightness;
            if (WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL >= value && value >= WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF) return value;
            return Settings.System.getFloat(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 128;
        } catch (Exception e) {
            return 0.5f;
        }
    }

    public static CharSequence getClipText() {
        ClipboardManager manager = (ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = manager == null ? null : manager.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) return "";
        return clipData.getItemAt(0).getText();
    }

    public static void copy(String text) {
        try {
            ClipboardManager manager = (ClipboardManager) App.get().getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(ClipData.newPlainText("", text));
            Notify.show(R.string.copied);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getDigit(String text) {
        try {
            if (text.startsWith("上") || text.startsWith("下")) return -1;
            return Integer.parseInt(text.replaceAll("(?i)(mp4|H264|H265|720p|1080p|2160p|4K)", "").replaceAll("\\D+", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    public static List<String> getPart(String text) {
        List<String> items = new ArrayList<>();
        String[] splits = new String[0];
        items.add(text);
        if (text.contains("：")) {
            splits = text.split("：");
        } else if (text.contains("第") && text.contains("季")) {
            splits = Arrays.stream(text.split("第")).filter(s -> !s.isEmpty() && !s.contains("季")).toArray(String[]::new);
        } else if (text.contains("(")) {
            splits = new String[]{text.split("\\(")[0]};
        } else if (text.contains(" ")) {
            splits = text.split(" ");
        }
        for (String s : splits) {
            s = s.trim();
            if (s.contains(" ")) s = s.split(" ")[0].trim();
            if (!s.isEmpty()) items.add(s);
        }
        return items;
    }

    public static String clean(String text) {
        StringBuilder sb = new StringBuilder();
        text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().replace("\u00A0", "").replace("\u3000", "");
        for (String line : text.split("\\r?\\n")) if (!line.isEmpty()) sb.append(line.trim()).append("\n");
        return substring(sb.toString());
    }

    public static String getAndroidId() {
        try {
            String id = Settings.Secure.getString(App.get().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(id)) throw new NullPointerException();
            return id;
        } catch (Exception e) {
            return "0000000000000000";
        }
    }

    public static String getSerial() {
        return Shell.exec("getprop ro.serialno").replace("\n", "");
    }

    public static String getMac(String name) {
        try {
            StringBuilder sb = new StringBuilder();
            NetworkInterface nif = NetworkInterface.getByName(name);
            if (nif.getHardwareAddress() == null) return "";
            for (byte b : nif.getHardwareAddress()) sb.append(String.format("%02X:", b));
            return substring(sb.toString());
        } catch (Exception e) {
            return "";
        }
    }

    public static String getDeviceName() {
        String model = Build.MODEL;
        String manufacturer = Build.MANUFACTURER;
        return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
    }

    public static String substring(String text) {
        return substring(text, 1);
    }

    public static String substring(String text, int num) {
        if (text != null && text.length() > num) return text.substring(0, text.length() - num);
        return text;
    }

    public static long format(String src, List<SimpleDateFormat> formats) {
        for (SimpleDateFormat format : formats) try { return format.parse(src).getTime(); } catch (Exception ignored) {}
        return 0;
    }

    public static boolean isLeanback() {
        return "leanback".equals(BuildConfig.FLAVOR_mode);
    }

    public static boolean isMobile() {
        return "mobile".equals(BuildConfig.FLAVOR_mode);
    }

    public static String format(StringBuilder builder, Formatter formatter, long timeMs) {
        try {
            return androidx.media3.common.util.Util.getStringForTime(builder, formatter, timeMs);
        } catch (Exception e) {
            return "";
        }
    }

    public static Intent getChooser(Intent intent) {
        List<ComponentName> components = new ArrayList<>();
        for (ResolveInfo resolveInfo : App.get().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            String pkgName = resolveInfo.activityInfo.packageName;
            if (pkgName.equals(App.get().getPackageName())) {
                components.add(new ComponentName(pkgName, resolveInfo.activityInfo.name));
            }
        }
        return Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, components.toArray(new ComponentName[0]));
    }
}
