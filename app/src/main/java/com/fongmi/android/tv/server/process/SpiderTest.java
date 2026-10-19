package com.fongmi.android.tv.server.process;

import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import com.fongmi.android.tv.api.loader.SpiderTestSession;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.server.SpiderSource;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.runtime.PythonRuntime;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import okhttp3.HttpUrl;

public final class SpiderTest implements Process, AutoCloseable {

    private static final String API = "/spider-api";
    private static final long LIFETIME = TimeUnit.MINUTES.toMillis(15);
    private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    private final Future<?> pruning = Task.scheduler().scheduleWithFixedDelay(this::onPrune, 1, 1, TimeUnit.MINUTES);
    private boolean closed;

    private void onPrune() {
        try {
            prune();
        } catch (RuntimeException | Error error) {
            SpiderDebug.log(error);
        }
    }

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.equals(API) || url.startsWith(API + "/");
    }

    @Override
    public Response doResponse(IHTTPSession request, String url, Map<String, String> files) {
        try {
            return handleRequest(request, url, files);
        } catch (UnsupportedOperationException e) {
            return error(url.equals(API + "/format") ? Status.NOT_IMPLEMENTED : Status.INTERNAL_ERROR, e);
        } catch (SecurityException e) {
            return error(Status.FORBIDDEN, e);
        } catch (ConcurrentModificationException | IllegalStateException e) {
            return error(Status.CONFLICT, e);
        } catch (IllegalArgumentException | JSONException e) {
            return error(Status.BAD_REQUEST, e);
        } catch (Exception e) {
            return error(Status.INTERNAL_ERROR, e);
        }
    }

    private Response handleRequest(IHTTPSession request, String url, Map<String, String> files) throws Exception {
        requireOrigin(request.getHeaders());
        Method method = request.getMethod();
        if (method != Method.GET && method != Method.POST && method != Method.DELETE) return response(Status.METHOD_NOT_ALLOWED, new JSONObject().put("error", "GET, POST or DELETE required"));
        if (method == Method.POST && !url.equals(API + "/upload")) {
            String type = request.getHeaders().get("content-type");
            if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) return response(Status.UNSUPPORTED_MEDIA_TYPE, new JSONObject().put("error", "application/json required"));
        }
        if (url.equals(API) && method == Method.POST) return response(Status.OK, create());
        Client client = require(request.getHeaders().get("x-spider-session"));
        if (url.equals(API) && method == Method.GET) return response(Status.OK, client.snapshot());
        if (url.equals(API) && method == Method.DELETE) {
            clients.remove(client.key, client);
            client.close();
            return response(Status.OK, new JSONObject());
        }
        if (url.equals(API + "/files") && method == Method.GET) return response(Status.OK, list(request.getParameters().getOrDefault("path", List.of(Path.rootPath())).get(0)));
        if (url.equals(API + "/versions") && method == Method.GET) return response(Status.OK, client.versions());
        if (method != Method.POST) return response(Status.METHOD_NOT_ALLOWED, new JSONObject().put("error", "POST required"));
        return response(Status.OK, executeRequest(client, request, url, files));
    }

    private JSONObject executeRequest(Client client, IHTTPSession request, String url, Map<String, String> files) throws Exception {
        JSONObject body = url.equals(API + "/upload") ? null : body(files);
        return switch (url.substring(API.length())) {
            case "/load" -> client.load(body);
            case "/spiders" -> client.spiders(body);
            case "/run" -> client.run(body);
            case "/format" -> formatPython(body);
            case "/play" -> client.play(body);
            case "/cancel" -> client.cancel();
            case "/file" -> client.select(resolve(body.getString("path")));
            case "/save" -> client.save(body);
            case "/checkpoint" -> client.checkpoint();
            case "/version" -> client.version(body);
            case "/restore" -> client.restore(body);
            case "/upload" -> client.upload(request.getParameters(), files);
            default -> throw new IllegalArgumentException("Unknown crawler request");
        };
    }

    private static JSONObject formatPython(JSONObject body) throws Exception {
        String text = body.getString("text");
        if (text.getBytes(StandardCharsets.UTF_8).length > SpiderSource.MAX_BYTES) throw new IllegalArgumentException("Source exceeds 2 MiB");
        return new JSONObject().put("text", RuntimeModule.PYTHON.entry(PythonRuntime.class).format(text));
    }

    private synchronized JSONObject create() throws Exception {
        if (closed) throw new IllegalStateException("Crawler service stopped");
        prune();
        if (clients.size() >= 8) throw new IllegalStateException("Too many crawler sessions");
        Client client = new Client();
        clients.put(client.key, client);
        return new JSONObject().put("session", client.key).put("root", Path.rootPath());
    }

    private Client require(String key) {
        Client client = key == null ? null : clients.get(key);
        if (client == null) throw new SecurityException("Crawler session expired. Reconnect.");
        client.access = SystemClock.elapsedRealtime();
        return client;
    }

    private void prune() {
        long now = SystemClock.elapsedRealtime();
        Throwable failure = null;
        for (Client client : clients.values()) if (now - client.access >= LIFETIME && clients.remove(client.key, client)) failure = close(client, failure);
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private static void requireOrigin(Map<String, String> headers) {
        String origin = headers.get("origin");
        String site = headers.get("sec-fetch-site");
        if ("cross-site".equals(site)) throw new SecurityException("Same origin required");
        if (origin == null) return;
        HttpUrl source = HttpUrl.parse(origin);
        String host = headers.get("host");
        HttpUrl target = host == null ? null : HttpUrl.parse("http://" + host);
        if (source == null || target == null || !source.equals(target)) throw new SecurityException("Same origin required");
    }

    private static JSONObject body(Map<String, String> files) throws Exception {
        String text = files.get("postData");
        if (text == null || text.length() > 3 * SpiderSource.MAX_BYTES) throw new IllegalArgumentException("Invalid request body");
        JSONTokener parser = new JSONTokener(text);
        Object value = parser.nextValue();
        if (!(value instanceof JSONObject object) || parser.nextClean() != 0) throw new IllegalArgumentException("JSON object required");
        return object;
    }

    private static File resolve(String path) throws IOException {
        File file = new File(path).getCanonicalFile();
        if (!file.toPath().startsWith(Path.root().getCanonicalFile().toPath())) throw new SecurityException("Choose a file in shared storage");
        return file;
    }

    private static JSONObject list(String path) throws Exception {
        File dir = resolve(path);
        if (!dir.isDirectory() || !dir.canRead()) throw new IOException("Cannot open folder. Check file permission.");
        JSONArray entries = new JSONArray();
        for (File file : Path.list(dir)) if (file.isDirectory() || supported(file.getName())) entries.put(new JSONObject().put("name", file.getName()).put("path", file.getAbsolutePath()).put("directory", file.isDirectory()));
        return new JSONObject().put("path", dir.getAbsolutePath()).put("parent", dir.equals(Path.root().getCanonicalFile()) ? JSONObject.NULL : dir.getParent()).put("entries", entries);
    }

    private static boolean supported(String name) {
        return name.endsWith(".py") || name.endsWith(".js") || name.endsWith(".java") || name.endsWith(".jar");
    }

    private static Response error(Status status, Exception error) {
        try {
            JSONObject body = new JSONObject().put("error", error.getMessage() == null ? error.toString() : error.getMessage());
            if (error instanceof ConcurrentModificationException) body.put("code", "source_changed");
            if (status == Status.NOT_IMPLEMENTED && error instanceof UnsupportedOperationException) body.put("code", "python_format_unavailable");
            return response(status, body);
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Response response(Status status, JSONObject body) {
        Response response = NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8", body.toString());
        response.addHeader("Cache-Control", "no-store");
        return response;
    }

    @Override
    public synchronized void close() {
        closed = true;
        pruning.cancel(false);
        Throwable failure = null;
        for (Client client : clients.values()) failure = close(client, failure);
        clients.clear();
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable close(Client client, Throwable failure) {
        try {
            client.close();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    static final class Client implements AutoCloseable {

        private static final int PROXY_PREVIEW_BYTES = 64 * 1024;
        private final String key = UUID.randomUUID().toString();
        private volatile long access = SystemClock.elapsedRealtime();
        private SpiderTestSession runtime;
        private SpiderSource source;
        private Future<?> task;
        private Future<?> deadline;
        private InputStream preview;
        private volatile int generation;
        private String state = "idle";
        private String output = "";
        private String method = "";
        private String api = "";
        private String ext = "";
        private String jar = "";
        private JSONArray spiders = new JSONArray();
        private long elapsed;
        private boolean closed;
        private JSONObject requests = new JSONObject();

        synchronized JSONObject snapshot() throws Exception {
            return new JSONObject().put("state", state).put("result", output).put("elapsed", elapsed).put("loaded", runtime != null && runtime.isLoaded()).put("api", api).put("ext", ext).put("jar", jar).put("spiders", spiders).put("requests", requests);
        }

        synchronized JSONObject load(JSONObject body) throws Exception {
            requireIdle();
            int timeout = timeout(body);
            if (body.getString("api").trim().endsWith(".java")) throw new IllegalArgumentException("Load a JAR to test Java crawlers");
            release();
            api = body.getString("api").trim();
            ext = body.optString("ext");
            jar = body.optString("jar").trim();
            String address = sourceUrl(api);
            String library = jar.startsWith("/") ? "file://" + jar : jar;
            String settings = ext;
            SpiderTestSession owner = runtime = new SpiderTestSession();
            int request = generation;
            state = "loading";
            requests = new JSONObject();
            output = "";
            long start = scheduleTimeout(timeout, request);
            task = Task.submitLarge(() -> {
                try {
                    owner.load(address, settings, library);
                    complete(request, "loaded", "", start);
                } catch (Throwable error) {
                    owner.close();
                    fail(request, error, start);
                }
            });
            return snapshot();
        }

        synchronized JSONObject spiders(JSONObject body) throws Exception {
            requireIdle();
            int timeout = timeout(body);
            release();
            jar = body.getString("jar").trim();
            String library = jar.startsWith("/") ? "file://" + jar : jar;
            SpiderTestSession owner = runtime = new SpiderTestSession();
            int request = generation;
            state = "loading";
            requests = new JSONObject();
            output = "";
            long start = scheduleTimeout(timeout, request);
            task = Task.submitLarge(() -> {
                try {
                    JSONArray names = owner.spiders(library);
                    completeSpiders(request, names, start);
                } catch (Throwable error) {
                    owner.close();
                    fail(request, error, start);
                }
            });
            return snapshot();
        }

        private synchronized void completeSpiders(int request, JSONArray names, long start) {
            if (closed || generation != request) return;
            spiders = names;
            complete(request, "done", "", start);
        }

        synchronized JSONObject run(JSONObject body) throws Exception {
            requireIdle();
            int timeout = timeout(body);
            if (runtime == null || !runtime.isLoaded()) throw new IllegalStateException("Load the crawler first");
            String method = this.method = body.getString("method");
            JSONArray args = body.getJSONArray("args");
            SpiderTestSession owner = runtime;
            int request = ++generation;
            state = "running";
            requests = new JSONObject();
            output = "";
            long start = scheduleTimeout(timeout, request);
            task = Task.submitLarge(() -> {
                try {
                    complete(request, "done", format(owner.call(method, args), request), start);
                } catch (Throwable error) {
                    fail(request, error, start);
                }
            });
            return snapshot();
        }

        synchronized JSONObject cancel() throws Exception {
            try {
                release();
            } finally {
                state = "cancelled";
            }
            return snapshot();
        }

        synchronized JSONObject play(JSONObject body) throws Exception {
            requireIdle();
            if (runtime == null || !runtime.isLoaded() || !state.equals("done") || !method.equals("play") || !output.equals(body.getString("result"))) throw new IllegalStateException("Run play successfully before sending its result");
            Result result = Result.fromJson(output);
            if (result.getUrl().isEmpty()) throw new IllegalArgumentException("Playback URL is empty");
            ServerEvent.push(runtime.preview(output));
            return new JSONObject();
        }

        synchronized JSONObject select(File file) throws Exception {
            requireIdle();
            if (!file.isFile() || !supported(file.getName())) throw new IllegalArgumentException("Choose a .py, .js, .java or .jar file");
            SpiderSource next = file.getName().endsWith(".jar") ? null : new SpiderSource(file);
            JSONObject body = next == null ? new JSONObject().put("name", file.getName()).put("path", file.getAbsolutePath()) : next.snapshot();
            release();
            source = next;
            state = "idle";
            return body;
        }

        synchronized JSONObject save(JSONObject body) throws Exception {
            requireIdle();
            if (source == null) throw new IllegalStateException("Open a source file first");
            JSONObject saved = source.save(body.getString("version"), body.getString("text"));
            release();
            state = "idle";
            return saved;
        }

        synchronized JSONObject versions() throws Exception {
            requireIdle();
            if (source == null) throw new IllegalStateException("Open a source file first");
            return new JSONObject().put("versions", source.versions());
        }

        synchronized JSONObject checkpoint() throws Exception {
            requireIdle();
            if (source == null) throw new IllegalStateException("Open a source file first");
            return new JSONObject().put("versions", source.checkpoint());
        }

        synchronized JSONObject version(JSONObject body) throws Exception {
            requireIdle();
            if (source == null) throw new IllegalStateException("Open a source file first");
            return source.version(body.getString("id"));
        }

        synchronized JSONObject restore(JSONObject body) throws Exception {
            requireIdle();
            if (source == null) throw new IllegalStateException("Open a source file first");
            JSONObject restored = source.restore(body.getString("path"), body.getString("version"), body.getString("id"));
            release();
            state = "idle";
            return restored;
        }

        private static int timeout(JSONObject body) throws Exception {
            Object value = body.opt("timeout");
            if (value == null) return 30;
            if (!(value instanceof Integer seconds) || seconds < 1 || seconds > 300) throw new IllegalArgumentException("Timeout must be 1 to 300 seconds");
            return seconds;
        }

        private long scheduleTimeout(int seconds, int request) {
            long start = SystemClock.elapsedRealtime();
            deadline = Task.scheduler().schedule(() -> expire(request, start), seconds, TimeUnit.SECONDS);
            return start;
        }

        private synchronized void expire(int request, long start) {
            if (closed || generation != request) return;
            try {
                release();
            } finally {
                state = "timedout";
                output = "Crawler request timed out";
                elapsed = SystemClock.elapsedRealtime() - start;
            }
        }

        synchronized JSONObject upload(Map<String, List<String>> params, Map<String, String> files) throws Exception {
            requireIdle();
            List<String> names = params.get("source");
            String name = names == null || names.isEmpty() ? null : names.get(0);
            String temporary = files.get("source");
            if (name == null || temporary == null || name.contains("/") || name.contains("\\") || !supported(name)) throw new IllegalArgumentException("Choose a .py, .js, .java or .jar file");
            File input = new File(temporary);
            long limit = name.endsWith(".jar") ? 64L * 1024 * 1024 : SpiderSource.MAX_BYTES;
            if (input.length() > limit) throw new IOException("Source is too large");
            File target = new File(new File(new File(Path.tv(), "spider-test"), key), name);
            Path.writeAtomically(target, input);
            return select(target);
        }

        private void requireIdle() {
            if (closed) throw new IllegalStateException("Crawler session ended");
            if (state.equals("loading") || state.equals("running")) throw new IllegalStateException("Cancel the current request first");
        }

        private synchronized void complete(int request, String value, String result, long start) {
            if (closed || generation != request) return;
            if (deadline != null) deadline.cancel(false);
            deadline = null;
            state = value;
            if (runtime != null) requests = runtime.requests();
            output = result;
            elapsed = SystemClock.elapsedRealtime() - start;
        }

        private void fail(int request, Throwable error, long start) {
            SpiderDebug.log(error);
            complete(request, "error", Log.getStackTraceString(error), start);
        }

        String format(Object value, int request) throws Exception {
            if (value instanceof Object[] proxy) return formatProxy(proxy, request);
            String text = String.valueOf(value);
            try {
                JSONTokener parser = new JSONTokener(text);
                Object json = parser.nextValue();
                if (parser.nextClean() != 0) return text;
                if (json instanceof JSONObject object) return object.toString(2);
                if (json instanceof JSONArray array) return array.toString(2);
            } catch (JSONException ignored) {
            }
            return text;
        }

        private String formatProxy(Object[] proxy, int request) throws Exception {
            if (proxy.length > 0 && proxy[0] instanceof Response response) proxy = new Object[]{response.getStatus().getRequestStatus(), response.getMimeType(), response.getData()};
            if (proxy.length < 3) throw new IllegalArgumentException("Invalid proxy response");
            Object body = proxy[2];
            byte[] bytes;
            if (body instanceof InputStream stream) bytes = readPreview(stream, request);
            else if (body == null) bytes = new byte[0];
            else throw new IllegalArgumentException("Invalid proxy body: expected InputStream or null");
            JSONObject output = new JSONObject().put("status", proxy[0]).put("mime", proxy[1]);
            output.put("bodyEncoding", "base64").put("body", Base64.encodeToString(Arrays.copyOf(bytes, Math.min(bytes.length, PROXY_PREVIEW_BYTES)), Base64.NO_WRAP));
            output.put("truncated", bytes.length > PROXY_PREVIEW_BYTES);
            if (proxy.length > 3) output.put("headers", proxy[3] instanceof Map<?, ?> headers ? new JSONObject(headers) : proxy[3]);
            return output.toString(2);
        }

        private byte[] readPreview(InputStream stream, int request) throws Exception {
            boolean current;
            synchronized (this) {
                current = generation == request;
                if (current) preview = stream;
            }
            if (!current) {
                CancellationException error = new CancellationException("Crawler test session has ended");
                closePreview(stream, error);
                throw error;
            }
            Throwable failure = null;
            try {
                return readPreviewBytes(stream);
            } catch (Exception | Error error) {
                failure = error;
                throw error;
            } finally {
                releasePreview(stream, failure);
            }
        }

        private void releasePreview(InputStream stream, Throwable failure) throws Exception {
            boolean close;
            synchronized (this) {
                close = preview == stream;
                if (close) preview = null;
            }
            if (close) closePreview(stream, failure);
        }

        private static byte[] readPreviewBytes(InputStream stream) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (output.size() <= PROXY_PREVIEW_BYTES) {
                int count = stream.read(buffer, 0, Math.min(buffer.length, PROXY_PREVIEW_BYTES + 1 - output.size()));
                if (count < 0) break;
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }

        private void closePreview(InputStream stream, Throwable failure) throws Exception {
            try {
                stream.close();
            } catch (Exception | Error error) {
                if (failure == null) throw error;
                if (failure != error) failure.addSuppressed(error);
            }
        }

        private static String sourceUrl(String api) {
            if (!api.startsWith("/") && !api.startsWith("file://")) return UrlUtil.convert(api);
            File file = api.startsWith("/") ? new File(api) : Path.local(api);
            String path = file.getAbsolutePath();
            String root = Path.rootPath() + File.separator;
            if (path.startsWith(root)) path = path.substring(root.length());
            return Server.get().getAddress("/file/") + android.net.Uri.encode(path, "/");
        }

        private void release() {
            generation++;
            if (deadline != null) deadline.cancel(false);
            deadline = null;
            spiders = new JSONArray();
            closePreviewLater();
            if (task != null) task.cancel(true);
            task = null;
            closeRuntime();
        }

        private void closePreviewLater() {
            InputStream current = preview;
            preview = null;
            if (current != null) Task.submitLarge(() -> {
                try {
                    current.close();
                } catch (Exception error) {
                    SpiderDebug.log(error);
                }
            });
        }

        private void closeRuntime() {
            SpiderTestSession owner = runtime;
            runtime = null;
            if (owner != null) {
                try {
                    owner.close();
                } finally {
                    requests = owner.requests();
                }
            }
        }

        @Override
        public synchronized void close() {
            closed = true;
            try {
                release();
            } finally {
                source = null;
            }
        }
    }
}
