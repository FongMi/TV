/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package master.flame.danmaku.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;
import java.util.Locale;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.DrawHandler.Callback;
import master.flame.danmaku.controller.DrawHelper;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.controller.IDanmakuViewController;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.danmaku.util.SystemClock;

public class DanmakuView extends View implements IDanmakuView, IDanmakuViewController {

    private static final int MAX_RECORD_SIZE = 50;
    private static final int ONE_SECOND = 1000;

    private OnDanmakuClickListener mOnDanmakuClickListener;
    private DanmakuTouchHelper mTouchHelper;
    private HandlerThread mHandlerThread;
    private volatile DrawHandler handler;
    private LinkedList<Long> mDrawTimes;
    private Object mDrawMonitor;
    private Callback mCallback;

    private boolean mEnableDrawingCache;
    private boolean isSurfaceCreated;
    private boolean mDanmakuVisible;
    private boolean mRequestRender;
    private boolean mDrawFinished;
    private boolean mClearFlag;
    private boolean mShowFps;
    private int mDrawingThreadType;
    private int mResumeTryCount;
    private long mUiThreadId;
    private float mXOff;
    private float mYOff;

    private final Runnable mResumeRunnable = new Runnable() {
        @Override
        public void run() {
            DrawHandler drawHandler = handler;
            if (drawHandler == null) return;
            if (++mResumeTryCount > 4 || DanmakuView.super.isShown()) {
                drawHandler.resume();
            } else {
                drawHandler.postDelayed(this, 100L * mResumeTryCount);
            }
        }
    };

    public DanmakuView(Context context) {
        super(context);
        init();
    }

