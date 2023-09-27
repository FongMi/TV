package master.flame.danmaku.controller;

import android.content.Context;

/**
 * For internal control. DO NOT ACCESS this interface.
 */
public interface IDanmakuViewController {

    boolean isViewReady();

    int getViewWidth();

    int getViewHeight();

    Context getContext();

    long drawDanmakus();

    void clear();

    boolean isHardwareAccelerated();

    boolean isDanmakuDrawingCacheEnabled();
}
