package com.undcover.freedom.pyramid;

import android.content.Context;

import androidx.annotation.Keep;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.catvod.utils.Path;

public class Loader {

    private PyObject app;
    private String cache;

    @Keep
    private void init(Context context) {
        if (!Python.isStarted()) Python.start(new AndroidPlatform(context));
        app = Python.getInstance().getModule("app");
        cache = Path.py().getAbsolutePath();
    }

    @Keep
    public Spider spider(Context context, String key, String api) {
        if (app == null) init(context);
        PyObject obj = app.callAttr("spider", cache, key, api);
        return new Spider(app, obj);
    }
}
