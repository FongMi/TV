package com.fongmi.android.tv.playback;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.C;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.UrlUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ExternalPlayback {

    public static final String FORWARD_RESULT = "com.fongmi.android.tv.external.FORWARD_RESULT";
    public static final int REQUEST_CODE = 1002;
    private static final String RETURN_RESULT = "com.fongmi.android.tv.external.RETURN_RESULT";
    private static final String HEADERS = "com.fongmi.android.tv.external.HEADERS";
    private static final String SUBTITLE = "com.fongmi.android.tv.external.SUBTITLE";
    private static final String POSITION = "com.fongmi.android.tv.external.POSITION";
    private static final String TITLE = "com.fongmi.android.tv.external.TITLE";

    private ExternalPlayback() {
    }

    public static void open(FragmentActivity activity, Intent source, Consumer<String> onLive) {
        Uri uri = getSourceUri(source);
        if (uri == null) {
            CharSequence text = source.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (TextUtils.isEmpty(text) && source.getClipData() != null && source.getClipData().getItemCount() > 0) text = source.getClipData().getItemAt(0).getText();
            if (!TextUtils.isEmpty(text)) openText(activity, text.toString(), source);
            else {
                Notify.show(R.string.error_play_url);
                if (source.getBooleanExtra(FORWARD_RESULT, false)) activity.finish();
            }
            return;
        }
        if (!FileChooser.isFileSource(uri)) {
            openText(activity, uri.toString(), source);
            return;
        }
        Runnable openFile = () -> FileChooser.getFileUri(uri, file -> {
            if (FileChooser.isLiveSource(new Intent(source).setDataAndType(file, source.getType()))) onLive.accept(UrlUtil.toLocalUrl(file));
            else start(activity, SiteApi.LOCAL, file.toString(), FileUtil.getDisplayName(file), source);
        }, () -> {
            if (source.getBooleanExtra(FORWARD_RESULT, false)) activity.finish();
        });
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) PermissionUtil.requestFile(activity, granted -> openFile.run());
        else openFile.run();
    }

    private static void openText(FragmentActivity activity, String text, Intent source) {
        String url = Sniffer.getUrl(text);
        start(activity, SiteApi.PUSH, url, url, source);
    }

    private static void start(FragmentActivity activity, String key, String id, String fallbackName, Intent source) {
        String title = getTitle(source);
        Intent player = new Intent(activity, VideoActivity.class);
        player.putExtra("key", key).putExtra("id", id).putExtra("name", TextUtils.isEmpty(title) ? fallbackName : title);
        if (!TextUtils.isEmpty(title)) player.putExtra(TITLE, title);
        player.putExtra(HEADERS, getHeaders(source));
        long position = getPosition(source);
        if (position >= 0) player.putExtra(POSITION, position);
        boolean forwardResult = source.getBooleanExtra(FORWARD_RESULT, false);
        player.putExtra(RETURN_RESULT, forwardResult && source.getBooleanExtra("return_result", false));
        String subtitle = getSubtitle(source);
        Uri subtitleUri = TextUtils.isEmpty(subtitle) ? null : UrlUtil.uri(subtitle);
        if (FileChooser.isFileSource(subtitleUri)) {
            FileChooser.getFileUri(subtitleUri, file -> launch(activity, player.putExtra(SUBTITLE, file.toString()), forwardResult), () -> launch(activity, player, forwardResult));
        } else {
            launch(activity, player.putExtra(SUBTITLE, subtitle), forwardResult);
        }
    }

    private static void launch(Activity activity, Intent player, boolean forwardResult) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (forwardResult) activity.startActivityForResult(player, REQUEST_CODE);
        else activity.startActivity(player);
    }

    public static Uri getSourceUri(Intent source) {
        if (Intent.ACTION_VIEW.equals(source.getAction()) && source.getData() != null) return source.getData();
        Object stream = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? source.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class) : source.getParcelableExtra(Intent.EXTRA_STREAM);
        if (stream instanceof Uri uri) return uri;
        if (source.getData() != null) return source.getData();
        ClipData clip = source.getClipData();
        return clip == null || clip.getItemCount() == 0 ? null : clip.getItemAt(0).getUri();
    }

    public static Uri getSubtitleUri(Intent source) {
        String value = getSubtitle(source);
        return TextUtils.isEmpty(value) ? null : UrlUtil.uri(value);
    }

    private static String getSubtitle(Intent source) {
        Bundle extras = source.getExtras();
        if (extras == null) return null;
        Object value = extras.get("subtitle");
        return value instanceof Uri || value instanceof String ? value.toString() : null;
    }

    private static String getTitle(Intent source) {
        CharSequence title = source.getCharSequenceExtra("title");
        if (TextUtils.isEmpty(title)) title = source.getCharSequenceExtra("name");
        if (TextUtils.isEmpty(title)) title = source.getCharSequenceExtra(Intent.EXTRA_TITLE);
        return title == null ? null : title.toString();
    }

    private static long getPosition(Intent source) {
        Bundle extras = source.getExtras();
        Object value = extras == null ? null : extras.get("position");
        long position = value instanceof Number number ? number.longValue() : C.TIME_UNSET;
        return position >= 0 ? position : C.TIME_UNSET;
    }

    private static Bundle getHeaders(Intent source) {
        Bundle result = new Bundle();
        Bundle extra = source.getBundleExtra("extra_headers");
        if (extra != null) for (String key : extra.keySet()) {
            String value = extra.getString(key);
            if (!TextUtils.isEmpty(key) && value != null) result.putString(key, value);
        }
        String[] pairs = source.getStringArrayExtra("headers");
        if (pairs != null) for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (!TextUtils.isEmpty(pairs[i]) && pairs[i + 1] != null) result.putString(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    public static void apply(Result result, Intent source) {
        Bundle headers = source.getBundleExtra(HEADERS);
        if (headers != null && !headers.isEmpty()) {
            Map<String, String> values = new HashMap<>();
            for (String key : headers.keySet()) values.put(key, headers.getString(key));
            result.addHeaders(values);
        }
        String subtitle = source.getStringExtra(SUBTITLE);
        if (!TextUtils.isEmpty(subtitle)) result.addSub(Sub.from(FileUtil.getDisplayName(UrlUtil.uri(subtitle)), subtitle));
    }

    public static void applyTitle(Result detail, Intent source) {
        String title = source.getStringExtra(TITLE);
        if (!TextUtils.isEmpty(title) && detail != null && !detail.getList().isEmpty()) detail.getVod().setName(title);
    }

    public static long takePosition(Intent source, long fallback) {
        long position = source.getLongExtra(POSITION, C.TIME_UNSET);
        source.removeExtra(POSITION);
        return position >= 0 ? position : fallback;
    }

    public static void clearOptions(Intent source) {
        source.removeExtra(HEADERS);
        source.removeExtra(SUBTITLE);
        source.removeExtra(POSITION);
        source.removeExtra(TITLE);
    }

    public static boolean returnsResult(Intent source) {
        return source.getBooleanExtra(RETURN_RESULT, false);
    }

    public static void setResult(Activity activity, long position, boolean completed) {
        int value = (int) Math.min(Integer.MAX_VALUE, Math.max(0, position));
        activity.setResult(Activity.RESULT_OK, new Intent().putExtra("position", value).putExtra("end_by", completed ? "playback_completion" : "user"));
    }
}
