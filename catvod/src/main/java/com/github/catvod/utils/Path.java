package com.github.catvod.utils;

import android.os.Environment;
import android.os.StatFs;
import android.system.ErrnoException;
import android.system.Os;

import com.github.catvod.Init;
import com.orhanobut.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path {

    private static final String TAG = Path.class.getSimpleName();

    private static File mkdir(File file) {
        if (file == null || file.exists()) return file;
        if (file.mkdirs()) Logger.t(TAG).d("Created dir:" + file);
        return file;
    }

    public static boolean exists(String path) {
        return new File(path.replace("file://", "")).exists();
    }

    public static boolean exists(File file) {
        return file != null && file.exists() && file.length() > 0;
    }

    public static File root() {
        return Environment.getExternalStorageDirectory();
    }

    public static File cache() {
        return Init.context().getCacheDir();
    }

    public static File files() {
        return Init.context().getFilesDir();
    }

    public static String rootPath() {
        return root().getAbsolutePath();
    }

    public static File tv() {
        return mkdir(new File(root(), "TV"));
    }

    public static File backup() {
        return mkdir(new File(tv(), "backup"));
    }

    public static File font() {
        return mkdir(new File(tv(), "fonts"));
    }

    public static File wall(int index) {
        return files("wallpaper_" + index);
    }

    public static File wallCache() {
        return files("wallpaper_cache");
    }

    public static File so() {
        return mkdir(new File(files(), "so"));
    }

    public static File js() {
        return mkdir(new File(cache(), "js"));
    }

    public static File py() {
        return mkdir(new File(cache(), "py"));
    }

    public static File jar() {
        return mkdir(new File(cache(), "jar"));
    }

    public static File exoCache() {
        return mkdir(new File(cache(), "exo"));
    }

    public static File mpvCache() {
        return mkdir(new File(cache(), "mpv"));
    }

    public static File mpv() {
        return mkdir(new File(tv(), "mpv"));
    }

    public static File epg() {
        return mkdir(new File(cache(), "epg"));
    }

    public static File jpa() {
        return mkdir(new File(cache(), "jpa"));
    }

    public static File thunder() {
        return mkdir(new File(cache(), "thunder"));
    }

    public static File root(String name) {
        return new File(root(), name);
    }

    public static File root(String child, String name) {
        return new File(mkdir(new File(root(), child)), name);
    }

    public static File cache(String name) {
        return new File(cache(), name);
    }

    public static File files(String name) {
        return new File(files(), name);
    }

    public static File mpv(String name) {
        return new File(mpv(), name);
    }

    public static File epg(String name) {
        return new File(epg(), name);
    }

    public static File js(String name) {
        return new File(js(), name);
    }

    public static File py(String name) {
        return new File(py(), name);
    }

    public static File jar(String name) {
        return new File(jar(), Crypto.md5(name).concat(".jar"));
    }

    public static File thunder(String name) {
        return mkdir(new File(thunder(), name));
    }

    public static File local(String path) {
        path = path.replace("file:/", "");
        File file = new File(root(), path);
        return file.exists() ? file : new File(path);
    }

    public static String read(File file) {
        try {
            return new String(readToByte(file), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String read(InputStream is) {
        try {
            return new String(readToByte(is), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public static byte[] readToByte(File file) {
        try (FileInputStream is = new FileInputStream(file)) {
            return readToByte(is);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static byte[] readToByte(InputStream is) throws IOException {
        try (InputStream input = is; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int read;
            byte[] buffer = new byte[16384];
            while ((read = input.read(buffer)) != -1) bos.write(buffer, 0, read);
            return bos.toByteArray();
        }
    }

    public static File write(File file, InputStream is) {
        try (InputStream input = is; FileOutputStream output = new FileOutputStream(create(file))) {
            int read;
            byte[] buffer = new byte[16384];
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return file;
        } catch (IOException e) {
            return file;
        }
    }

    public static File write(File file, byte[] data) {
        try (FileOutputStream fos = new FileOutputStream(create(file))) {
            fos.write(data);
            fos.flush();
            return file;
        } catch (IOException e) {
            return file;
        }
    }

    public static void copy(File in, File out) {
        try {
            copyOrThrow(in, out);
        } catch (IOException ignored) {
        }
    }

    public static void copy(InputStream in, File out) {
        try {
            copyOrThrow(in, out);
        } catch (IOException ignored) {
        }
    }

    public static void move(File source, File target) throws IOException {
        try {
            Os.rename(source.getAbsolutePath(), target.getAbsolutePath());
        } catch (ErrnoException e) {
            throw new IOException("Unable to move file", e);
        }
    }

    public static long size(File file) {
        long total = 0;
        if (file == null) return total;
        if (file.isDirectory()) for (File child : list(file)) total += size(child);
        else total = file.length();
        return total;
    }

    public static long available(File file) {
        try {
            StatFs stat = new StatFs(file.getAbsolutePath());
            return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        } catch (Exception e) {
            return 0;
        }
    }

    private static void copyOrThrow(File in, File out) throws IOException {
        if (!in.getCanonicalFile().equals(out.getCanonicalFile())) copyOrThrow(new FileInputStream(in), out);
    }

    private static void copyOrThrow(InputStream in, File out) throws IOException {
        try (InputStream input = in; FileOutputStream output = new FileOutputStream(create(out))) {
            int read;
            byte[] buffer = new byte[16384];
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        }
    }

    public static void sort(File[] files) {
        Arrays.sort(files, (o1, o2) -> {
            if (o1.isDirectory() && o2.isFile()) return -1;
            if (o1.isFile() && o2.isDirectory()) return 1;
            return o1.getName().compareToIgnoreCase(o2.getName());
        });
    }

    public static List<File> list(File dir) {
        File[] files = dir.listFiles();
        if (files != null) sort(files);
        return files == null ? new ArrayList<>() : Arrays.asList(files);
    }

    public static void clear(File dir) {
        if (dir == null) return;
        if (dir.isDirectory()) for (File file : list(dir)) clear(file);
        if (dir.delete()) Logger.t(TAG).d("Deleted:" + dir);
    }

    public static File create(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null) mkdir(parent);
            if (file.exists()) clear(file);
            if (file.createNewFile()) Logger.t(TAG).d("Create:" + file);
            file.setReadable(true);
            file.setWritable(true);
            file.setExecutable(true);
            Shell.exec("chmod 777 " + file);
            return file;
        } catch (IOException e) {
            return file;
        }
    }
}
