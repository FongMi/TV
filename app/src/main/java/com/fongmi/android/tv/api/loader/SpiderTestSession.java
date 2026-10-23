package com.fongmi.android.tv.api.loader;

import android.os.CancellationSignal;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.Download;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.github.catvod.net.NetTrace;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import dalvik.system.DexClassLoader;

import fi.iki.elonen.NanoHTTPD.Response;
import okhttp3.HttpUrl;

public final class SpiderTestSession implements AutoCloseable {

    private static final String PREFIX = "spider-test:";
    private static final String PREVIEW = "spider-test://";
    private static final ConcurrentHashMap<String, State> sessions = new ConcurrentHashMap<>();
    private final Executor cleanup;
    private volatile State state;
    private volatile boolean closed;

    public SpiderTestSession() {
        this(Task::execute);
    }

    SpiderTestSession(Executor cleanup) {
        this.cleanup = cleanup;
    }

    public void load(String api, String ext, String jar) throws Exception {
        validate(api, jar);
        State next = begin();
        Throwable failure = null;
        try {
            next.load(api, ext, jar);
            next.publish();
        } catch (Exception | Error error) {
            failure = next.close(error);
        }
        failure = next.end(failure);
        if (failure instanceof Exception error) throw error;
        if (failure instanceof Error error) throw error;
    }

    public JSONArray spiders(String jar) throws Exception {
        if (jar == null || jar.isBlank()) throw new IllegalArgumentException("Java crawler JAR is required");
        State next = begin();
        JSONArray names = null;
        Throwable failure = null;
        try {
            names = new JSONArray(JarLoader.spiders(next.readJar(jar)));
            next.requireOpen();
        } catch (Exception | Error error) {
            failure = error;
        }
        failure = next.close(failure);
        failure = next.end(failure);
        if (failure instanceof Exception error) throw error;
        if (failure instanceof Error error) throw error;
        return names;
    }

    private State begin() {
        State next;
        State previous;
        synchronized (this) {
            if (closed) throw new CancellationException("Crawler test session has ended");
            next = new State(cleanup);
            previous = state;
            state = next;
        }
        if (previous != null) previous.close();
        next.begin();
        return next;
    }

    public String getKey() {
        State current = state;
        return current == null ? "" : current.key;
    }

    public boolean isLoaded() {
        State current = state;
        return current != null && current.loaded && !current.closed;
    }

    public JSONObject requests() {
        State current = state;
        return current == null ? new JSONObject() : new JSONObject(current.trace.snapshot());
    }

    public String preview(String result) {
        State current = state;
        if (current == null) throw new IllegalStateException("Load a crawler first");
        synchronized (current) {
            current.requireOpen();
            if (!current.loaded) throw new IllegalStateException("Load a crawler first");
            current.previewId = UUID.randomUUID().toString();
            current.preview = result;
            return PREVIEW + current.key.substring(PREFIX.length()) + "/" + current.previewId;
        }
    }

    public static boolean isPreview(String url) {
        return url.startsWith(PREVIEW);
    }

    public static String previewResult(String url) {
        String[] parts = url.substring(PREVIEW.length()).split("/", 2);
        State current = parts.length == 2 ? sessions.get(PREFIX + parts[0]) : null;
        if (current == null) throw new IllegalStateException("Crawler preview expired. Send it again from the test page.");
        synchronized (current) {
            current.requireOpen();
            if (!parts[1].equals(current.previewId)) throw new IllegalStateException("Crawler preview was replaced. Send it again from the test page.");
            return current.preview;
        }
    }

    public Object call(String method, JSONArray args) throws Exception {
        State current = state;
        if (current == null || !current.loaded) throw new IllegalStateException("Load a crawler first");
        current.begin();
        try {
            return current.finishCall(call(current.spider, method, args));
        } finally {
            current.end();
        }
    }

    @Override
    public void close() {
        State current;
        synchronized (this) {
            closed = true;
            current = state;
        }
        if (current != null) current.close();
    }

    static boolean isKey(String key) {
        return key != null && key.startsWith(PREFIX);
    }

    static Spider require(String key) {
        State current = sessions.get(key);
        if (current == null) throw new IllegalStateException("Crawler test session has ended");
        current.requireOpen();
        return current.spider;
    }

