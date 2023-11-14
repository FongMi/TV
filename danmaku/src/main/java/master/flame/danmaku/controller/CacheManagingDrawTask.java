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

package master.flame.danmaku.controller;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import master.flame.danmaku.danmaku.model.AbsDisplay;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.ICacheManager;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDrawingCache;
import master.flame.danmaku.danmaku.model.android.CachingPolicy;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.DanmakuContext.DanmakuConfigTag;
import master.flame.danmaku.danmaku.model.android.DanmakuFactory;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.DrawingCache;
import master.flame.danmaku.danmaku.model.android.DrawingCachePoolManager;
import master.flame.danmaku.danmaku.model.objectpool.Pool;
import master.flame.danmaku.danmaku.model.objectpool.Pools;
import master.flame.danmaku.danmaku.renderer.IRenderer.RenderingState;
import master.flame.danmaku.danmaku.util.DanmakuUtils;
import master.flame.danmaku.danmaku.util.SystemClock;

public class CacheManagingDrawTask extends DrawTask {

    private static final int MAX_CACHE_SCREEN_SIZE = 3;
    private final Object mDrawingNotify = new Object();
    private int mMaxCacheSize = 2;
    private CacheManager mCacheManager;
    private DanmakuTimer mCacheTimer;
    private int mRemainingCacheCount;

    public CacheManagingDrawTask(DanmakuTimer timer, DanmakuContext config, TaskListener taskListener) {
        super(timer, config, taskListener);
        mMaxCacheSize = (int) Math.max(1024 * 1024 * 4, Runtime.getRuntime().maxMemory() * config.cachingPolicy.maxCachePoolSizeFactorPercentage);
        mCacheManager = new CacheManager(mMaxCacheSize, MAX_CACHE_SCREEN_SIZE);
        mRenderer.setCacheManager(mCacheManager);
    }

    @Override
    protected void initTimer(DanmakuTimer timer) {
        mTimer = timer;
        mCacheTimer = new DanmakuTimer();
        mCacheTimer.update(timer.currMillisecond);
    }

    @Override
    public void addDanmaku(BaseDanmaku danmaku) {
        super.addDanmaku(danmaku);
        if (mCacheManager == null) return;
        mCacheManager.addDanmaku(danmaku);
    }

    @Override
    public void invalidateDanmaku(BaseDanmaku item, boolean remeasure) {
        super.invalidateDanmaku(item, remeasure);
        if (mCacheManager == null) return;
        mCacheManager.invalidateDanmaku(item);
    }

    @Override
    public void removeAllDanmakus(boolean isClearDanmakusOnScreen) {
        super.removeAllDanmakus(isClearDanmakusOnScreen);
        if (mCacheManager != null) mCacheManager.requestClearAll();
    }

    @Override
    protected void onDanmakuRemoved(BaseDanmaku danmaku) {
        super.onDanmakuRemoved(danmaku);
        if (mCacheManager != null) {
            if (++mRemainingCacheCount > 5) {  // control frequency (it does not require very precise
                mCacheManager.requestClearTimeout();
                mRemainingCacheCount = 0;
            }
        } else {
            IDrawingCache<?> cache = danmaku.getDrawingCache();
            if (cache != null) {
                if (cache.hasReferences()) {
                    cache.decreaseReference();
                } else {
                    cache.destroy();
                }
                danmaku.cache = null;
            }
        }
    }

