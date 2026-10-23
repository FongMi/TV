package com.fongmi.quickjs;

import com.fongmi.quickjs.crawler.Loader;
import com.fongmi.quickjs.host.Host;
import com.fongmi.quickjs.utils.Module;
import com.fongmi.quickjs.utils.Scripts;
import com.github.catvod.crawler.Spider;
import com.github.catvod.runtime.QuickJsRuntime;

import dalvik.system.DexClassLoader;

import java.util.concurrent.CompletableFuture;

public final class RuntimeEntry implements QuickJsRuntime {

    private Loader loader;

    @Override
    public void check() {
        Loader.check();
    }

    @Override
    public CompletableFuture<String> evaluate(String source, String options) {
        return Scripts.evaluate(source, options);
    }

    @Override
    public synchronized Session session() {
        if (loader == null) loader = new Loader();
        return new Session() {
            private final Module module = new Module();

            @Override
            public Spider spider(String api, DexClassLoader dex, Host host) {
                return loader.spider(api, dex, host, module);
            }

            @Override
            public void clear() {
                module.clear();
            }
        };
    }
}
