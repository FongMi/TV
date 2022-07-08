package com.fongmi.bear.utils;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Rational;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.fongmi.bear.App;
import com.fongmi.bear.R;
import com.google.android.exoplayer2.util.Util;

public class Utils {

    public static boolean hasEvent(KeyEvent event) {
        return isArrowKey(event) || isBackKey(event) || isMenuKey(event) || isDigitKey(event) || event.isLongPress();
    }

    private static boolean isArrowKey(KeyEvent event) {
        return isEnterKey(event) || isUpKey(event) || isDownKey(event) || isLeftKey(event) || isRightKey(event);
    }

    static boolean isBackKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_BACK;
    }

    static boolean isMenuKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_MENU;
    }

    public static boolean isDigitKey(KeyEvent event) {
        return event.getKeyCode() >= KeyEvent.KEYCODE_0 && event.getKeyCode() <= KeyEvent.KEYCODE_9 || event.getKeyCode() >= KeyEvent.KEYCODE_NUMPAD_0 && event.getKeyCode() <= KeyEvent.KEYCODE_NUMPAD_9;
    }

    static boolean isEnterKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_SPACE || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    static boolean isUpKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_UP || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }

    static boolean isDownKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT;
    }

    static boolean isLeftKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT;
    }

    static boolean isRightKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    public static void loadImage(String url, ImageView view) {
        Glide.with(App.get()).load(url).placeholder(R.drawable.ic_img_loading).error(R.drawable.ic_img_error).into(view);
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
}
