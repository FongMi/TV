package com.undcover.freedom.pyramid;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.catvod.crawler.Spider;

import java.util.List;

public class Loader {

    private PyObject app;
    private String cache;

    public void init(Context context) {
        new Thread(() -> {
            if (!Python.isStarted()) Python.start(new AndroidPlatform(context));
            cache = context.getCacheDir().getAbsolutePath();
            app = Python.getInstance().getModule("app");
        }).start();
    }

    public Spider spider(String ext) {
        String path = app.callAttr("downloadPlugin", cache, ext).toString();
        PyObject pySpider = app.callAttr("loadFromDisk", path);
        List<PyObject> dependencies = app.callAttr("getDependence", pySpider).asList();
        for (PyObject dependence : dependencies) {
            String api = dependence.toString();
        }
        return new PySpider(app, pySpider);
    }
}
