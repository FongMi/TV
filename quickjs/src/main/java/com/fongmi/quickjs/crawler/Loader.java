package com.fongmi.quickjs.crawler;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.host.Host;
import com.fongmi.quickjs.utils.Module;
import com.fongmi.quickjs.QuickJSLoader;

import dalvik.system.DexClassLoader;

public class Loader {

    public Loader() {
        QuickJSLoader.init();
    }

    public static void check() {
        try (QuickJS context = new QuickJS(QuickJSLoader::init)) {
            if (((Number) context.evaluate("6 * 7").join()).intValue() != 42) throw new IllegalStateException("QuickJS evaluation returned an unexpected result");
        }
    }

    public Spider spider(String api, DexClassLoader dex) {
        return spider(api, dex, Host.NONE);
    }

    public Spider spider(String api, DexClassLoader dex, Host host) {
        return new Spider(api, dex, host);
    }

    public Spider spider(String api, DexClassLoader dex, Host host, Module module) {
        return new Spider(api, dex, host, module);
    }
}
