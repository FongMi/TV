package com.fongmi.chaquo;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.github.catvod.net.Net;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;

import java.util.function.Supplier;

public class Loader {

    private final PyObject app;

    public Loader() {
        RuntimeModule.PYTHON.require();
        synchronized (Python.class) {
            if (!Python.isStarted()) Python.start(Platform.create());
            app = Python.getInstance().getModule("app");
        }
    }

    public static void check() {
        new Loader();
        Python python = Python.getInstance();
        python.getModule("ujson");
        python.getModule("Crypto.Cipher.AES");
        try (PyObject globals = python.getBuiltins().callAttr("dict");
             PyObject result = python.getBuiltins().callAttr("eval", "6 * 7", globals)) {
            if (result.toInt() != 42) throw new IllegalStateException("Python evaluation returned an unexpected result");
        }
    }

    public Spider spider(String api) {
        return spider(api, null);
    }

    public Spider spider(String api, Net.Cookies cookies) {
        return spider(api, cookies, null);
    }

    public Spider spider(String api, Net.Cookies cookies, Supplier<String> userAgent) {
        return spider(api, "", cookies, userAgent);
    }

    public Spider spider(String api, String siteKey, Net.Cookies cookies, Supplier<String> userAgent) {
        Net net = siteKey.isEmpty() ? new Net(cookies) : new Net(cookies, siteKey);
        PyObject obj = null;
        try {
            obj = app.callAttr("spider", Path.py().getAbsolutePath(), api, siteKey, net);
            Spider spider = new Spider(app, obj, api, cookies, userAgent, net);
            spider.siteKey = siteKey;
            return spider;
        } catch (RuntimeException | Error e) {
            releaseSpider(net, obj, e);
            throw e;
        }
    }

    private void releaseSpider(Net net, PyObject obj, Throwable failure) {
        try {
            net.close();
        } catch (RuntimeException | Error error) {
            if (error != failure) failure.addSuppressed(error);
        }
        try {
            if (obj != null) app.callAttr("release", obj);
        } catch (RuntimeException | Error error) {
            if (error != failure) failure.addSuppressed(error);
        }
    }
}
