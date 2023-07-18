package com.fongmi.android.tv.bean;

import androidx.annotation.Nullable;

public class Page {

    private final String vodId;
    private final int position;

    public static Page get(String vodId, int position) {
        return new Page(vodId, position);
    }

    private Page(String vodId, int position) {
        this.vodId = vodId;
        this.position = position;
    }

    public String getVodId() {
        return vodId;
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
