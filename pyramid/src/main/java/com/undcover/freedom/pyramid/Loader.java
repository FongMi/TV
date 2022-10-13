package com.undcover.freedom.pyramid;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.catvod.crawler.Spider;

public class Loader {

    private PyObject app;
    private String cache;

    private void init(Context context) {
        if (!Python.isStarted()) Python.start(new AndroidPlatform(context));
        cache = context.getCacheDir().getAbsolutePath();
        app = Python.getInstance().getModule("app");
    }

    public Spider spider(Context context, String ext) {
        if (app == null) init(context);
        String path = app.callAttr("downloadPlugin", cache, ext).toString();
        return new PySpider(app, app.callAttr("loadFromDisk", path));
    }
}
