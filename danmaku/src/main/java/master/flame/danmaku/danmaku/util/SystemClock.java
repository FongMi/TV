package master.flame.danmaku.danmaku.util;

/**
 * Created by ch on 15-12-9.
 */
public class SystemClock {

    public static long uptimeMillis() {
        return CustomClock.getInstance().elapsedRealtime();
    }

    public static void sleep(long mills) {
        android.os.SystemClock.sleep(mills);
    }
}
