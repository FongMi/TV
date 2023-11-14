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

package master.flame.danmaku.danmaku.util;

import android.text.TextUtils;

import master.flame.danmaku.danmaku.model.AbsDisplay;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplay;
import master.flame.danmaku.danmaku.model.android.DrawingCache;
import master.flame.danmaku.danmaku.model.android.DrawingCacheHolder;

public class DanmakuUtils {

    public static boolean willHitInDuration(IDisplay disp, BaseDanmaku d1, BaseDanmaku d2, long duration, long currTime) {
        final int type1 = d1.getType();
        final int type2 = d2.getType();
        if (type1 != type2) return false;
        if (d1.isOutside()) return false;
        long dTime = d2.getActualTime() - d1.getActualTime();
        if (dTime <= 0) return true;
        if (Math.abs(dTime) >= duration || d1.isTimeOut() || d2.isTimeOut()) return false;
        if (type1 == BaseDanmaku.TYPE_FIX_TOP || type1 == BaseDanmaku.TYPE_FIX_BOTTOM) return true;
        return checkHitAtTime(disp, d1, d2, currTime) || checkHitAtTime(disp, d1, d2, d1.getActualTime() + d1.getDuration());
    }

    private static boolean checkHitAtTime(IDisplay disp, BaseDanmaku d1, BaseDanmaku d2, long time) {
        final float[] rectArr1 = d1.getRectAtTime(disp, time);
        final float[] rectArr2 = d2.getRectAtTime(disp, time);
        if (rectArr1 == null || rectArr2 == null) return false;
        return checkHit(d1.getType(), d2.getType(), rectArr1, rectArr2);
    }

    private static boolean checkHit(int type1, int type2, float[] rectArr1, float[] rectArr2) {
        if (type1 != type2) return false;
        if (type1 == BaseDanmaku.TYPE_SCROLL_RL) return rectArr2[0] < rectArr1[2];
        if (type1 == BaseDanmaku.TYPE_SCROLL_LR) return rectArr2[2] > rectArr1[0];
        return false;
    }

    public static DrawingCache buildDanmakuDrawingCache(BaseDanmaku danmaku, IDisplay disp, DrawingCache cache, int bitsPerPixel) {
        if (cache == null) cache = new DrawingCache();
        cache.build((int) Math.ceil(danmaku.paintWidth), (int) Math.ceil(danmaku.paintHeight), disp.getDensityDpi(), false, bitsPerPixel);
        DrawingCacheHolder holder = cache.get();
        if (holder != null) {
            ((AbsDisplay) disp).drawDanmaku(danmaku, holder.canvas, 0, 0, true);
            if (disp.isHardwareAccelerated()) {
                holder.splitWith(disp.getWidth(), disp.getHeight(), disp.getMaximumCacheWidth(), disp.getMaximumCacheHeight());
            }
        }
        return cache;
    }

    public static int getCacheSize(int w, int h, int bytesPerPixel) {
        return (w) * (h) * bytesPerPixel;
    }

    public static boolean isDuplicate(BaseDanmaku obj1, BaseDanmaku obj2) {
        if (obj1 == obj2) return false;
        if (obj1.text == obj2.text) return true;
        return obj1.text != null && obj1.text.equals(obj2.text);
    }

    public static int compare(BaseDanmaku obj1, BaseDanmaku obj2) {
        if (obj1 == obj2) return 0;
        if (obj1 == null) return -1;
        if (obj2 == null) return 1;
        long val = obj1.getTime() - obj2.getTime();
        if (val > 0) return 1;
        else if (val < 0) return -1;
        int r = obj1.index - obj2.index;
        if (r != 0) return r < 0 ? -1 : 1;
        r = obj1.hashCode() - obj2.hashCode();
        return r;
    }

    public static boolean isOverSize(IDisplay disp, BaseDanmaku item) {
        return disp.isHardwareAccelerated() && (item.paintWidth > disp.getMaximumCacheWidth() || item.paintHeight > disp.getMaximumCacheHeight());
    }

    public static void fillText(BaseDanmaku danmaku, String text) {
        danmaku.setText(text.trim());
        if (TextUtils.isEmpty(text) || !text.contains(BaseDanmaku.DANMAKU_BR_CHAR)) return;
        String[] lines = danmaku.getText().toString().split(BaseDanmaku.DANMAKU_BR_CHAR, -1);
        if (lines.length > 1) danmaku.setLines(lines);
    }
}
