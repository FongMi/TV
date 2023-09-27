package master.flame.danmaku.danmaku.util;

import android.app.Activity;
import android.content.Context;
import android.view.Display;

/**
 * Created by xyoye on 2021/3/31.
 */

public class FrameHelper {

    private static final int DEFAULT_FRAME_CONSUMING_TIME = 16;

    public static int getFrameConsumingTime(Context context) {
        try {
            Activity activity = (Activity) context;
            Display display = activity.getWindowManager().getDefaultDisplay();
            float refreshRate = display.getRefreshRate();
            int rate = (int) (1000 / refreshRate);
            rate = Math.max(1, rate);
            return Math.min(DEFAULT_FRAME_CONSUMING_TIME, rate);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return DEFAULT_FRAME_CONSUMING_TIME;
    }
}
