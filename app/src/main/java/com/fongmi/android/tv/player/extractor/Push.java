package com.fongmi.android.tv.player.extractor;

import android.net.Uri;
import android.os.SystemClock;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.activity.DetailActivity;

public class Push implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("push");
    }

    @Override
    public String fetch(String url) throws Exception {
        DetailActivity.push((FragmentActivity) App.activity(), Uri.parse(url.substring(7)));
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
