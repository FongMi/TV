package com.fongmi.android.tv.player.mpv;

import android.net.Uri;

import com.github.catvod.utils.Path;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MpvConfigFile {

    private static final String MPV_CONF = "mpv.conf";
    private static final char UTF_8_BOM = '\uFEFF';

    private static final List<String> INTERFACE_MANAGED_OPTIONS = List.of(
            "vo",
            "gpu-api",
            "gpu-context",
            "hwdec",
            "audio-spdif",
            "android-dolby-vision-output",
            "demuxer-dovi-profile7",
            "cache",
            "cache-on-disk",
            "demuxer-cache-dir",
            "cache-secs",
            "sub-font",
            "sub-fonts-dir",
            "sub-ass-style-overrides",
            "embeddedfonts",
            "sub-color",
            "sub-back-color",
            "sub-border-style",
            "sub-outline-color",
            "sub-outline-size",
            "sub-shadow-offset",
            "secondary-sub-ass-override",
            "sub-ass-override",
            "sub-pos",
            "sub-scale",
            "sub-scale-signs",
            "secondary-sub-pos",
            "secondary-sid"
    );

    private static File file() {
        return Path.mpv(MPV_CONF);
    }

    public static String read() {
        return Path.read(file());
    }

    public static String read(Uri uri) throws IOException {
        try (InputStream input = Path.open(uri, "Unable to open mpv.conf")) {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder());
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) content.append(buffer, 0, count);
            return content.toString();
        }
    }

    public static boolean write(String content) {
        try {
            Path.writeAtomically(file(), content.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    public static List<String> findInterfaceManagedOptions(CharSequence content) {
        Set<String> configured = getDefaultOptions(content);
        return INTERFACE_MANAGED_OPTIONS.stream().filter(option -> configured.contains(option) || configured.contains("no-" + option)).toList();
    }

    private static Set<String> getDefaultOptions(CharSequence content) {
        Set<String> options = new HashSet<>();
        boolean inDefaultProfile = true;
        for (String line : removeBom(content.toString()).split("[\\r\\n]+")) {
            String profile = getProfileName(line);
            if (profile != null) {
                inDefaultProfile = profile.isEmpty() || "default".equals(profile);
                continue;
            }
            if (!inDefaultProfile) continue;
            String option = getOptionName(line);
            if (!option.isEmpty()) options.add(option);
        }
        return options;
    }

    private static String getOptionName(String line) {
        String value = line.trim();
        if (value.isEmpty() || value.startsWith("#")) return "";
        if (value.startsWith("--")) value = value.substring(2);
        int end = 0;
        while (end < value.length() && isOptionNameCharacter(value.charAt(end))) end++;
        String trailing = value.substring(end).trim();
        if (!trailing.startsWith("=") && hasTrailingContent(trailing)) return "";
        return value.substring(0, end);
    }

    private static boolean isOptionNameCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-';
    }

    private static String getProfileName(String line) {
        String value = line.trim();
        if (!value.startsWith("[")) return null;
        int end = value.indexOf(']');
        if (end < 0 || hasTrailingContent(value.substring(end + 1))) return null;
        return value.substring(1, end);
    }

    private static boolean hasTrailingContent(String value) {
        String trailing = value.trim();
        return !trailing.isEmpty() && !trailing.startsWith("#");
    }

    private static String removeBom(String value) {
        return !value.isEmpty() && value.charAt(0) == UTF_8_BOM ? value.substring(1) : value;
    }
}
