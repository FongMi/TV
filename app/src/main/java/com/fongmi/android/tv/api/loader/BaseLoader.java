package com.fongmi.android.tv.api.loader;

import android.net.Uri;
import android.text.TextUtils;

import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.github.catvod.Proxy;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import dalvik.system.DexClassLoader;

public class BaseLoader {

    private final List<SpiderLoader> retiredLoaders;
    private volatile JarLoader jarLoader;
    private volatile NodeLoader nodeLoader;
    private volatile PyLoader pyLoader;
    private volatile JsLoader jsLoader;

    private BaseLoader() {
        jarLoader = new JarLoader();
        nodeLoader = new NodeLoader();
        retiredLoaders = new ArrayList<>();
    }

    public static BaseLoader get() {
        return Loader.INSTANCE;
    }

    private static boolean isJs(String api) {
        return api.contains(".js");
    }

    private static boolean isPy(String api) {
        return api.contains(".py");
    }

    private static boolean isNode(String api) {
        return api.startsWith("node:");
    }

    private static boolean isCsp(String api) {
        return api.startsWith("csp_");
    }

    private synchronized JsLoader getJsLoader() {
        RuntimeModule.QUICKJS.require();
        if (jsLoader == null) jsLoader = new JsLoader();
        return jsLoader;
    }

    private synchronized PyLoader getPyLoader() {
        RuntimeModule.PYTHON.require();
        if (pyLoader == null) pyLoader = new PyLoader();
        return pyLoader;
    }

    public void clear() {
        clear(false);
    }

    public void destroy() {
        clear(true);
    }

    private void clear(boolean exiting) {
        List<SpiderLoader> previous;
        List<SpiderLoader> retired;
        synchronized (this) {
            previous = Stream.of(nodeLoader, jarLoader, pyLoader, jsLoader).filter(Objects::nonNull).toList();
            retiredLoaders.addAll(previous);
            retired = exiting ? new ArrayList<>(retiredLoaders) : List.of();
            if (exiting) retiredLoaders.clear();
            jarLoader = new JarLoader();
            nodeLoader = new NodeLoader();
            if (exiting) {
                pyLoader = null;
                jsLoader = null;
            } else {
                if (pyLoader != null) pyLoader = new PyLoader(pyLoader);
                if (jsLoader != null) jsLoader = new JsLoader(jsLoader);
            }
        }
        Task.execute(() -> clearLoaders(previous, retired));
    }

    private void clearLoaders(List<SpiderLoader> previous, List<SpiderLoader> retired) {
        Throwable failure = null;
        for (SpiderLoader loader : previous) failure = clearLoader(loader, failure);
        for (SpiderLoader loader : retired) failure = destroyLoader(loader, failure);
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable clearLoader(SpiderLoader loader, Throwable failure) {
        try {
            loader.clear();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private Throwable destroyLoader(SpiderLoader loader, Throwable failure) {
        try {
            loader.destroy();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        if (isNode(api)) {
            RuntimeModule.NODEJS.require();
            return nodeLoader.getSpider(key, api, ext);
        } else if (isPy(api)) {
            return getPyLoader().getSpider(key, api, ext);
        } else if (isJs(api)) {
            return getJsLoader().getSpider(key, api, ext, jar);
        } else if (isCsp(api)) {
            return jarLoader.getSpider(key, api, ext, jar);
        } else {
            return new SpiderNull();
        }
    }

    public Spider getSpider(String key) {
        if (SpiderTestSession.isKey(key)) return SpiderTestSession.require(key);
        Site site = VodConfig.get().getSite(key);
        Live live = LiveConfig.get().getLive(key);
        if (!site.isEmpty()) return site.spider();
        if (!live.isEmpty()) return live.spider();
        return new SpiderNull();
    }

    public void setRecent(String key, String api, String jar) {
        if (isNode(api)) return;
        if (isJs(api)) getJsLoader().setRecent(key);
        else if (isPy(api)) getPyLoader().setRecent(key);
        else if (isCsp(api)) jarLoader.setRecent(Crypto.md5(jar));
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        if (params.containsKey("siteKey")) return getSpider(params.get("siteKey")).proxy(params);
        if ("js".equals(params.get("do"))) return getJsLoader().proxy(params);
        if ("py".equals(params.get("do"))) return getPyLoader().proxy(params);
        return jarLoader.proxy(params);
    }

    public void requireProxy(String url) {
        Uri uri = getProxyUri(url);
        if (uri == null) return;
        String key = uri.getQueryParameter("siteKey");
        if (key != null) requireSite(key);
        else if ("py".equals(uri.getQueryParameter("do"))) RuntimeModule.PYTHON.require();
        else if ("js".equals(uri.getQueryParameter("do"))) RuntimeModule.QUICKJS.require();
    }

    private Uri getProxyUri(String url) {
        String prefix = Proxy.getUrl(true) + "?";
        if (url.startsWith("proxy://")) url = prefix + url.substring(8);
        return url.startsWith(prefix) ? Uri.parse(url) : null;
    }

    private void requireSite(String key) {
        if (SpiderTestSession.isKey(key)) {
            SpiderTestSession.require(key);
            return;
        }
        Site site = VodConfig.get().getSite(key);
        Live live = LiveConfig.get().getLive(key);
        requireRuntime(!site.isEmpty() ? site.getApi() : live.getApi());
    }

    private void requireRuntime(String api) {
        if (isNode(api)) RuntimeModule.NODEJS.require();
        else if (isPy(api)) RuntimeModule.PYTHON.require();
        else if (isJs(api)) RuntimeModule.QUICKJS.require();
    }

    public void parseJar(String jar, boolean recent) {
        if (TextUtils.isEmpty(jar)) return;
        String key = Crypto.md5(jar);
        JarLoader loader = jarLoader;
        loader.parseJar(key, jar);
        if (recent) loader.setRecent(key);
    }

    public DexClassLoader dex(String jar) {
        return jarLoader.dex(jar);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    private static class Loader {
        static volatile BaseLoader INSTANCE = new BaseLoader();
    }
}
