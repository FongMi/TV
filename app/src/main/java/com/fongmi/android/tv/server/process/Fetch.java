package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.github.catvod.net.Download;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Task;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import okhttp3.Call;
import okhttp3.HttpUrl;

public final class Fetch implements Process, AutoCloseable {

    private final File root;
    private final Call.Factory client;
    private Job job;

    public Fetch() {
        this(Path.root(), OkHttp.client());
    }

    Fetch(File root, Call.Factory client) {
        this.root = root;
        this.client = client;
    }

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.equals("/download/start") || url.equals("/download/status") || url.equals("/download/pause") || url.equals("/download/resume") || url.equals("/download/cancel");
    }

    @Override
    public synchronized Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        if (session.getMethod() != Method.POST) return Nano.error(Status.METHOD_NOT_ALLOWED, "POST required");
        try {
            return handleRequest(session.getParms(), url);
        } catch (IllegalArgumentException | SecurityException e) {
            return Nano.error(Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private Response handleRequest(Map<String, String> params, String url) throws IOException {
        if (url.equals("/download/start")) {
            if (job != null && job.running) return Nano.error(Status.CONFLICT, "A download is already in progress");
            start(params);
        } else if (job == null) {
            return Nano.error(Status.NOT_FOUND, "Download not found");
        } else {
            if (!(url.equals("/download/status") && params.get("id") == null) && !job.id.equals(params.get("id"))) return Nano.error(Status.CONFLICT, "Download changed");
            updateJob(url);
        }
        Response response = Nano.ok(state(job).toString());
        response.addHeader("Cache-Control", "no-store");
        return response;
    }

    private void updateJob(String url) {
        if (url.equals("/download/pause")) {
            if (job.running) stop(job, "pausing");
        } else if (url.equals("/download/cancel")) {
            if (job.running) stop(job, "cancelling");
            else job.state = "cancelled";
        } else if (url.equals("/download/resume") && !job.running && (job.state.equals("paused") || job.state.equals("failed"))) {
            start(job);
        }
    }

    private void start(Map<String, String> params) throws IOException {
        if (params.get("url") == null) throw new IllegalArgumentException("Missing download URL");
        HttpUrl source = HttpUrl.get(params.get("url"));
        String name = params.get("name");
        if (name == null || name.isEmpty()) name = source.pathSegments().get(source.pathSegments().size() - 1);
        if (name.isEmpty()) name = "download";
        File target = Local.resolveTarget(root, params.get("path"), name);
        if (target.isDirectory()) throw new IllegalArgumentException("File name refers to a directory");
        job = new Job(source.toString(), target);
        start(job);
    }

    private void start(Job current) {
        File directory = new File(current.target.getParentFile(), ".tv-downloads");
        current.download = Download.create(current.url, current.target).directory(directory).allowEmpty(true);
        current.state = "downloading";
        current.error = null;
        current.speed = 0;
        current.running = true;
        Task.execute(() -> run(current));
    }

    private void run(Job current) {
        String result = "completed", error = null;
        try (Download download = current.download) {
            download(current, download);
        } catch (Exception e) {
            result = "failed";
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        } finally {
            finish(current, result, error);
        }
    }

    private void download(Job current, Download download) throws IOException {
        current.call = client.newCall(download.request(client));
        if (!current.state.equals("downloading")) current.call.cancel();
        download.write(current.call, current.call.execute(), (total, downloaded) -> onProgress(current, download, total, downloaded));
    }

    private synchronized void onProgress(Job current, Download download, long total, long downloaded) {
        current.total = total;
        current.downloaded = downloaded;
        current.speed = download.getSpeed();
    }

    private synchronized void finish(Job current, String result, String error) {
        if (result.equals("completed")) current.total = current.downloaded = current.target.length();
        if (!result.equals("completed") && current.state.equals("pausing")) result = "paused";
        else if (!result.equals("completed") && current.state.equals("cancelling")) result = "cancelled";
        current.state = result;
        current.error = result.equals("failed") ? error : null;
        current.running = false;
        current.call = null;
    }

    private void stop(Job current, String state) {
        current.state = state;
        current.download.cancel();
        Call call = current.call;
        if (call != null) call.cancel();
    }

    private JsonObject state(Job current) throws IOException {
        JsonObject state = new JsonObject();
        state.addProperty("id", current.id);
        state.addProperty("name", current.target.getName());
        String path = root.getCanonicalFile().toPath().relativize(current.target.getParentFile().toPath()).toString().replace(File.separatorChar, '/');
        state.addProperty("path", path.isEmpty() ? "" : "/" + path);
        state.addProperty("state", current.state);
        state.addProperty("total", current.total);
        state.addProperty("downloaded", current.downloaded);
        state.addProperty("speed", current.speed);
        state.addProperty("error", current.error);
        return state;
    }

    @Override
    public synchronized void close() {
        if (job != null && job.running) stop(job, "cancelling");
    }

    private static final class Job {

        final String id = UUID.randomUUID().toString();
        final String url;
        final File target;
        volatile String state;
        long total = -1;
        long downloaded;
        long speed;
        boolean running;
        String error;
        Download download;
        volatile Call call;

        Job(String url, File target) {
            this.url = url;
            this.target = target;
        }
    }
}
