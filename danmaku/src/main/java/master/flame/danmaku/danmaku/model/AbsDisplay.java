package master.flame.danmaku.danmaku.model;

import android.graphics.Canvas;
import android.graphics.Typeface;

import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;

public abstract class AbsDisplay implements IDisplay {

    public abstract Canvas getExtraData();

    public abstract void setExtraData(Canvas data);

    @Override
    public boolean isHardwareAccelerated() {
        return false;
    }

    public abstract void drawDanmaku(BaseDanmaku danmaku, Canvas canvas, float left, float top, boolean fromWorkerThread);

    public abstract void clearTextHeightCache();

    public abstract void setTypeFace(Typeface font);

    public abstract void setFakeBoldText(boolean bold);

    public abstract void setTransparency(int newTransparency);

    public abstract void setScaleTextSizeFactor(float factor);

    public abstract BaseCacheStuffer getCacheStuffer();

    public abstract void setCacheStuffer(BaseCacheStuffer cacheStuffer);
}