    static void validate(String api, String jar) throws Exception {
        if (api == null || api.isBlank()) throw new IllegalArgumentException("Crawler API is required");
        if (api.startsWith("csp_")) {
            if (!api.substring(4).matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) throw new IllegalArgumentException("Invalid Java crawler class");
            if (jar == null || jar.isBlank()) throw new IllegalArgumentException("Java crawler JAR is required");
            return;
        }
        String path = sourcePath(api);
        if (path == null || !path.endsWith(".py") && !path.endsWith(".js")) throw new IllegalArgumentException("Use a .py, .js or csp_ crawler API");
    }

    private static String sourcePath(String api) throws Exception {
        HttpUrl url = HttpUrl.parse(api);
        return url == null ? new URI(api).getPath() : url.uri().getPath();
    }

    static Object call(Spider spider, String method, JSONArray args) throws Exception {
        return switch (method) {
            case "home" -> spider.homeContent(args.getBoolean(0));
            case "homeVod" -> spider.homeVideoContent();
            case "category" -> spider.categoryContent(args.getString(0), args.getString(1), args.getBoolean(2), stringMap(args.getJSONObject(3)));
            case "detail" -> spider.detailContent(strings(args.getJSONArray(0)));
            case "search" -> {
                String keyword = args.getString(0), page = args.getString(2);
                boolean quick = args.getBoolean(1);
                yield page.equals("1") ? spider.searchContent(keyword, quick) : spider.searchContent(keyword, quick, page);
            }
            case "play" -> spider.playerContent(args.getString(0), args.getString(1), strings(args.getJSONArray(2)));
            case "action" -> spider.action(args.getString(0));
            case "live" -> spider.liveContent(args.getString(0));
            case "sniffer" -> spider.manualVideoCheck();
            case "isVideo" -> spider.isVideoFormat(args.getString(0));
            case "proxy" -> spider.proxy(stringMap(args.getJSONObject(0)));
            default -> throw new IllegalArgumentException("Unknown crawler method: " + method);
        };
    }

