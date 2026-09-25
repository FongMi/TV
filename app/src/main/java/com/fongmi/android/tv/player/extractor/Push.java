package com.fongmi.android.tv.player.extractor;

import android.net.Uri;
import android.os.SystemClock;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.UrlUtil;

public class Push implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return "push".equals(UrlUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) throws Exception {
        if (App.activity() != null) VideoActivity.start(App.activity(), url.substring(7));
        SystemClock.sleep(500);
        return "";
    }
}
