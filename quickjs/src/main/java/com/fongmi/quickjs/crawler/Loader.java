package com.fongmi.quickjs.crawler;

import com.fongmi.quickjs.host.Host;
import com.whl.quickjs.android.QuickJSLoader;

import dalvik.system.DexClassLoader;

public class Loader {

    public Loader() {
        QuickJSLoader.init();
    }

    public Spider spider(String api, DexClassLoader dex) {
        return spider(api, dex, Host.NONE);
    }

    public Spider spider(String api, DexClassLoader dex, Host host) {
        return new Spider(api, dex, host);
    }
}