    private static HashMap<String, String> stringMap(JSONObject object) throws Exception {
        HashMap<String, String> values = new HashMap<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            values.put(key, object.getString(key));
        }
        return values;
    }

    private static List<String> strings(JSONArray array) throws Exception {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) values.add(array.getString(i));
        return values;
    }

    static final class State {

        final String key = PREFIX + UUID.randomUUID();
        private final NetTrace trace = new NetTrace(key);
        private final Executor cleanup;
        private volatile Thread worker;
        private volatile Spider spider;
        private volatile Net source;
        private volatile CancellationSignal download;
        private volatile JsLoader js;
        private volatile File file;
        private volatile boolean closed;
        private String previewId;
        private String preview;
        private volatile boolean loaded;

        State(Executor cleanup) {
            this.cleanup = cleanup;
        }

        synchronized void begin() {
            requireOpen();
            if (worker != null) throw new IllegalStateException("A crawler operation is already running");
            trace.start();
            worker = Thread.currentThread();
        }

        synchronized void end() {
            trace.stop();
            worker = null;
            if (closed && file != null) file.delete();
        }

        void requireOpen() {
            if (closed) throw new CancellationException("Crawler test session has ended");
        }

        Object finishCall(Object result) {
            try {
                requireOpen();
                return result;
            } catch (CancellationException error) {
                closeCanceledResult(result, error);
                throw error;
            }
        }

        private void closeCanceledResult(Object result, CancellationException error) {
            try {
                if (result instanceof Object[] values) {
                    if (values.length > 0 && values[0] instanceof Response response) response.close();
                    else if (values.length > 2 && values[2] instanceof InputStream input) input.close();
                }
            } catch (Throwable closeError) {
                error.addSuppressed(closeError);
            }
        }

        void load(String api, String ext, String jar) throws Exception {
            Spider created = createSpider(api, jar);
            bind(created);
            created.init(App.get(), ext == null ? "" : ext);
            requireOpen();
        }

        private Spider createSpider(String api, String jar) throws Exception {
            boolean python = !api.startsWith("csp_") && sourcePath(api).endsWith(".py");
            DexClassLoader dex = python || jar == null || jar.isBlank() ? null : JarLoader.newDex(readJar(jar));
            requireOpen();
            Spider created;
            if (api.startsWith("csp_")) {
                created = JarLoader.newSpider(key, api, dex);
            } else if (python) {
                RuntimeModule.PYTHON.require();
                created = new PyLoader().newSpider(key, api);
            } else {
                created = createJavaScript(api, dex);
            }
            return created;
        }

        private Spider createJavaScript(String api, DexClassLoader dex) {
            RuntimeModule.QUICKJS.require();
            JsLoader loader = new JsLoader();
            synchronized (this) {
                if (closed) {
                    cleanup.execute(loader::clear);
                    requireOpen();
                }
                js = loader;
            }
            return loader.newSpider(key, api, dex);
        }

        void bind(Spider created) {
            synchronized (this) {
                if (!closed) {
                    spider = created;
                    return;
                }
            }
            Throwable failure = closeUnpublishedNetwork(created);
            failure = destroyUnpublished(created, failure);
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
            requireOpen();
        }

        synchronized void publish() {
            requireOpen();
            sessions.put(key, this);
            loaded = true;
        }

        private File readJar(String jar) throws Exception {
            Net client = new Net(JsHost.INSTANCE, key);
            CancellationSignal cancellation = new CancellationSignal();
            File target = Path.cache(key.substring(PREFIX.length()) + ".jar");
            synchronized (this) {
                if (closed) {
                    client.close();
                    requireOpen();
                }
                source = client;
                download = cancellation;
                file = target;
            }
            String[] parts = jar.split(";md5;", 2);
            String url = parts[0];
            if (url.startsWith("assets")) url = UrlUtil.convert(url);
            if (url.startsWith("http://") || url.startsWith("https://")) Download.create(url, target).get(cancellation, null);
            else if (url.startsWith("file://")) Files.copy(Path.local(url).toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            else throw new IllegalArgumentException("Use an HTTP or file JAR URL");
            requireOpen();
            if (parts.length == 2) verifyJar(client, target, parts[1].trim());
            return target;
        }

        private void verifyJar(Net client, File target, String expected) throws IOException {
            if (expected.startsWith("http://") || expected.startsWith("https://")) {
                Net.Result result = client.request(expected, new NetOptions());
                if (result.error != null) throw new IOException(result.error);
                if ((Integer) result.code < 200 || (Integer) result.code >= 300) throw new IOException("MD5 HTTP " + result.code);
                expected = result.text().trim();
            }
            if (!expected.isEmpty() && !Crypto.equals(target, expected)) throw new IOException("JAR MD5 mismatch");
        }

        void close() {
            Spider current;
            JsLoader loader;
            File jar;
            synchronized (this) {
                if (closed) return;
                closed = true;
                trace.close();
                loaded = false;
                sessions.remove(key, this);
                if (worker != null && worker != Thread.currentThread()) worker.interrupt();
                current = spider;
                loader = js;
                jar = file;
            }
            Throwable failure = cancelDownload();
            failure = closeSource(failure);
            failure = closeSpiderNetwork(current, failure);
            failure = cancelJavaScript(loader, failure);
            failure = destroyLater(current, loader, jar, failure);
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        }

        private static void destroy(Spider current, JsLoader loader, File jar) {
            Throwable failure = destroySpider(current);
            failure = clearModule(loader, failure);
            failure = deleteJar(jar, failure);
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        }

        private Throwable close(Throwable failure) {
            try {
                close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private Throwable end(Throwable failure) {
            try {
                end();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private Throwable cancelDownload() {
            try {
                if (download != null) download.cancel();
                return null;
            } catch (RuntimeException | Error error) {
                return error;
            }
        }

        private Throwable closeSource(Throwable failure) {
            try {
                if (source != null) source.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private static Throwable closeUnpublishedNetwork(Spider created) {
            try {
                created.js.close();
                if (created.net != null) created.net.close();
                return null;
            } catch (RuntimeException | Error error) {
                return error;
            }
        }

        private static Throwable closeSpiderNetwork(Spider current, Throwable failure) {
            try {
                if (current != null) current.js.close();
                if (current != null && current.net != null) current.net.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private static Throwable cancelJavaScript(JsLoader loader, Throwable failure) {
            try {
                if (loader != null) loader.cancel();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private Throwable destroyUnpublished(Spider created, Throwable failure) {
            try {
                cleanup.execute(created::destroy);
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private Throwable destroyLater(Spider current, JsLoader loader, File jar, Throwable failure) {
            try {
                cleanup.execute(() -> destroy(current, loader, jar));
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private static Throwable destroySpider(Spider current) {
            try {
                if (current != null) current.destroy();
                return null;
            } catch (RuntimeException | Error error) {
                return error;
            }
        }

        private static Throwable clearModule(JsLoader loader, Throwable failure) {
            try {
                if (loader != null) loader.clear();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }

        private static Throwable deleteJar(File jar, Throwable failure) {
            try {
                if (jar != null) jar.delete();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            return failure;
        }
    }
}
