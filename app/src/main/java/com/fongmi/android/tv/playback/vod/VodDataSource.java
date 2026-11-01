package com.fongmi.android.tv.playback.vod;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;

import java.util.List;

public interface VodDataSource {

    void detailContent(String key, String id);

    void detailContent(String key, String id, boolean builtin);

    void cancelContent();

    void playerContent(VodPlayRequest request);

    void resolveContent(VodPlayRequest request, Result result);

    void preloadContent(VodPlayRequest request);

    void cancelPreloadContent();

    void searchContent(List<Site> sites, String keyword, boolean quick);
}
