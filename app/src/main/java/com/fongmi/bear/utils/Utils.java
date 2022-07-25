package com.fongmi.bear.utils;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Rational;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.fongmi.bear.App;
import com.fongmi.bear.R;
import com.google.android.exoplayer2.util.Util;

import java.util.regex.Pattern;

public class Utils {

    private static final Pattern snifferMatch = Pattern.compile("http((?!http).){26,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)\\?.*|http((?!http).){26,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)|http((?!http).){26,}?/m3u8\\?pt=m3u8.*|http((?!http).)*?default\\.ixigua\\.com/.*|http((?!http).)*?cdn-tos[^\\?]*|http((?!http).)*?/obj/tos[^\\?]*|http.*?/player/m3u8play\\.php\\?url=.*|http.*?/player/.*?[pP]lay\\.php\\?url=.*|http.*?/playlist/m3u8/\\?vid=.*|http.*?\\.php\\?type=m3u8&.*|http.*?/download.aspx\\?.*|http.*?/api/up_api.php\\?.*|https.*?\\.66yk\\.cn.*|http((?!http).)*?netease\\.com/file/.*");

    public static <T> void loadImage(T model, ImageView view) {
        Glide.with(App.get()).load(model).placeholder(R.drawable.ic_img_loading).error(R.drawable.ic_img_error).into(view);
    }

    public static boolean hasPIP() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && App.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    public static void enterPIP(Activity activity) {
        try {
            if (!hasPIP() || activity.isInPictureInPictureMode()) return;
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            builder.setAspectRatio(new Rational(16, 9)).build();
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getUUID() {
        return Settings.Secure.getString(App.get().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getUserAgent() {
        return Util.getUserAgent(App.get(), App.get().getPackageName().concat(".").concat(getUUID()));
    }

    public static boolean isVideoFormat(String url) {
        if (url.contains("=http") || url.contains("=https") || url.contains("=https%3a%2f") || url.contains("=http%3a%2f")) return false;
        if (snifferMatch.matcher(url).find()) return !url.contains("cdn-tos") || (!url.contains(".js") && !url.contains(".css"));
        return false;
    }
}