    @Override
    public RenderingState draw(AbsDisplay display) {
        RenderingState result = super.draw(display);
        synchronized (mDrawingNotify) {
            mDrawingNotify.notify();
        }
        if (result != null && mCacheManager != null) {
            if (result.totalDanmakuCount - result.lastTotalDanmakuCount < -20) {
                mCacheManager.requestClearTimeout();
                mCacheManager.requestBuild(-mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
            }
        }
        return result;
    }

    @Override
    public void seek(long mills) {
        super.seek(mills);
        if (mCacheManager == null) start();
        mCacheManager.seek(mills);
    }

    @Override
    public void start() {
        super.start();
        if (mCacheManager == null) {
            mCacheManager = new CacheManager(mMaxCacheSize, MAX_CACHE_SCREEN_SIZE);
            mCacheManager.begin();
            mRenderer.setCacheManager(mCacheManager);
        } else {
            mCacheManager.resume();
        }
    }

    @Override
    public void quit() {
        super.quit();
        reset();
        mRenderer.setCacheManager(null);
        if (mCacheManager != null) {
            mCacheManager.end();
            mCacheManager = null;
        }
    }

    @Override
    public void prepare() {
        if (mParser == null) return;
        loadDanmakus(mParser);
        mCacheManager.begin();
    }

    @Override
    public void onPlayStateChanged(int state) {
        super.onPlayStateChanged(state);
        if (mCacheManager != null) mCacheManager.onPlayStateChanged(state);

    }

    @Override
    public void requestSync(long fromTimeMills, long toTimeMills, long offsetMills) {
        super.requestSync(fromTimeMills, toTimeMills, offsetMills);
        if (mCacheManager != null) mCacheManager.seek(toTimeMills);
    }

    @Override
    public boolean onDanmakuConfigChanged(DanmakuContext config, DanmakuConfigTag tag, Object... values) {
        if (super.handleOnDanmakuConfigChanged(config, tag, values)) {
            // do nothing
        } else if (DanmakuConfigTag.SCROLL_SPEED_FACTOR.equals(tag)) {
            mDisp.resetSlopPixel(mContext.scaleTextSize);
            requestClear();
        } else if (tag.isVisibilityRelatedTag()) {
            if (values != null && values.length > 0) {
                if (values[0] != null && (!(values[0] instanceof Boolean) || (Boolean) values[0])) {
                    if (mCacheManager != null) {
                        mCacheManager.requestBuild(0L);
                    }
                }
            }
            requestClear();
        } else if (DanmakuConfigTag.TRANSPARENCY.equals(tag) || DanmakuConfigTag.SCALE_TEXTSIZE.equals(tag) || DanmakuConfigTag.DANMAKU_STYLE.equals(tag)) {
            if (DanmakuConfigTag.SCALE_TEXTSIZE.equals(tag)) {
                mDisp.resetSlopPixel(mContext.scaleTextSize);
            }
            if (mCacheManager != null) {
                mCacheManager.requestClearAll();
                mCacheManager.requestBuild(-mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
            }
        } else {
            if (mCacheManager != null) {
                mCacheManager.requestClearUnused();
                mCacheManager.requestBuild(0L);
            }
        }
        if (mTaskListener != null && mCacheManager != null) {
            mCacheManager.post(() -> mTaskListener.onDanmakuConfigChanged());
        }
        return true;
    }

    public class CacheManager implements ICacheManager {

        public HandlerThread mThread;

        Danmakus mCaches = new Danmakus();

        DrawingCachePoolManager mCachePoolManager = new DrawingCachePoolManager();

        Pool<DrawingCache> mCachePool = Pools.finitePool(mCachePoolManager, 800);

        private final int mMaxSize;

        private int mRealSize;

        private int mScreenSize = 3;

        private CacheHandler mHandler;

        private boolean mEndFlag;

        public CacheManager(int maxSize, int screenSize) {
            mEndFlag = false;
            mRealSize = 0;
            mMaxSize = maxSize;
            mScreenSize = screenSize;
        }

        public void seek(long mills) {
            if (mHandler == null) return;
            mHandler.requestCancelCaching();
            mHandler.removeMessages(CacheHandler.BUILD_CACHES);
            mHandler.obtainMessage(CacheHandler.SEEK, mills).sendToTarget();
        }

        @Override
        public void addDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null) {
                if (danmaku.isLive && danmaku.forceBuildCacheInSameThread) {
                    if (!danmaku.isTimeOut()) {
                        mHandler.createCache(danmaku);
                    }
                    return;
                }
                mHandler.obtainMessage(CacheHandler.ADD_DANMAKU, danmaku).sendToTarget();
            }
        }

        @Override
        public void buildDanmakuCache(BaseDanmaku danmaku) {
            if (danmaku == null) {
                return;
            }
            CacheHandler handler = mHandler;
            if (handler != null) {
                handler.addDanmakuAndBuildCache(danmaku);
            }
        }

        public void invalidateDanmaku(BaseDanmaku danmaku) {
            if (mHandler != null) {
                mHandler.requestCancelCaching();
                mHandler.obtainMessage(CacheHandler.REBUILD_CACHE, danmaku).sendToTarget();
                mHandler.sendEmptyMessage(CacheHandler.DISABLE_CANCEL_FLAG);
                requestBuild(0);
            }
        }

        public void begin() {
            mEndFlag = false;
            if (mThread == null) {
                mThread = new HandlerThread("DFM Cache-Building Thread");
                mThread.start();
            }
            if (mHandler == null) {
                mHandler = new CacheHandler(mThread.getLooper());
            }
            mHandler.begin();
        }

        public void end() {
            mEndFlag = true;
            synchronized (mDrawingNotify) {
                mDrawingNotify.notifyAll();
            }
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.pause();
                mHandler = null;
            }
            if (mThread != null) {
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mThread.quit();
                mThread = null;
            }
        }

