package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.content.Context;
import android.os.IBinder;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.fongmi.android.tv.App;

import java.util.regex.Pattern;

public class Utils {

    private static final Pattern SNIFFER = Pattern.compile("http((?!http).){20,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)\\?.*|http((?!http).){20,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)|http((?!http).){20,}?\\/m3u8\\?pt=m3u8.*|http((?!http).)*?default\\.ixigua\\.com\\/.*|http((?!http).)*?cdn-tos[^\\?]*|http((?!http).)*?\\/obj\\/tos[^\\?]*|http.*?\\/player\\/m3u8play\\.php\\?url=.*|http.*?\\/player\\/.*?[pP]lay\\.php\\?url=.*|http.*?\\/playlist\\/m3u8\\/\\?vid=.*|http.*?\\.php\\?type=m3u8&.*|http.*?\\/download.aspx\\?.*|http.*?\\/api\\/up_api.php\\?.*|https.*?\\.66yk\\.cn.*|http((?!http).)*?netease\\.com\\/file\\/.*");

    public static void hideSystemUI(Activity activity) {
        int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public static boolean isVideoFormat(String url) {
        if (url.contains("=http") || url.contains("=https") || url.contains("=https%3a%2f") || url.contains("=http%3a%2f")) return false;
        if (SNIFFER.matcher(url).find()) return !url.contains("cdn-tos") || (!url.contains(".js") && !url.contains(".css"));
        return false;
    }

    public static String getBase64(String ext) {
        return Base64.encodeToString(ext.getBytes(), Base64.DEFAULT | Base64.NO_WRAP);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) App.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        IBinder windowToken = view.getWindowToken();
        if (imm != null && windowToken != null) {
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }
}
