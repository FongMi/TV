package com.fongmi.quickjs.utils;

import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.LruCache;

import com.fongmi.quickjs.ModuleBytecodeCache;
import com.fongmi.quickjs.QuickJS;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.UriUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.HttpUrl;

public class Module implements QuickJS.ModuleLoader {

    private static final int MAX_SIZE = 50;
    private static final int MAX_BYTECODE_FILES = 50;
    private static final long MAX_BYTECODE_BYTES = 16 * 1024 * 1024;
    private final LruCache<String, String> cache;
    private final Map<String, CompletableFuture<String>> pending = new HashMap<>();
    private Net net = new Net();
    private ModuleBytecodeCache bytecode;

    public Module() {
        cache = new LruCache<>(MAX_SIZE);
    }

    private synchronized ModuleBytecodeCache bytecode() {
        if (bytecode == null) bytecode = createBytecodeCache();
        return bytecode;
    }

    private ModuleBytecodeCache createBytecodeCache() {
        String abi = Process.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0];
        File directory = new File(Path.cache(), "quickjs-bytecode");
        String identity = RuntimeModule.QUICKJS.getCompatibilityId() + '\0' + abi;
        return new ModuleBytecodeCache(directory, identity, MAX_BYTECODE_BYTES, MAX_BYTECODE_FILES, SpiderDebug::log);
    }

    @Override
    public String normalize(String base, String name) {
        return UriUtil.resolve(base, name);
    }

    @Override
    public String url(String name) {
        return name.startsWith("http://") || name.startsWith("https://") ? HttpUrl.get(name).uri().toString() : null;
    }

    @Override
    public boolean cacheBytecode() {
        return true;
    }

    @Override
    public byte[] loadBytecode(String name, String source) {
        return bytecode().load(name, source);
    }

    @Override
    public void storeBytecode(String name, String source, byte[] bytes) {
        bytecode().store(name, source, bytes);
    }

    @Override
    public String load(String name) {
        CompletableFuture<String> future;
        Net client;
        boolean loading;
        synchronized (this) {
            String content = cache.get(name);
            if (!TextUtils.isEmpty(content)) return content;
            loading = !pending.containsKey(name);
            future = pending.computeIfAbsent(name, key -> new CompletableFuture<>());
            client = net;
        }
        if (loading) load(name, client, future);
        return awaitSource(name, future);
    }

    private String awaitSource(String name, CompletableFuture<String> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Module load canceled", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Module load failed: " + name, e.getCause());
        }
    }

    private void load(String name, Net client, CompletableFuture<String> future) {
        try {
            read(name, client).whenComplete((content, error) -> finish(name, future, content, error));
        } catch (Throwable error) {
            finish(name, future, null, error);
        }
    }

    private void finish(String name, CompletableFuture<String> future, String content, Throwable error) {
        synchronized (this) {
            if (pending.remove(name, future) && error == null && content != null) cache.put(name, content);
        }
        if (error == null) future.complete(content);
        else future.completeExceptionally(error);
    }

    private CompletableFuture<String> read(String name, Net client) {
        if (name.startsWith("http")) return client.requestAsync(name, new NetOptions()).thenApply(this::readResponse);
        if (name.startsWith("assets")) return CompletableFuture.completedFuture(RuntimeModule.QUICKJS.readAsset(name));
        if (name.startsWith("lib/")) return CompletableFuture.completedFuture(RuntimeModule.QUICKJS.readAsset("js/" + name));
        return CompletableFuture.completedFuture(null);
    }

    private String readResponse(Net.Result result) {
        if (result.error != null) throw new IllegalStateException(result.error);
        int code = (Integer) result.code;
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
        return result.text();
    }

    public synchronized void clear() {
        try {
            net.close();
        } finally {
            net = new Net();
            pending.values().forEach(future -> future.cancel(false));
            pending.clear();
            cache.evictAll();
        }
    }
}
