package com.fongmi.android.tv.bean;

import androidx.annotation.Nullable;

public class Page {

    private final String vodId;
    private final Style style;
    private final int position;

    public static Page get(Vod vod, int position) {
        return new Page(vod, position);
    }

    private Page(Vod vod, int position) {
        this.vodId = vod.getVodId();
        this.style = vod.getCate() != null ? vod.getCate().getStyle() : null;
        this.position = position;
    }

    public String getVodId() {
        return vodId;
    }

    public Style getStyle() {
        return style;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Page)) return false;
        Page it = (Page) obj;
        return getVodId().equals(it.getVodId()) && getPosition() == it.getPosition();
    }
}
