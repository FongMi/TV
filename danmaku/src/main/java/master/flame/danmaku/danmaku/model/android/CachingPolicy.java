package master.flame.danmaku.danmaku.model.android;

import android.os.Build;

/**
 * Created by ch on 17/4/28. <br/>
 * The cacing policy apply to {@link master.flame.danmaku.controller.CacheManagingDrawTask}
 * 提供缓存相关的策略设置: <br/>
 * 1.缓存格式  ARGB_4444  ARGB_8888 <br/>
 * 2.缓存池总容量大小百分比系数(0.0~1.0) <br/>
 * 3.过期缓存回收频率 <br/>
 * 4.缓存回收条件内存占比阈值 <br/>
 * 5.可复用缓存尺寸调节
 */
public class CachingPolicy {

    public final static int BMP_BPP_ARGB_4444 = 16;
    public final static int BMP_BPP_ARGB_8888 = 32;
    public final static int CACHE_PERIOD_AUTO = 0;
    public final static int CACHE_PERIOD_NOT_RECYCLE = -1;

    public final static CachingPolicy POLICY_LAZY = new CachingPolicy(BMP_BPP_ARGB_4444, 0.3f, CACHE_PERIOD_AUTO, 50, 0.01f);
    public final static CachingPolicy POLICY_DEFAULT = POLICY_LAZY;

    public int bitsPerPixelOfCache;
    public float maxCachePoolSizeFactorPercentage;
    public long periodOfRecycle;

    public float forceRecycleThreshold;
    public int reusableOffsetPixel;
    public int maxTimesOfStrictReusableFinds = 20;
    public int maxTimesOfReusableFinds = 150;

    public CachingPolicy(int bitsPerPixelOfCache, float maxCachePoolSizeFactorPercentage, long periodOfRecycle, int reusableOffsetPixel, float forceRecyleThreshold) {
        this.bitsPerPixelOfCache = Build.VERSION.SDK_INT >= 19 ? BMP_BPP_ARGB_8888 : bitsPerPixelOfCache;
        this.maxCachePoolSizeFactorPercentage = maxCachePoolSizeFactorPercentage;
        this.periodOfRecycle = periodOfRecycle;
        this.reusableOffsetPixel = reusableOffsetPixel;
        this.forceRecycleThreshold = forceRecyleThreshold;
    }
}
