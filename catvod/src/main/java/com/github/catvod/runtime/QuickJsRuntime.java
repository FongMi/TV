package com.github.catvod.runtime;

import com.github.catvod.crawler.Spider;
import com.fongmi.quickjs.host.Host;

import dalvik.system.DexClassLoader;

import java.util.concurrent.CompletableFuture;

public interface QuickJsRuntime extends RuntimePlugin {

    Session session();

    default CompletableFuture<String> evaluate(String source, String options) {
        CompletableFuture<String> result = new CompletableFuture<>();
        result.completeExceptionally(new UnsupportedOperationException("Update the QuickJS module to use js.eval"));
        return result;
    }

    interface Session {

        Spider spider(String api, DexClassLoader dex, Host host);

        void clear();
    }
}
