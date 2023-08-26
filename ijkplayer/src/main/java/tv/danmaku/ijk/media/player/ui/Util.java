package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Util {

    public static final String USER_AGENT = "User-Agent";

    public static float dp2px(Context context, float dp) {
        return (dp * context.getResources().getDisplayMetrics().densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }

    public static float sp2px(Context context, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }
}
