package com.github.catvod.utils;

import com.github.catvod.Runtime;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PackageAbi {

    private PackageAbi() {
    }

    public static String get() {
        return get(new File(Runtime.context().getApplicationInfo().sourceDir));
    }

    static String get(File file) {
        try (ZipFile apk = new ZipFile(file)) {
            boolean arm64 = hasAbi(apk, "arm64-v8a");
            boolean arm32 = hasAbi(apk, "armeabi-v7a");
            if (arm64 == arm32) throw new IllegalStateException("Expected one packaged ARM ABI in " + file);
            return arm64 ? "arm64-v8a" : "armeabi-v7a";
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read packaged ABI from " + file, e);
        }
    }

    private static boolean hasAbi(ZipFile apk, String abi) {
        String prefix = "lib/" + abi + "/";
        Enumeration<? extends ZipEntry> entries = apk.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix) && name.endsWith(".so") && name.indexOf('/', prefix.length()) < 0) return true;
        }
        return false;
    }
}
