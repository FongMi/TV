package master.flame.danmaku.danmaku.model;

public class Duration implements Cloneable {

    public long value;
    private long mInitialDuration;
    private float factor = 1.0f;

    public Duration(long initialDuration) {
        mInitialDuration = initialDuration;
        value = initialDuration;
    }

    public void setValue(long initialDuration) {
        mInitialDuration = initialDuration;
        value = (long) (mInitialDuration * factor);
    }

    public void setFactor(float f) {
        if (factor != f) {
            factor = f;
            value = (long) (mInitialDuration * f);
        }
    }
}