        public void resume() {
            if (mHandler != null) {
                mHandler.resume();
            } else {
                begin();
            }
        }

        public void onPlayStateChanged(int state) {
            if (mHandler != null) {
                mHandler.onPlayStateChanged(state == IDrawTask.PLAY_STATE_PLAYING);
            }
        }

        public float getPoolPercent() {
            if (mMaxSize == 0) return 0;
            return mRealSize / (float) mMaxSize;
        }

        private void evictAll() {
            mRealSize = 0;
            if (mCaches == null) return;
            mCaches.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    entryRemoved(danmaku);
                    return ACTION_CONTINUE;
                }
            });
            mCaches.clear();
        }

        private void evictAllNotInScreen() {
            if (mCaches == null) return;
            mCaches.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku danmaku) {
                    if (danmaku.isOutside()) {
                        entryRemoved(danmaku);
                        return ACTION_REMOVE;
                    }
                    return ACTION_CONTINUE;
                }
            });
        }

        protected void entryRemoved(BaseDanmaku oldValue) {
            IDrawingCache<?> cache = oldValue.getDrawingCache();
            if (cache == null) return;
            long releasedSize = clearCache(oldValue);
            if (oldValue.isTimeOut()) mContext.getDisplay().getCacheStuffer().releaseResource(oldValue);
            if (releasedSize <= 0) return;
            mRealSize -= releasedSize;
            mCachePool.release((DrawingCache) cache);
        }

        private long clearCache(BaseDanmaku oldValue) {
            IDrawingCache<?> cache = oldValue.cache;
            if (cache == null) return 0;
            if (cache.hasReferences()) {
                cache.decreaseReference();
                oldValue.cache = null;
                return 0;
            }
            long size = sizeOf(oldValue);
            cache.destroy();
            oldValue.cache = null;
            return size;
        }

        protected int sizeOf(BaseDanmaku value) {
            if (value.cache != null && !value.cache.hasReferences()) return value.cache.size();
            return 0;
        }

        private void clearCachePool() {
            DrawingCache item;
            while ((item = mCachePool.acquire()) != null) item.destroy();
        }

        private boolean push(BaseDanmaku item, int itemSize, boolean forcePush) {
            if (itemSize > 0) clearTimeOutAndFilteredCaches(itemSize, forcePush);
            mCaches.addItem(item);
            mRealSize += itemSize;
            return true;
        }

        private void clearTimeOutCaches() {
            mCaches.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku val) {
                    if (val.isTimeOut()) {
                        IDrawingCache<?> cache = val.cache;
                        if (mContext.cachingPolicy.periodOfRecycle == CachingPolicy.CACHE_PERIOD_NOT_RECYCLE && cache != null && !cache.hasReferences()) {
                            if (cache.size() / (float) mMaxCacheSize < mContext.cachingPolicy.forceRecycleThreshold) {
                                return ACTION_CONTINUE;
                            }
                        }
                        entryRemoved(val);
                        return ACTION_REMOVE;
                    } else {
                        return ACTION_BREAK;
                    }
                }
            });
        }

        private BaseDanmaku findReusableCache(final BaseDanmaku refDanmaku, final boolean strictMode, final int maximumTimes) {
            int slopPixel = 0;
            if (!strictMode) {
                slopPixel = mDisp.getSlopPixel() * 2;
            }
            final int finalSlopPixel = slopPixel + mContext.cachingPolicy.reusableOffsetPixel;
            IDanmakus.Consumer<BaseDanmaku, BaseDanmaku> consumer = new IDanmakus.Consumer<BaseDanmaku, BaseDanmaku>() {
                int count = 0;
                BaseDanmaku mResult;

                @Override
                public BaseDanmaku result() {
                    return mResult;
                }

                @Override
                public int accept(BaseDanmaku danmaku) {
                    if (count++ >= maximumTimes) {
                        return ACTION_BREAK;
                    }
                    IDrawingCache<?> cache = danmaku.getDrawingCache();
                    if (cache == null || cache.get() == null) {
                        return ACTION_CONTINUE;
                    }
                    if (danmaku.paintWidth == refDanmaku.paintWidth
                            && danmaku.paintHeight == refDanmaku.paintHeight
                            && danmaku.underlineColor == refDanmaku.underlineColor
                            && danmaku.borderColor == refDanmaku.borderColor
                            && danmaku.textColor == refDanmaku.textColor
                            && danmaku.text.equals(refDanmaku.text)
                            && danmaku.tag == refDanmaku.tag) {
                        mResult = danmaku;
                        return ACTION_BREAK;
                    }
                    if (strictMode) {
                        return ACTION_CONTINUE;
                    }
                    if (!danmaku.isTimeOut()) {
                        return ACTION_BREAK;
                    }
                    if (cache.hasReferences()) {
                        return ACTION_CONTINUE;
                    }
                    float widthGap = cache.width() - refDanmaku.paintWidth;
                    float heightGap = cache.height() - refDanmaku.paintHeight;
                    if (widthGap >= 0 && widthGap <= finalSlopPixel && heightGap >= 0 && heightGap <= finalSlopPixel) {
                        mResult = danmaku;
                        return ACTION_BREAK;
                    }
                    return ACTION_CONTINUE;
                }
            };
            mCaches.forEach(consumer);
            return consumer.result();
        }

        private void clearTimeOutAndFilteredCaches(int expectedFreeSize, final boolean forcePush) {
            final int fexpectedFreeSize = expectedFreeSize;
            mCaches.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                @Override
                public int accept(BaseDanmaku oldValue) {
                    if (mEndFlag) {
                        return IDanmakus.Consumer.ACTION_BREAK;
                    }
                    if (mRealSize + fexpectedFreeSize > mMaxSize) {
                        if (oldValue.isTimeOut() || oldValue.isFiltered()) {
                            entryRemoved(oldValue);
                            return IDanmakus.Consumer.ACTION_REMOVE;
                        } else if (forcePush) {
                            return IDanmakus.Consumer.ACTION_BREAK;
                        }
                    } else {
                        return IDanmakus.Consumer.ACTION_BREAK;
                    }
                    return IDanmakus.Consumer.ACTION_CONTINUE;
                }
            });
        }

        public long getFirstCacheTime() {
            if (mCaches != null && mCaches.size() > 0) {
                BaseDanmaku firstItem = mCaches.first();
                if (firstItem == null)
                    return 0;
                return firstItem.getActualTime();
            }
            return 0;
        }

        public void requestBuild(long correctionTime) {
            if (mHandler != null) {
                mHandler.requestBuildCacheAndDraw(correctionTime);
            }
        }

        public void requestClearAll() {
            if (mHandler == null) return;
            mHandler.removeMessages(CacheHandler.BUILD_CACHES);
            mHandler.removeMessages(CacheHandler.DISABLE_CANCEL_FLAG);
            mHandler.requestCancelCaching();
            mHandler.removeMessages(CacheHandler.CLEAR_ALL_CACHES);
            mHandler.sendEmptyMessage(CacheHandler.CLEAR_ALL_CACHES);
        }

        public void requestClearUnused() {
            if (mHandler == null) return;
            mHandler.removeMessages(CacheHandler.CLEAR_OUTSIDE_CACHES_AND_RESET);
            mHandler.sendEmptyMessage(CacheHandler.CLEAR_OUTSIDE_CACHES_AND_RESET);
        }

        public void requestClearTimeout() {
            if (mHandler == null) return;
            mHandler.removeMessages(CacheHandler.CLEAR_TIMEOUT_CACHES);
            mHandler.sendEmptyMessage(CacheHandler.CLEAR_TIMEOUT_CACHES);
        }

        public void post(Runnable runnable) {
            if (mHandler == null) return;
            mHandler.post(runnable);
        }

        public class CacheHandler extends Handler {

            public static final int ADD_DANMAKU = 0x2;
            public static final int BUILD_CACHES = 0x3;
            public static final int CLEAR_TIMEOUT_CACHES = 0x4;
            public static final int SEEK = 0x5;
            public static final int QUIT = 0x6;
            public static final int CLEAR_ALL_CACHES = 0x7;
            public static final int CLEAR_OUTSIDE_CACHES = 0x8;
            public static final int CLEAR_OUTSIDE_CACHES_AND_RESET = 0x9;
            public static final int DISPATCH_ACTIONS = 0x10;
            public static final int REBUILD_CACHE = 0x11;
            public static final int DISABLE_CANCEL_FLAG = 0x12;
            private static final int PREPARE = 0x1;
            private boolean mPause;

            private boolean mIsPlayerPause;

            private boolean mSeekedFlag;

            private boolean mCancelFlag;

            public CacheHandler(android.os.Looper looper) {
                super(looper);
            }

            public void requestCancelCaching() {
                mCancelFlag = true;
            }

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                switch (what) {
                    case PREPARE:
                        evictAllNotInScreen();
                        for (int i = 0; i < 300; i++) {
                            mCachePool.release(new DrawingCache());
                        }
                    case DISPATCH_ACTIONS:
                        long delayed = dispatchAction();
                        if (delayed <= 0) {
                            delayed = mContext.mDanmakuFactory.MAX_DANMAKU_DURATION / 2;
                        }
                        sendEmptyMessageDelayed(DISPATCH_ACTIONS, delayed);
                        break;
                    case BUILD_CACHES:
                        removeMessages(BUILD_CACHES);
                        boolean repositioned = ((mTaskListener != null && !mReadyState) || mSeekedFlag);
                        prepareCaches(repositioned);
                        if (repositioned)
                            mSeekedFlag = false;
                        if (mTaskListener != null && !mReadyState) {
                            mTaskListener.ready();
                            mReadyState = true;
                        }
                        break;
                    case ADD_DANMAKU:
                        BaseDanmaku item = (BaseDanmaku) msg.obj;
                        addDanmakuAndBuildCache(item);
                        break;
                    case REBUILD_CACHE:
                        BaseDanmaku cacheitem = (BaseDanmaku) msg.obj;
                        if (cacheitem != null) {
                            IDrawingCache<?> cache = cacheitem.getDrawingCache();
                            boolean requestRemeasure = 0 != (cacheitem.requestFlags & BaseDanmaku.FLAG_REQUEST_REMEASURE);
                            if (!requestRemeasure && cache != null && cache.get() != null && !cache.hasReferences()) {
                                cache = DanmakuUtils.buildDanmakuDrawingCache(cacheitem, mDisp, (DrawingCache) cacheitem.cache, mContext.cachingPolicy.bitsPerPixelOfCache);
                                cacheitem.cache = cache;
                                push(cacheitem, 0, true);
                                return;
                            }
                            if (cacheitem.isLive) {
                                clearCache(cacheitem);
                                createCache(cacheitem);
                            } else {
                                if (cache != null && cache.hasReferences()) {
                                    cache.destroy();
                                }
                                entryRemoved(cacheitem);
                                addDanmakuAndBuildCache(cacheitem);
                            }
                        }
                        break;
                    case CLEAR_TIMEOUT_CACHES:
                        clearTimeOutCaches();
                        break;
                    case SEEK:
                        Long seekMills = (Long) msg.obj;
                        if (seekMills != null) {
                            long seekCacheTime = seekMills;
                            long oldCacheTime = mCacheTimer.currMillisecond;
                            mCacheTimer.update(seekCacheTime);
                            mSeekedFlag = true;
                            long firstCacheTime = getFirstCacheTime();
                            if (seekCacheTime > oldCacheTime || firstCacheTime - seekCacheTime > mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                                evictAllNotInScreen();
                            } else {
                                clearTimeOutCaches();
                            }
                            prepareCaches(true);
                            resume();
                        }
                        break;
                    case QUIT:
                        removeCallbacksAndMessages(null);
                        mPause = true;
                        evictAll();
                        clearCachePool();
                        this.getLooper().quit();
                        break;
                    case CLEAR_ALL_CACHES:
                        evictAll();
                        mCacheTimer.update(mTimer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
                        mSeekedFlag = true;
                        break;
                    case CLEAR_OUTSIDE_CACHES:
                        evictAllNotInScreen();
                        mCacheTimer.update(mTimer.currMillisecond);
                        break;
                    case CLEAR_OUTSIDE_CACHES_AND_RESET:
                        evictAllNotInScreen();
                        mCacheTimer.update(mTimer.currMillisecond);
                        requestClear();
                        break;
                    case DISABLE_CANCEL_FLAG:
                        mCancelFlag = false;
                        break;
                }
            }

            private long dispatchAction() {
                if (mCacheTimer.currMillisecond <= mTimer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                    if (mContext.cachingPolicy.periodOfRecycle != CachingPolicy.CACHE_PERIOD_NOT_RECYCLE) {
                        evictAllNotInScreen();
                    }
                    mCacheTimer.update(mTimer.currMillisecond);
                    sendEmptyMessage(BUILD_CACHES);
                    return 0;
                }
                float level = getPoolPercent();
                BaseDanmaku firstCache = mCaches.first();
                long gapTime = firstCache != null ? firstCache.getActualTime() - mTimer.currMillisecond : 0;
                long doubleScreenDuration = mContext.mDanmakuFactory.MAX_DANMAKU_DURATION * 2;
                if (level < 0.6f && gapTime > mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                    mCacheTimer.update(mTimer.currMillisecond);
                    removeMessages(BUILD_CACHES);
                    sendEmptyMessage(BUILD_CACHES);
                    return 0;
                } else if (level > 0.4f && gapTime < -doubleScreenDuration) {
                    removeMessages(CLEAR_TIMEOUT_CACHES);
                    sendEmptyMessage(CLEAR_TIMEOUT_CACHES);
                    return 0;
                }
                if (level >= 0.9f) {
                    return 0;
                }
                // check cache time
                long deltaTime = mCacheTimer.currMillisecond - mTimer.currMillisecond;
                if (firstCache != null && firstCache.isTimeOut() && deltaTime < -mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                    mCacheTimer.update(mTimer.currMillisecond);
                    sendEmptyMessage(CLEAR_OUTSIDE_CACHES);
                    sendEmptyMessage(BUILD_CACHES);
                    return 0;
                } else if (deltaTime > doubleScreenDuration) {
                    return 0;
                }
                removeMessages(BUILD_CACHES);
                sendEmptyMessage(BUILD_CACHES);
                return 0;
            }

            private void releaseDanmakuCache(BaseDanmaku item, DrawingCache cache) {
                if (cache == null) {
                    cache = (DrawingCache) item.cache;
                }
                item.cache = null;
                if (cache == null) {
                    return;
                }
                cache.destroy();
                mCachePool.release(cache);
            }

            private void preMeasure() {
                // pre measure
                IDanmakus danmakus = null;
                try {
                    long begin = mTimer.currMillisecond;
                    long end = begin + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION * 2;
                    danmakus = danmakuList.subnew(begin - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION, end);
                } catch (Exception ignored) {

                }
                if (danmakus == null || danmakus.isEmpty()) {
                    return;
                }
                danmakus.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                    @Override
                    public int accept(BaseDanmaku item) {
                        if (mPause || mCancelFlag) {
                            return ACTION_BREAK;
                        }
                        if (!item.hasPassedFilter()) {
                            mContext.mDanmakuFilters.filter(item, 0, 0, null, true, mContext);
                        }
                        if (item.isFiltered()) {
                            return ACTION_CONTINUE;
                        }
                        if (!item.isMeasured()) {
                            item.measure(mDisp, true);
                        }
                        if (!item.isPrepared()) {
                            item.prepare(mDisp, true);
                        }
                        return ACTION_CONTINUE;
                    }
                });
            }

            private void prepareCaches(final boolean repositioned) {
                preMeasure();
                final long curr = mCacheTimer.currMillisecond - 30;
                final long end = curr + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize;
                if (end < mTimer.currMillisecond) {
                    return;
                }
                final long startTime = SystemClock.uptimeMillis();
                IDanmakus danmakus = null;
                int tryCount = 0;
                boolean hasException = false;
                do {
                    try {
                        danmakus = danmakuList.subnew(curr, end);
                    } catch (Exception e) {
                        hasException = true;
                        SystemClock.sleep(10);
                    }
                } while (++tryCount < 3 && danmakus == null && hasException);
                if (danmakus == null) {
                    mCacheTimer.update(end);
                    return;
                }
                final BaseDanmaku first = danmakus.first();
                final BaseDanmaku last = danmakus.last();
                if (first == null || last == null) {
                    mCacheTimer.update(end);
                    return;
                }
                long deltaTime = first.getActualTime() - mTimer.currMillisecond;
                long sleepTime = (deltaTime < 0 ? 30 : 30 + 10 * deltaTime / mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
                sleepTime = Math.min(100, sleepTime);
                if (repositioned) {
                    sleepTime = 0;
                }
                final long finalSleepTime = sleepTime;
                final int sizeInScreen = danmakus.size();
                danmakus.forEach(new IDanmakus.DefaultConsumer<BaseDanmaku>() {
                    int orderInScreen = 0;
                    int currScreenIndex = 0;

                    @Override
                    public int accept(BaseDanmaku item) {
                        if (mPause || mCancelFlag) {
                            return ACTION_BREAK;
                        }
                        if (last.getActualTime() < mTimer.currMillisecond) {
                            return ACTION_BREAK;
                        }

                        IDrawingCache<?> cache = item.getDrawingCache();
                        if (cache != null && cache.get() != null) {
                            return ACTION_CONTINUE;
                        }

                        if (!repositioned && (item.isTimeOut() || !item.isOutside())) {
                            return ACTION_CONTINUE;
                        }

                        if (!item.hasPassedFilter()) {
                            mContext.mDanmakuFilters.filter(item, orderInScreen, sizeInScreen, null, true, mContext);
                        }

                        if (item.priority == 0 && item.isFiltered()) {
                            return ACTION_CONTINUE;
                        }

                        if (item.getType() == BaseDanmaku.TYPE_SCROLL_RL) {
                            int screenIndex = (int) ((item.getActualTime() - curr) / mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
                            if (currScreenIndex == screenIndex)
                                orderInScreen++;
                            else {
                                orderInScreen = 0;
                                currScreenIndex = screenIndex;
                            }
                        }

                        if (!repositioned && !mIsPlayerPause) {
                            try {
                                synchronized (mDrawingNotify) {
                                    mDrawingNotify.wait(finalSleepTime);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return ACTION_BREAK;
                            }
                        }

                        cache = item.getDrawingCache();
                        if (cache == null || cache.get() == null) {
                            buildCache(item, false);
                        }

                        if (!repositioned) {
                            long consumingTime = SystemClock.uptimeMillis() - startTime;
                            if (consumingTime >= DanmakuFactory.COMMON_DANMAKU_DURATION * mScreenSize) {
                                return ACTION_BREAK;
                            }
                        }
                        return ACTION_CONTINUE;
                    }
                });
                mCacheTimer.update(end);
            }

            public void createCache(BaseDanmaku item) {
                if (!item.isMeasured()) {
                    item.measure(mDisp, true);
                }
                DrawingCache cache = null;
                try {
                    cache = mCachePool.acquire();
                    cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache, mContext.cachingPolicy.bitsPerPixelOfCache);
                    item.cache = cache;
                } catch (OutOfMemoryError | Exception e) {
                    if (cache != null) mCachePool.release(cache);
                    item.cache = null;
                }
            }

            private void buildCache(BaseDanmaku item, boolean forceInsert) {
                if (!item.isMeasured()) item.measure(mDisp, true);
                DrawingCache cache = null;
                try {
                    BaseDanmaku danmaku = findReusableCache(item, true, mContext.cachingPolicy.maxTimesOfStrictReusableFinds);
                    if (danmaku != null) {
                        cache = (DrawingCache) danmaku.cache;
                    }
                    if (cache != null) {
                        cache.increaseReference();
                        item.cache = cache;
                        mCacheManager.push(item, 0, forceInsert);
                        return;
                    }
                    danmaku = findReusableCache(item, false, mContext.cachingPolicy.maxTimesOfReusableFinds);
                    if (danmaku != null) {
                        cache = (DrawingCache) danmaku.cache;
                    }
                    if (cache != null) {
                        danmaku.cache = null;
                        cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache, mContext.cachingPolicy.bitsPerPixelOfCache);  //redraw
                        item.cache = cache;
                        mCacheManager.push(item, 0, forceInsert);
                        return;
                    }
                    int cacheSize = DanmakuUtils.getCacheSize((int) item.paintWidth, (int) item.paintHeight, mContext.cachingPolicy.bitsPerPixelOfCache / 8);
                    if (cacheSize * 2 > mMaxCacheSize) {
                        return;
                    }
                    if (!forceInsert && (mRealSize + cacheSize > mMaxSize)) {
                        mCacheManager.clearTimeOutAndFilteredCaches(cacheSize, false);
                        return;
                    }
                    cache = mCachePool.acquire();
                    cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache, mContext.cachingPolicy.bitsPerPixelOfCache);
                    item.cache = cache;
                    boolean pushed = mCacheManager.push(item, sizeOf(item), forceInsert);
                    if (!pushed) {
                        releaseDanmakuCache(item, cache);
                    }
                } catch (OutOfMemoryError | Exception e) {
                    releaseDanmakuCache(item, cache);
                }
            }

            private void addDanmakuAndBuildCache(BaseDanmaku danmaku) {
                if (danmaku.isTimeOut() || (danmaku.getActualTime() > mCacheTimer.currMillisecond + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION && !danmaku.isLive)) {
                    return;
                }
                if (danmaku.priority == 0 && danmaku.isFiltered()) {
                    return;
                }
                IDrawingCache<?> cache = danmaku.getDrawingCache();
                if (cache == null || cache.get() == null) {
                    cache = danmaku.getDrawingCache();
                    if (cache == null || cache.get() == null) {
                        buildCache(danmaku, true);
                    }
                }
            }

            public void begin() {
                sendEmptyMessage(PREPARE);
                sendEmptyMessageDelayed(CLEAR_TIMEOUT_CACHES, mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
            }

            public void pause() {
                mPause = true;
                sendEmptyMessage(QUIT);
            }

            public void resume() {
                sendEmptyMessage(DISABLE_CANCEL_FLAG);
                mPause = false;
                removeMessages(DISPATCH_ACTIONS);
                sendEmptyMessage(DISPATCH_ACTIONS);
                sendEmptyMessageDelayed(CLEAR_TIMEOUT_CACHES, mContext.mDanmakuFactory.MAX_DANMAKU_DURATION);
            }

            public boolean isPause() {
                return mPause;
            }

            public void requestBuildCacheAndDraw(long correctionTime) {
                removeMessages(CacheHandler.BUILD_CACHES);
                mSeekedFlag = true;
                sendEmptyMessage(DISABLE_CANCEL_FLAG);
                mCacheTimer.update(mTimer.currMillisecond + correctionTime);
                sendEmptyMessage(CacheHandler.BUILD_CACHES);
            }

            public void onPlayStateChanged(boolean isPlaying) {
                mIsPlayerPause = !isPlaying;
            }
        }
    }
}