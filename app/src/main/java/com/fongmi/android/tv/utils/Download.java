package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.Response;

public class Download {

    private final File file;
    private final String url;
    private final Callback callback;

    public static Download create(String url, File file) {
        return create(url, file, null);
    }

    public static Download create(String url, File file, Callback callback) {
        return new Download(url, file, callback);
    }

    public Download(String url, File file, Callback callback) {
        this.url = url;
        this.file = file;
        this.callback = callback;
    }

    public void start() {
        if (url.startsWith("file")) return;
        if (callback == null) doInBackground();
        else App.execute(this::doInBackground);
    }

    private void doInBackground() {
        try {
            Path.create(file);
            Response response = OkHttp.newCall(url).execute();
            download(response.body().byteStream(), Double.parseDouble(response.header(HttpHeaders.CONTENT_LENGTH, "1")));
            if (callback != null) App.post(() -> callback.success(file));
        } catch (Exception e) {
            if (callback != null) App.post(() -> callback.error(e.getMessage()));
        }
    }

    private void download(InputStream is, double length) throws Exception {
        FileOutputStream os = new FileOutputStream(file);
        try (BufferedInputStream input = new BufferedInputStream(is)) {
            byte[] buffer = new byte[4096];
            int readBytes;
            long totalBytes = 0;
            while ((readBytes = input.read(buffer)) != -1) {
                totalBytes += readBytes;
                os.write(buffer, 0, readBytes);
                int progress = (int) (totalBytes / length * 100.0);
                if (callback != null) App.post(() -> callback.progress(progress));
            }
        }
    }

    public interface Callback {

        void progress(int progress);

        void error(String msg);

        void success(File file);
    }
}
