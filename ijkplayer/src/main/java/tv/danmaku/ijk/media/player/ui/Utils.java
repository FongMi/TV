package tv.danmaku.ijk.media.player.ui;

import android.content.Context;
import android.util.DisplayMetrics;

public class Utils {

    public static final String USER_AGENT = "User-Agent";

    public static float dp2px(Context context, float dpValue) {
        return (dpValue * context.getResources().getDisplayMetrics().densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
    }
}
