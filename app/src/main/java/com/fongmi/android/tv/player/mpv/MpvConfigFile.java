package com.fongmi.android.tv.player.mpv;

import android.net.Uri;

import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MpvConfigFile {

    private static final String MPV_CONF = "mpv.conf";
    private static final char UTF_8_BOM = '\uFEFF';

    private static File file() {
        return Path.mpv(MPV_CONF);
    }

    public static String read() {
        return Path.read(file());
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
        Set<String> configured = getDefaultOptions(content);
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
