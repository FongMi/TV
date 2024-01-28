package com.fongmi.android.tv.player.extractor;

import android.os.SystemClock;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.activity.VideoActivity;

public class Push implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("push");
    }

    @Override
    public String fetch(String url) throws Exception {
        if (App.activity() != null) VideoActivity.start(App.activity(), url.substring(7));
        SystemClock.sleep(500);
        return "";
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
