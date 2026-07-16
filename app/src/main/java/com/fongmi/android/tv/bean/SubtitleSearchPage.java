package com.fongmi.android.tv.bean;

import java.util.List;

public final class SubtitleSearchPage {

    private final List<SubtitleSearchItem> items;
    private final int resultCount;

    private SubtitleSearchPage(List<SubtitleSearchItem> items, int resultCount) {
        this.items = items;
        this.resultCount = resultCount;
    }

    public static SubtitleSearchPage from(List<SubtitleSearchItem> items, int resultCount) {
        return new SubtitleSearchPage(items, resultCount);
    }

    public List<SubtitleSearchItem> getItems() {
        return items;
    }

    public int getResultCount() {
        return resultCount;
    }
}
