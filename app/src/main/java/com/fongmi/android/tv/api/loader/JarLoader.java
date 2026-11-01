package com.fongmi.android.tv.api.loader;

import android.content.Context;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.exception.SpiderLoadException;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.Download;
import com.github.catvod.net.Net;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Local;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

public class JarLoader extends SpiderLoader {

    private final ConcurrentHashMap<String, DexClassLoader> loaders;
    private final ConcurrentHashMap<String, Method> methods;
    private final ConcurrentHashMap<String, Object> locks;
    private volatile String recent;

    public JarLoader() {
        loaders = new ConcurrentHashMap<>();
        methods = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    @Override
    protected void clearReferences() {
        loaders.clear();
        methods.clear();
        locks.clear();
        recent = null;
    }

    public synchronized void setRecent(String recent) {
        checkOpen();
        this.recent = recent;
    }

    private void load(String key, File file) {
        if (Thread.interrupted()) return;
        if (!Path.exists(file) || !file.setReadOnly()) return;
        String cachePath = Path.jar().getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(file.getAbsolutePath(), cachePath, cachePath, RuntimeModule.getSpiderClassLoader());
        try {
            initDex(loader);
        } catch (Exception e) {
            throw new SpiderLoadException(e);
        }
        cacheLoader(key, loader);
    }

    private synchronized void cacheLoader(String key, DexClassLoader loader) {
        checkOpen();
        invokeProxy(key, loader);
        loaders.put(key, loader);
    }

    static void initDex(DexClassLoader loader) throws Exception {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Init");
            clz.getMethod("init", Context.class).invoke(null, App.get());
        } catch (ClassNotFoundException ignored) {
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception error) throw error;
            if (e.getCause() instanceof Error error) throw error;
            throw e;
        }
    }

    private void invokeProxy(String key, DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Proxy");
            Method method = clz.getMethod("proxy", Map.class);
            methods.put(key, method);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void parseJar(String key, String jar) {
        Object lock;
        synchronized (this) {
            checkOpen();
            if (loaders.containsKey(key)) return;
            lock = locks.computeIfAbsent(key, k -> new Object());
        }
        if (jar.startsWith("assets")) jar = UrlUtil.convert(jar);
        synchronized (lock) {
            checkOpen();
            if (!loaders.containsKey(key)) loadJar(key, jar);
        }
    }

    private void loadJar(String key, String jar) {
        String[] texts = jar.split(";md5;");
        String md5 = texts.length > 1 ? texts[1].trim() : "";
        if (md5.startsWith("http")) md5 = OkHttp.string(md5).trim();
        jar = texts[0];
        if (!md5.isEmpty() && Crypto.equals(Path.jar(jar), md5)) load(key, Path.jar(jar));
        else if (jar.startsWith("http")) load(key, Download.create(jar, Path.jar(jar)).get());
        else if (jar.startsWith("file")) load(key, Path.local(jar));
    }

    public DexClassLoader dex(String jar) {
        if (TextUtils.isEmpty(jar)) return null;
        String jaKey = Crypto.md5(jar);
        parseJar(jaKey, jar);
        DexClassLoader loader = loaders.get(jaKey);
        if (loader == null) throw new SpiderLoadException(new IllegalStateException("Cannot load spider JAR: " + jar));
        return loader;
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        return load(Crypto.md5(jar) + key, App.get(), ext, () -> newSpider(key, api, jar));
    }

    Spider newSpider(String key, String api, String jar) throws Exception {
        String jaKey = Crypto.md5(jar);
        parseJar(jaKey, jar);
        DexClassLoader loader = loaders.get(jaKey);
        if (loader == null) throw new IllegalStateException("Cannot load spider JAR: " + jar);
        return newSpider(key, api, loader);
    }

    private static DexClassLoader openDex(File file) throws Exception {
        if (!Path.exists(file)) throw new IllegalArgumentException("JAR is missing or empty: " + file);
        if (!file.setReadOnly()) throw new IllegalStateException("Cannot mark JAR read-only: " + file);
        String cachePath = file.getParent();
        return new DexClassLoader(file.getAbsolutePath(), cachePath, cachePath, RuntimeModule.getSpiderClassLoader());
    }

    @SuppressWarnings("deprecation")
    static List<String> spiders(File file) throws Exception {
        DexClassLoader loader = openDex(file);
        TreeSet<String> names = new TreeSet<>();
        DexFile dex = new DexFile(file);
        try (Closeable close = dex::close) {
            readSpiders(dex, loader, names);
        }
        return new ArrayList<>(names);
    }

    private static void readSpiders(DexFile dex, DexClassLoader loader, TreeSet<String> names) throws Exception {
        Enumeration<String> entries = dex.entries();
        while (entries.hasMoreElements()) {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException("Crawler test session has ended");
            addSpider(entries.nextElement().replace('/', '.'), loader, names);
        }
    }

    private static void addSpider(String name, DexClassLoader loader, TreeSet<String> names) throws Exception {
        String prefix = "com.github.catvod.spider.";
        if (!name.startsWith(prefix)) return;
        Class<?> clz = Class.forName(name, false, loader);
        if (clz.getClassLoader() != loader || !Spider.class.isAssignableFrom(clz) || !Modifier.isPublic(clz.getModifiers()) || Modifier.isAbstract(clz.getModifiers())) return;
        if (Arrays.stream(clz.getConstructors()).anyMatch(constructor -> constructor.getParameterTypes().length == 0)) names.add("csp_" + name.substring(prefix.length()));
    }

    static DexClassLoader newDex(File file) throws Exception {
        DexClassLoader loader = openDex(file);
        initDex(loader);
        return loader;
    }

    static Spider newSpider(String key, String api, DexClassLoader loader) throws Exception {
        Spider spider;
        try {
            spider = (Spider) loader.loadClass("com.github.catvod.spider." + api.substring(4)).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception error) throw error;
            if (e.getCause() instanceof Error error) throw error;
            throw e;
        }
        spider.siteKey = key;
        spider.net = new Net(JsHost.INSTANCE, key);
        spider.local = new Local(key);
        return spider;
    }

    private DexClassLoader requireRecentLoader() {
        DexClassLoader loader = loaders.get(recent);
        if (loader == null) throw new IllegalStateException("No jar loaded for recent key: " + recent);
        return loader;
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        Class<?> clz = requireRecentLoader().loadClass("com.github.catvod.parser.Json" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class);
        return (JSONObject) method.invoke(null, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        Class<?> clz = requireRecentLoader().loadClass("com.github.catvod.parser.Mix" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
        return (JSONObject) method.invoke(null, jxs, name, flag, url);
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        Method method = recent != null ? methods.get(recent) : null;
        Object[] result = proxyInvoke(method, params);
        if (result != null) return result;
        return tryOthers(params);
    }

    private Object[] tryOthers(Map<String, String> p) {
        return methods.entrySet().stream().filter(e -> !e.getKey().equals(recent)).map(e -> proxyInvoke(e.getValue(), p)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Object[] proxyInvoke(Method method, Map<String, String> params) {
        try {
            return method == null ? null : (Object[]) method.invoke(null, params);
        } catch (Throwable e) {
            RuntimeModule.MissingException missing = RuntimeModule.MissingException.find(e);
            if (missing != null) throw missing;
            e.printStackTrace();
            return null;
        }
    }
}
