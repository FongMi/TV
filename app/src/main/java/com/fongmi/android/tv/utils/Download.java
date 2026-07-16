package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import okhttp3.Response;

public class Download {

    private final File file;
    private final String url;
    private Callback callback;
    private Future<?> future;
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
        this.callback = callback;
        future = Task.submit(this::doInBackground);
    }

    public Download cancel() {
        if (future != null) future.cancel(true);
        OkHttp.cancel(tag);
        future = null;
        return this;
    }

    private void doInBackground() {
        try (Response res = OkHttp.newCall(url, tag).execute()) {
            download(res.body().byteStream(), getLength(res));
            if (callback != null) App.post(() -> callback.success(file));
        } catch (Exception e) {
            Path.clear(file);
            if (callback != null) App.post(() -> callback.error(e.getMessage()));
            else throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void download(InputStream is, double length) throws IOException {
        if (length > maxBytes) throw new IOException("Download size limit exceeded");
        try (BufferedInputStream input = new BufferedInputStream(is); FileOutputStream os = new FileOutputStream(Path.create(file))) {
            byte[] buffer = new byte[16384];
            int readBytes;
            long totalBytes = 0;
            while ((readBytes = input.read(buffer)) != -1) {
                if (Thread.interrupted()) return;
                totalBytes += readBytes;
                if (totalBytes > maxBytes) throw new IOException("Download size limit exceeded");
                os.write(buffer, 0, readBytes);
                if (length <= 0) continue;
                int progress = (int) (totalBytes / length * 100.0);
                if (callback != null) App.post(() -> callback.progress(progress));
            }
        }
    }

    private double getLength(Response res) {
        try {
            String header = res.header(HttpHeaders.CONTENT_LENGTH);
            return header != null ? Double.parseDouble(header) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public interface Callback {

        void progress(int progress);

        void error(String msg);

        void success(File file);
    }
}
