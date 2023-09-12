package tv.cjump.jni;

import android.graphics.Bitmap;

public class NativeBitmapFactory {

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        return createBitmap(width, height, config, config.equals(Bitmap.Config.ARGB_4444) || config.equals(Bitmap.Config.ARGB_8888));
    }

    public static synchronized Bitmap createBitmap(int width, int height, Bitmap.Config config, boolean hasAlpha) {
        return Bitmap.createBitmap(width, height, config, hasAlpha);
    }
}