    public DanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DanmakuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mDanmakuVisible = true;
        mEnableDrawingCache = true;
        mDrawMonitor = new Object();
        mUiThreadId = Thread.currentThread().getId();
        setBackgroundColor(Color.TRANSPARENT);
        setDrawingCacheBackgroundColor(Color.TRANSPARENT);
        DrawHelper.useDrawColorToClearCanvas(true, false);
        mTouchHelper = DanmakuTouchHelper.instance(this);
        mDrawingThreadType = THREAD_TYPE_NORMAL_PRIORITY;
    }

    public void addDanmaku(BaseDanmaku item) {
        if (handler != null) handler.addDanmaku(item);
    }

    @Override
    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure) {
        if (handler != null) handler.invalidateDanmaku(item, remeasure);
    }

    @Override
    public void removeAllDanmakus(boolean isClearDanmakusOnScreen) {
        if (handler != null) handler.removeAllDanmakus(isClearDanmakusOnScreen);
    }

    @Override
    public void removeAllLiveDanmakus() {
        if (handler != null) handler.removeAllLiveDanmakus();
    }

    @Override
    public IDanmakus getCurrentVisibleDanmakus() {
        if (handler != null) return handler.getCurrentVisibleDanmakus();
        return null;
    }

    public DanmakuView setCallback(Callback callback) {
        if (handler != null) handler.setCallback(callback);
        mCallback = callback;
        return this;
    }

    public void setSpeed(float speed) {
        if (getConfig() == null) return;
        getConfig().setSpeed(speed);
    }

    @Override
    public void release() {
        stop();
        if (mDrawTimes != null) mDrawTimes.clear();
    }

    @Override
    public void stop() {
        stopDraw();
    }

    private synchronized void stopDraw() {
        if (handler == null) return;
        DrawHandler handler = this.handler;
        this.handler = null;
        unlockCanvasAndPost();
        if (handler != null) handler.quit();
        HandlerThread handlerThread = this.mHandlerThread;
        mHandlerThread = null;
        if (handlerThread != null) {
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handlerThread.quit();
        }
    }

    protected synchronized Looper getLooper(int type) {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        int priority;
        switch (type) {
            case THREAD_TYPE_MAIN_THREAD:
                return Looper.getMainLooper();
            case THREAD_TYPE_HIGH_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY;
                break;
            case THREAD_TYPE_LOW_PRIORITY:
                priority = android.os.Process.THREAD_PRIORITY_LOWEST;
                break;
            case THREAD_TYPE_NORMAL_PRIORITY:
            default:
                priority = android.os.Process.THREAD_PRIORITY_DEFAULT;
                break;
        }
        String threadName = "DFM Handler Thread #" + priority;
        mHandlerThread = new HandlerThread(threadName, priority);
        mHandlerThread.start();
        return mHandlerThread.getLooper();
    }

    private void prepare() {
        if (handler == null) handler = new DrawHandler(getLooper(mDrawingThreadType), this, mDanmakuVisible);
    }

    @Override
    public void prepare(BaseDanmakuParser parser, DanmakuContext config) {
        prepare();
        handler.setConfig(config);
        handler.setParser(parser);
        handler.setCallback(mCallback);
        handler.prepare();
    }

    @Override
    public boolean isPrepared() {
        return handler != null && handler.isPrepared();
    }

    @Override
    public DanmakuContext getConfig() {
        if (handler == null) return null;
        return handler.getConfig();
    }

    @Override
    public void showFPS(boolean show) {
        mShowFps = show;
    }

    private float fps() {
        long lastTime = SystemClock.uptimeMillis();
        mDrawTimes.addLast(lastTime);
        Long first = mDrawTimes.peekFirst();
        if (first == null) return 0.0f;
        float dtime = lastTime - first;
        int frames = mDrawTimes.size();
        if (frames > MAX_RECORD_SIZE) mDrawTimes.removeFirst();
        return dtime > 0 ? mDrawTimes.size() * ONE_SECOND / dtime : 0.0f;
    }

    @Override
    public long drawDanmakus() {
        if (!isSurfaceCreated) return 0;
        if (!isShown()) return -1;
        long stime = SystemClock.uptimeMillis();
        lockCanvas();
        return SystemClock.uptimeMillis() - stime;
    }

    private void postInvalidateCompat() {
        mRequestRender = true;
        this.postInvalidateOnAnimation();
    }

    protected void lockCanvas() {
        if (!mDanmakuVisible) return;
        postInvalidateCompat();
        synchronized (mDrawMonitor) {
            while ((!mDrawFinished) && (handler != null)) {
                try {
                    mDrawMonitor.wait(200);
                } catch (InterruptedException e) {
                    if (!mDanmakuVisible || handler == null || handler.isStop()) {
                        break;
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            mDrawFinished = false;
        }
    }

    private void lockCanvasAndClear() {
        mClearFlag = true;
        lockCanvas();
    }

    private void unlockCanvasAndPost() {
        synchronized (mDrawMonitor) {
            mDrawFinished = true;
            mDrawMonitor.notifyAll();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if ((!mDanmakuVisible) && (!mRequestRender)) {
            super.onDraw(canvas);
            return;
        }
        if (mClearFlag) {
            DrawHelper.clearCanvas(canvas);
            mClearFlag = false;
        } else {
            if (handler != null) {
                RenderingState rs = handler.draw(canvas);
                if (mShowFps) {
                    if (mDrawTimes == null) mDrawTimes = new LinkedList<>();
                    String fps = String.format(Locale.getDefault(), "fps %.2f,time:%d s,cache:%d,miss:%d", fps(), getCurrentTime() / 1000, rs.cacheHitCount, rs.cacheMissCount);
                    DrawHelper.drawFPS(canvas, fps);
                }
            }
        }
        mRequestRender = false;
        unlockCanvasAndPost();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (handler != null) handler.notifyDispSizeChanged(right - left, bottom - top);
        isSurfaceCreated = true;
    }

    public void toggle() {
        if (!isSurfaceCreated) return;
        if (handler == null) start();
        else if (handler.isStop()) resume();
        else pause();
    }

    @Override
    public void pause() {
        if (handler != null) {
            handler.removeCallbacks(mResumeRunnable);
            handler.pause();
        }
    }

    @Override
    public void resume() {
        if (handler != null && handler.isPrepared()) {
            mResumeTryCount = 0;
            handler.post(mResumeRunnable);
        } else if (handler == null) {
            restart();
        }
    }

    @Override
    public boolean isPaused() {
        if (handler != null) return handler.isStop();
        return false;
    }

    public void restart() {
        stop();
        start();
    }

    public void start(long position, boolean show) {
        start(position);
        if (show) show();
        else hide();
    }

    @Override
    public void start() {
        start(0);
    }

    @Override
    public void start(long position) {
        Handler handler = this.handler;
        if (handler == null) {
            prepare();
            handler = this.handler;
        } else {
            handler.removeCallbacksAndMessages(null);
        }
        if (handler != null) {
            handler.obtainMessage(DrawHandler.START, position).sendToTarget();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isEventConsumed = mTouchHelper.onTouchEvent(event);
        if (!isEventConsumed) return super.onTouchEvent(event);
        return isEventConsumed;
    }

    public void seekTo(Long ms) {
        if (handler != null) handler.seekTo(ms);
    }

    public void enableDanmakuDrawingCache(boolean enable) {
        mEnableDrawingCache = enable;
    }

    @Override
    public boolean isDanmakuDrawingCacheEnabled() {
        return mEnableDrawingCache;
    }

    @Override
    public boolean isViewReady() {
        return isSurfaceCreated;
    }

    @Override
    public int getViewWidth() {
        return super.getWidth();
    }

    @Override
    public int getViewHeight() {
        return super.getHeight();
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void show() {
        showAndResumeDrawTask(null);
    }

    @Override
    public void showAndResumeDrawTask(Long position) {
        mDanmakuVisible = true;
        mClearFlag = false;
        if (handler == null) return;
        handler.showDanmakus(position);
    }

    @Override
    public void hide() {
        mDanmakuVisible = false;
        if (handler == null) return;
        handler.hideDanmakus(false);
    }

    @Override
    public long hideAndPauseDrawTask() {
        mDanmakuVisible = false;
        if (handler == null) return 0;
        return handler.hideDanmakus(true);
    }

    @Override
    public void clear() {
        if (!isViewReady()) return;
        if (!mDanmakuVisible || Thread.currentThread().getId() == mUiThreadId) {
            mClearFlag = true;
            postInvalidateCompat();
        } else {
            lockCanvasAndClear();
        }
    }

    @Override
    public boolean isShown() {
        return mDanmakuVisible && super.isShown();
    }

    @Override
    public void setDrawingThreadType(int type) {
        mDrawingThreadType = type;
    }

    @Override
    public long getCurrentTime() {
        if (handler != null) return handler.getCurrentTime();
        return 0;
    }

    @Override
    public void clearDanmakusOnScreen() {
        if (handler != null) handler.clearDanmakusOnScreen();
    }

    @Override
    public void setOnDanmakuClickListener(OnDanmakuClickListener listener, float xOff, float yOff) {
        mOnDanmakuClickListener = listener;
        mXOff = xOff;
        mYOff = yOff;
    }

    @Override
    public OnDanmakuClickListener getOnDanmakuClickListener() {
        return mOnDanmakuClickListener;
    }

    @Override
    public void setOnDanmakuClickListener(OnDanmakuClickListener listener) {
        mOnDanmakuClickListener = listener;
    }

    @Override
    public float getXOff() {
        return mXOff;
    }

    @Override
    public float getYOff() {
        return mYOff;
    }

    public void forceRender() {
        mRequestRender = true;
        handler.forceRender();
    }
}
