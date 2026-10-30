package com.github.catvod.runtime;

import com.github.catvod.crawler.Spider;
import com.fongmi.quickjs.host.Host;
import com.github.catvod.utils.Task;

import dalvik.system.DexClassLoader;

import java.util.concurrent.CompletableFuture;

public interface QuickJsRuntime extends RuntimePlugin {

    Session session();

    default CompletableFuture<String> evaluate(String source, String options) {
        return Task.failedFuture(new UnsupportedOperationException("Update the QuickJS module to use js.eval"));
    }

    interface Session {

        Spider spider(String api, DexClassLoader dex, Host host);

        void clear();
    }
}
