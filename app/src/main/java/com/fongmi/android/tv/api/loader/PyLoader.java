package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.runtime.PythonRuntime;
import com.github.catvod.utils.RuntimeModule;

import java.util.Map;

public class PyLoader extends SpiderLoader {

    private final PythonRuntime loader;
    private volatile String recent;

    public PyLoader() {
        this(RuntimeModule.PYTHON.entry(PythonRuntime.class));
    }

    PyLoader(PyLoader previous) {
        this(previous.loader);
    }

    private PyLoader(PythonRuntime loader) {
        loader.initialize();
        this.loader = loader;
    }

    @Override
    protected void clearReferences() {
        recent = null;
    }

    public synchronized void setRecent(String recent) {
        checkOpen();
        this.recent = recent;
    }

    public Spider getSpider(String key, String api, String ext) {
        return load(key, App.get(), ext, () -> newSpider(key, api));
    }

    Spider newSpider(String key, String api) {
        return loader.spider(api, key, JsHost.INSTANCE, JsHost.INSTANCE::getUserAgent);
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        Spider spider = findSpider(recent);
        return spider != null ? spider.proxy(params) : null;
    }
}
