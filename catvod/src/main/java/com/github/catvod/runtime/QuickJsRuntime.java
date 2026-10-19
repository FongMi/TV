package com.github.catvod.runtime;

import com.github.catvod.crawler.Spider;
import com.fongmi.quickjs.host.Host;

import dalvik.system.DexClassLoader;

public interface QuickJsRuntime extends RuntimePlugin {

    Session session();

    interface Session {

        Spider spider(String api, DexClassLoader dex, Host host);

        void clear();
    }
}
