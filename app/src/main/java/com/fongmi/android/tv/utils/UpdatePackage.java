package com.fongmi.android.tv.utils;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.core.content.pm.PackageInfoCompat;

import com.fongmi.android.tv.BuildConfig;
import com.github.catvod.net.Download;
import com.github.catvod.utils.Crypto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public record UpdatePackage(String version, String url, long size, String digest) {

    private static final Pattern SHA256 = Pattern.compile("sha256:[0-9a-fA-F]{64}");

    public UpdatePackage {
        if (version == null || version.isEmpty() || url == null || url.isEmpty() || size <= 0) throw new IllegalArgumentException("Invalid update asset");
        if (digest == null) digest = "";
        if (!digest.isEmpty() && !SHA256.matcher(digest).matches()) throw new IllegalArgumentException("Invalid update digest");
    }

    public void verifyFile(File file) throws IOException {
        if (!file.isFile() || file.length() != size) throw new IOException("Update size mismatch");
        if (digest.isEmpty()) return;
        byte[] expected = new byte[32];
        for (int i = 0; i < expected.length; i++) {
            int offset = 7 + i * 2;
            expected[i] = (byte) (Character.digit(digest.charAt(offset), 16) * 16 + Character.digit(digest.charAt(offset + 1), 16));
        }
        if (!MessageDigest.isEqual(expected, Crypto.sha256(file))) throw new IOException("Update checksum mismatch");
    }

    public File file(File directory) throws IOException {
        Files.createDirectories(directory.toPath());
        File target = new File(directory, "update-" + Crypto.sha256(url + "\n" + version + "\n" + size + "\n" + digest) + ".apk");
        File[] expired = directory.listFiles(file -> ApkCache.APP.matcher(file.getName()).matches() && Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS));
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        if (expired != null) for (File file : expired) if (!file.equals(target) && file.lastModified() < cutoff) Download.clearCache(file);
        return target;
    }

    public void verifyApk(Context context, File file) throws IOException {
        verifyFile(file);
        PackageInfo info = context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
        if (info == null) throw new IOException("Invalid update APK");
        verifyPackage(info.packageName, info.versionName, PackageInfoCompat.getLongVersionCode(info));
    }

    public void verifyPackage(String packageName, String versionName, long versionCode) throws IOException {
        if (!BuildConfig.APPLICATION_ID.equals(packageName)) throw new IOException("Update package mismatch");
        if (!version.equals(versionName) || versionCode <= BuildConfig.VERSION_CODE) throw new IOException("Update version mismatch");
    }
}
