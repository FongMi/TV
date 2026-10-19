package com.github.catvod.net;

import android.util.AtomicFile;

import com.github.catvod.Runtime;
import com.github.catvod.utils.Crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

final class NetStore {

    private static final long MAX_BYTES = 16 * 1024 * 1024;
    private static final Object LOCK = new Object();
    private final File directory;

    NetStore(String siteKey) {
        if (siteKey == null || siteKey.isEmpty()) throw new IllegalArgumentException("Cache requires a site key");
        directory = new File(Runtime.context().getCacheDir(), "spider/" + Crypto.sha256(siteKey));
    }

    String get(String key) {
        File file = file(key);
        synchronized (LOCK) {
            if (!file.isFile() && !new File(file.getPath() + ".bak").isFile()) return null;
            try {
                return new String(new AtomicFile(file).readFully(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    void set(String key, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        File file = file(key);
        synchronized (LOCK) {
            if (bytes.length > MAX_BYTES) {
                delete(file);
                return;
            }
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Cannot create cache directory");
            AtomicFile atomic = new AtomicFile(file);
            FileOutputStream output = null;
            try {
                output = atomic.startWrite();
                output.write(bytes);
                atomic.finishWrite(output);
            } catch (IOException e) {
                atomic.failWrite(output);
                throw new UncheckedIOException(e);
            }
            prune();
        }
    }

    void remove(String key) {
        synchronized (LOCK) {
            if (key != null) {
                File file = file(key);
                delete(file);
                delete(new File(file.getPath() + ".bak"));
                delete(new File(file.getPath() + ".new"));
            } else {
                File[] files = directory.listFiles();
                if (files != null) for (File file : files) delete(file);
            }
        }
    }

    private void prune() {
        File[] files = directory.listFiles((dir, name) -> !name.endsWith(".bak") && !name.endsWith(".new"));
        if (files == null) throw new IllegalStateException("Cannot read cache directory");
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        long bytes = 0;
        for (File file : files) bytes += file.length();
        for (int i = 0; i < files.length && (files.length - i > 64 || bytes > MAX_BYTES); i++) {
            bytes -= files[i].length();
            delete(files[i]);
        }
    }

    private File file(String key) {
        return new File(directory, Crypto.sha256(key));
    }

    private void delete(File file) {
        if (file.exists() && !file.delete()) throw new IllegalStateException("Cannot delete cache file");
    }
}
