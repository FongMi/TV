package tv.cjump.jni;

import android.graphics.Bitmap;
import android.os.Build;

public class BitmapFactory {

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        return createBitmap(width, height, config, config.equals(Bitmap.Config.ARGB_4444) || config.equals(Bitmap.Config.ARGB_8888));
    }

    public static synchronized Bitmap createBitmap(int width, int height, Bitmap.Config config, boolean hasAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Bitmap.createBitmap(width, height, config, hasAlpha);
        } else {
            return Bitmap.createBitmap(width, height, config);
        }
    }
}
