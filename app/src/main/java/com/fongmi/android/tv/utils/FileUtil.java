package com.fongmi.android.tv.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.impl.Callback;
import com.github.catvod.utils.Path;

import java.io.File;
import java.net.URLConnection;
import java.text.DecimalFormat;

public class FileUtil {

    public static File getWall(int index) {
        return Path.files("wallpaper_" + index);
    }

    public static void openFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(getShareUri(file), FileUtil.getMimeType(file.getName()));
        App.get().startActivity(intent);
    }

    public static void clearCache(Callback callback) {
        App.execute(() -> {
            Path.clear(Path.cache());
            App.post(callback::success);
        });
    }

    public static void getCacheSize(Callback callback) {
        App.execute(() -> {
            String result = byteCountToDisplaySize(getFolderSize(Path.cache()));
            App.post(() -> callback.success(result));
        });
    }

    private static Uri getShareUri(File file) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.N ? Uri.fromFile(file) : FileProvider.getUriForFile(App.get(), App.get().getPackageName() + ".provider", file);
    }

    private static String getMimeType(String fileName) {
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        return TextUtils.isEmpty(mimeType) ? "*/*" : mimeType;
    }

    private static long getFolderSize(File file) {
        long size = 0;
        if (file == null) return 0;
        if (file.isDirectory()) for (File f : Path.list(file)) size += getFolderSize(f);
        else size = file.length();
        return size;
    }

    private static String byteCountToDisplaySize(long size) {
        if (size <= 0) return "0 KB";
        String[] units = new String[]{"bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
