package com.fongmi.android.tv.utils;

import com.github.catvod.net.Download;
import com.github.catvod.utils.Path;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class ApkCache {

    static final String MODULE_DIRECTORY = "downloads/modules";
    static final Pattern APP = Pattern.compile("update-[0-9a-f]{64}\\.apk");
    private static final Pattern MODULE = Pattern.compile("[0-9]+-runtime-(python|nodejs|quickjs|thunder|mpv|bdj)-(arm64_v8a|armeabi_v7a)\\.apk");
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(7);

    public static File app() throws IOException {
        return directory(Path.cache(), "app");
    }

    public static File modules() throws IOException {
        File directory = directory(Path.cache(), "modules");
        if (!RuntimeInstaller.hasPendingSession()) clearModules(directory, null, System.currentTimeMillis() - MAX_AGE);
        return directory;
    }

    static synchronized File directory(File cache, String name) throws IOException {
        boolean app = name.equals("app");
        File directory = new File(cache, app ? "downloads/app" : MODULE_DIRECTORY);
        File legacy = new File(cache, app ? "updates" : "runtimes");
        Pattern pattern = app ? APP : MODULE;
        try (Closeable retained = Download.retain(directory, legacy)) {
            Files.createDirectories(directory.toPath());
            migratePackages(legacy, directory, pattern);
            migrateDownloads(legacy, directory, app);
            clearLegacy(cache, legacy, app);
        }
        return directory;
    }

    private static void migratePackages(File legacy, File directory, Pattern pattern) throws IOException {
        for (File file : Path.list(legacy)) {
            File target = new File(directory, file.getName());
            if (pattern.matcher(file.getName()).matches() && Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) && !target.exists()) Files.move(file.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
    }

    private static void migrateDownloads(File legacy, File directory, boolean app) throws IOException {
        File partials = new File(legacy, ".downloads");
        File target = new File(directory, ".downloads");
        if (!app && partials.isDirectory() && !target.exists()) Files.move(partials.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        Download.clearExpired(partials);
    }

    private static void clearLegacy(File cache, File legacy, boolean app) throws IOException {
        if (app) Download.clearExpired(new File(cache, ".downloads"));
        File[] files = legacy.listFiles();
        if (files != null && files.length == 0) Files.deleteIfExists(legacy.toPath());
    }

    static void installed(File directory, String id, String name) throws IOException {
        if (name != null && MODULE.matcher(name).matches() && name.contains("-runtime-" + id + "-")) {
            File file = new File(directory, name);
            if (Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) Files.deleteIfExists(file.toPath());
        }
        clearModules(directory, id, System.currentTimeMillis() - MAX_AGE);
    }

    static void clearModules(File directory, String id, long cutoff) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!MODULE.matcher(file.getName()).matches() || id != null && !file.getName().contains("-runtime-" + id + "-")) continue;
            if (Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) && file.lastModified() < cutoff) Download.clearCache(file);
        }
    }
}
