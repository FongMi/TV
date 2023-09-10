package com.fongmi.android.tv.player.danmu;

import android.graphics.Color;

import com.fongmi.android.tv.bean.Danmu;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;

public class Parser extends BaseDanmakuParser {

    private final Danmu danmu;

    public Parser(Danmu danmu) {
        this.danmu = danmu;
    }

    @Override
    protected Danmakus parse() {
        Danmakus result = new Danmakus(IDanmakus.ST_BY_TIME);
        for (Danmu.D d : danmu.getD()) {
            String[] values = d.getP().split(",");
            if (values.length < 8) continue;
            int type = Integer.parseInt(values[1]);
            long time = (long) (Float.parseFloat(values[0]) * 1000);
            float size = Float.parseFloat(values[2]) * (mDispDensity - 0.6f);
            int color = (int) ((0x00000000ff000000L | Long.parseLong(values[3])) & 0x00000000ffffffffL);
            BaseDanmaku item = mContext.mDanmakuFactory.createDanmaku(type, mContext);
            item.setTime(time);
            item.setTimer(mTimer);
            item.setTextSize(size);
            item.setTextColor(color);
            item.setTextShadowColor(color <= Color.BLACK ? Color.WHITE : Color.BLACK);
            item.setFlags(mContext.mGlobalFlagValues);
            Object lock = result.obtainSynchronizer();
            synchronized (lock) {
                result.addItem(item);
            }
        }
        return result;
    }
}
