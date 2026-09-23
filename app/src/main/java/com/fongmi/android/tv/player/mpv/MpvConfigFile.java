package com.fongmi.android.tv.player.mpv;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.mpvplayer.MpvConfigFileInspector;

import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public final class MpvConfigFile {

    private static final String MPV_CONF = "mpv.conf";

    private static File file() {
        return Path.mpv(MPV_CONF);
    }

    private static MpvConfigFileInspector inspector() {
        return new MpvConfigFileInspector(Path.mpv());
    }

    public static String read() {
        return Path.read(file());
    }

    @Nullable
    public static File getSubtitleFontsDirectory() {
        return inspector().readSubtitleFontsDirectory();
    }

    public static boolean write(String content) {
        try {
            FileUtil.writeAtomically(content.getBytes(StandardCharsets.UTF_8), file());
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    public static List<String> findInterfaceManagedOptions(CharSequence content) {
        Set<String> configured = inspector().getDefaultOptionNames(content);
        return MpvUtil.getManagedOptionNames().stream().filter(option -> configured.contains(option) || configured.contains("no-" + option)).toList();
    }

    public static boolean importFrom(Uri uri) {
        try {
            FileUtil.copyAtomically(uri, file());
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }
}
