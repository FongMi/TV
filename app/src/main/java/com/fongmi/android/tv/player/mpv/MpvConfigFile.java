package com.fongmi.android.tv.player.mpv;

import android.content.Context;
import android.net.Uri;

import com.github.catvod.utils.Path;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class MpvConfigFile {

    private static final String MPV_CONF = "mpv.conf";

    public static File file() {
        return Path.mpv(MPV_CONF);
    }

    public static String read() {
        return Path.read(file());
    }

    public static void write(String content) {
        String value = content == null ? "" : content;
        Path.write(file(), value.getBytes(StandardCharsets.UTF_8));
    }

    public static void importFrom(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            Path.write(file(), in);
        } catch (Exception ignored) {
        }
    }
}
