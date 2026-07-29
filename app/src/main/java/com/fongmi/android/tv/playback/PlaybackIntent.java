package com.fongmi.android.tv.playback;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

public final class PlaybackIntent {

    public static void share(Activity activity, String url, Map<String, String> headers, CharSequence title) {
        try {
            if (url == null || url.isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, url);
            intent.putExtra("extra_headers", toBundle(headers));
            intent.putExtra("title", title).putExtra("name", title);
            intent.setType("text/plain");
            activity.startActivity(getChooser(intent));
        } catch (Exception ignored) {
        }
    }

    public static void choose(Activity activity, String url, Map<String, String> headers, boolean isVod, long position, CharSequence title) {
        try {
            if (url == null || url.isEmpty()) return;
            List<String> list = new ArrayList<>();
            headers.forEach((key, value) -> {
                list.add(key);
                list.add(value);
            });
            Uri data = url.startsWith("file://") || url.startsWith("/") ? FileUtil.getShareUri(url) : Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(data, "video/*");
            intent.putExtra("title", title).putExtra("return_result", isVod);
            intent.putExtra("headers", list.toArray(String[]::new));
            if (isVod) intent.putExtra("position", (int) position);
            activity.startActivityForResult(getChooser(intent), 1001);
        } catch (Exception ignored) {
        }
    }

    public static void onExternalResult(Intent data, Runnable onNext, LongConsumer seekTo) {
        try {
            if (data == null || data.getExtras() == null) return;
            long position = data.getExtras().getInt("position", 0);
            String endBy = data.getExtras().getString("end_by", "");
            if ("playback_completion".equals(endBy)) App.post(onNext);
            if ("user".equals(endBy)) seekTo.accept(position);
        } catch (Exception ignored) {
        }
    }

    private static Bundle toBundle(Map<String, String> headers) {
        Bundle bundle = new Bundle();
        if (headers != null) headers.forEach(bundle::putString);
        return bundle;
    }

    private static Intent getChooser(Intent intent) {
        List<ComponentName> components = new ArrayList<>();
        for (ResolveInfo resolveInfo : App.get().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            String pkgName = resolveInfo.activityInfo.packageName;
            if (pkgName.equals(App.get().getPackageName())) components.add(new ComponentName(pkgName, resolveInfo.activityInfo.name));
        }
        return Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, components.toArray(new ComponentName[0]));
    }
}
