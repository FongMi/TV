package com.fongmi.chaquo;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.Net;
import com.github.catvod.runtime.PythonRuntime;

import java.util.function.Supplier;

public final class RuntimeEntry implements PythonRuntime {

    private Loader loader;

    private synchronized Loader loader() {
        if (loader == null) loader = new Loader();
        return loader;
    }

    @Override
    public void initialize() {
        loader();
    }

    @Override
    public void check() {
        Loader.check();
    }

    @Override
    public String format(String source) {
        loader();
        try (PyObject result = Python.getInstance().getModule("_tv_format").callAttr("format_source", source)) {
            return result.toString();
        }
    }

    @Override
    public Spider spider(String api, String siteKey, Net.Cookies cookies, Supplier<String> userAgent) {
        return loader().spider(api, siteKey, cookies, userAgent);
    }
}
