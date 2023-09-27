package master.flame.danmaku.danmaku.util;

import android.os.SystemClock;

public class CustomClock {

    private final long baseTime;
    private float speed;
    private long speedBaseTime;
    private long timeOffset;

    private CustomClock() {
        this.speed = 1.0F;
        this.timeOffset = 0L;
        this.baseTime = SystemClock.elapsedRealtime();
        this.speedBaseTime = SystemClock.elapsedRealtime();
    }

    public static CustomClock getInstance() {
        return CustomClock.Holder.instance;
    }

    public void setSpeed(float speed) {
        if (speed <= 0.0F) speed = 1.0F;
        this.elapsedRealtime();
        this.speed = speed;
        this.speedBaseTime = SystemClock.elapsedRealtime();
    }

    public long elapsedRealtime() {
        long currentTime = SystemClock.elapsedRealtime();
        long realtime = currentTime - this.speedBaseTime;
        long newExtraTime = (long) ((float) realtime * this.speed);
        this.speedBaseTime = currentTime;
        this.timeOffset += newExtraTime;
        return this.baseTime + this.timeOffset;
    }

    public CustomClock init() {
        this.setSpeed(1.0F);
        return this;
    }

    private static class Holder {

        static CustomClock instance = new CustomClock();

        private Holder() {
        }
    }
}
