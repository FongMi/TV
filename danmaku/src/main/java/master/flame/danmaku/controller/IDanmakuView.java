package master.flame.danmaku.controller;

import android.view.View;

import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public interface IDanmakuView {

    int THREAD_TYPE_NORMAL_PRIORITY = 0x0;
    int THREAD_TYPE_MAIN_THREAD = 0x1;
    int THREAD_TYPE_HIGH_PRIORITY = 0x2;
    int THREAD_TYPE_LOW_PRIORITY = 0x3;

    boolean isPrepared();

    boolean isPaused();

    boolean isHardwareAccelerated();

    /**
     * @param type One of THREAD_TYPE_MAIN_THREAD, THREAD_TYPE_HIGH_PRIORITY, THREAD_TYPE_NORMAL_PRIORITY, or THREAD_TYPE_LOW_PRIORITY.
     */
    void setDrawingThreadType(int type);

    void enableDanmakuDrawingCache(boolean enable);

    boolean isDanmakuDrawingCacheEnabled();

    void showFPS(boolean show);

    /**
     * danmaku.isLive == true的情况下,请在非UI线程中使用此方法,避免可能卡住主线程
     *
     * @param item
     */
    void addDanmaku(BaseDanmaku item);

    void invalidateDanmaku(BaseDanmaku item, boolean remeasure);

    void removeAllDanmakus(boolean isClearDanmakusOnScreen);

    void removeAllLiveDanmakus();

    IDanmakus getCurrentVisibleDanmakus();

    IDanmakuView setCallback(Callback callback);

    /**
     * for getting the accurate play-time. use this method intead of parser.getTimer().currMillisecond
     *
     * @return
     */
    long getCurrentTime();

    DanmakuContext getConfig();

    // ------------- Android View方法  --------------------

    View getView();

    int getWidth();

    int getHeight();

    void setVisibility(int visibility);

    boolean isShown();


    // ------------- 播放控制 -------------------

    void prepare(BaseDanmakuParser parser, DanmakuContext config);

    void seekTo(Long ms);

    void start();

    void start(long postion);

    void stop();

    void pause();

    void resume();

    void release();

    void toggle();

    void show();

    void hide();

    /**
     * show the danmakuview again if you called hideAndPauseDrawTask()
     *
     * @param position The position you want to resume
     * @see #hideAndPauseDrawTask
     */
    void showAndResumeDrawTask(Long position);

    /**
     * hide the danmakuview and pause the drawtask
     *
     * @return the paused position
     * @see #showAndResumeDrawTask
     */
    long hideAndPauseDrawTask();

    void clearDanmakusOnScreen();

    void setOnDanmakuClickListener(OnDanmakuClickListener listener, float xOff, float yOff);

    OnDanmakuClickListener getOnDanmakuClickListener();

    void setOnDanmakuClickListener(OnDanmakuClickListener listener);

    float getXOff();

    float getYOff();

    void forceRender();

    // ------------- Click Listener -------------------
    interface OnDanmakuClickListener {
        /**
         * @param danmakus all to be clicked, this value may be empty;
         *                 danmakus.last() is the latest danmaku which may be null;
         * @return True if the event was handled, false otherwise.
         */
        boolean onDanmakuClick(IDanmakus danmakus);

        boolean onDanmakuLongClick(IDanmakus danmakus);

        boolean onViewClick(IDanmakuView view);
    }
}
