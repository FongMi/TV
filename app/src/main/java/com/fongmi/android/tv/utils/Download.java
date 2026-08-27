package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Download {

    private static final int BUFFER_SIZE = 16 * 1024;

    private final String url;
    private final File file;

    private volatile Call call;
    private volatile boolean canceled;

    private Callback callback;
    private Future<?> future;
    private int lastProgress;
    private long maxBytes;
    private String tag;

    public static Download create(String url, File file) {
        return new Download(url, file);
    }

    public Download(String url, File file) {
        this.maxBytes = Long.MAX_VALUE;
        this.tag = url;
        this.url = url;
        this.file = file;
    }

    public Download tag(String tag) {
        this.tag = tag;
        return this;
    }

    public Download maxBytes(long maxBytes) {
        this.maxBytes = maxBytes > 0 ? maxBytes : Long.MAX_VALUE;
        return this;
    }

    public File get() {
        doInBackground();
        return file;
    }

    public void start(Callback callback) {
        this.lastProgress = -1;
        this.canceled = false;
        this.callback = callback;
        future = Task.submit(this::doInBackground);
    }

    public Download cancel() {
        canceled = true;
        if (future != null) future.cancel(true);
        Call request = call;
        if (request != null) request.cancel();
        OkHttp.cancel(tag);
        future = null;
        return this;
    }

    private void doInBackground() {
        Call request = null;
        try {
            request = createCall();
            execute(request);
            postCallback(listener -> listener.success(file));
        } catch (Exception error) {
            handleError(error);
        } finally {
            clearCall(request);
        }
    }

    private Call createCall() {
        Call request = OkHttp.newCall(url, tag);
        call = request;
        return request;
    }

    private void execute(Call request) throws IOException {
        checkCanceled();
        try (Response response = request.execute()) {
            checkCanceled();
            validateResponse(response);
            ResponseBody body = response.body();
            write(body.byteStream(), body.contentLength());
        }
    }

    private void validateResponse(Response response) throws IOException {
        if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " " + response.message());
    }

    private void write(InputStream stream, long length) throws IOException {
        checkSize(length);
        try (BufferedInputStream input = new BufferedInputStream(stream); FileOutputStream output = new FileOutputStream(Path.create(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int readBytes;
            long totalBytes = 0;
            while ((readBytes = input.read(buffer)) != -1) {
                checkCanceled();
                totalBytes += readBytes;
                checkSize(totalBytes);
                output.write(buffer, 0, readBytes);
                updateProgress(totalBytes, length);
            }
            if (totalBytes == 0) throw new IOException("Empty download");
        }
    }

    private void checkCanceled() throws InterruptedIOException {
        if (Thread.interrupted() || canceled) throw new InterruptedIOException();
    }

    private void checkSize(long size) throws IOException {
        if (size > maxBytes) throw new IOException("Download size limit exceeded");
    }

    private void updateProgress(long totalBytes, long length) {
        if (length <= 0) return;
        int progress = Math.min(100, (int) (totalBytes * 100.0 / length));
        if (progress == lastProgress) return;
        lastProgress = progress;
        postCallback(listener -> listener.progress(progress));
    }

    private void handleError(Exception error) {
        if (!canceled) Path.clear(file);
        if (callback == null) throw new RuntimeException(error.getMessage(), error);
        postCallback(listener -> listener.error(error.getMessage()));
    }

    private void clearCall(Call request) {
        if (call == request) call = null;
    }

    private void postCallback(Consumer<Callback> action) {
        Callback current = callback;
        if (current == null || canceled) return;
        App.post(() -> {
            if (!canceled && callback == current) action.accept(current);
        });
    }

    public interface Callback {

        default void progress(int progress) {
        }

        void error(String msg);

        void success(File file);
    }
}
