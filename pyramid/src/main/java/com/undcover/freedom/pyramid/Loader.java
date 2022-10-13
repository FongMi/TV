package com.undcover.freedom.pyramid;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class Loader {

    private PyObject app;
    private String cache;

    private void init(Context context) {
        if (!Python.isStarted()) Python.start(new AndroidPlatform(context));
        cache = context.getCacheDir().getAbsolutePath() + "/plugin/";
        app = Python.getInstance().getModule("app");
    }

    public Spider spider(Context context, String name, String ext) {
        if (app == null) init(context);
        PyObject obj = app.callAttr("init_py", cache, name, ext);
        return new Spider(app, obj);
    }
}
