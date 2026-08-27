package com.fongmi.android.tv.player.subtitle;

import android.util.Xml;

import androidx.annotation.Nullable;

import com.github.catvod.utils.Path;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class AndroidFontConfig {

    private static final String FONTS_CONF = "fonts.conf";

    @Nullable
    public static synchronized File prepare() {
        File output = Path.mpv(FONTS_CONF);
        if (Path.exists(output)) return output;
        try (FileOutputStream stream = new FileOutputStream(output, false)) {
            writeConfig(stream, Path.mpvCache());
            return output;
        } catch (IOException ignored) {
            Path.clear(output);
            return null;
        }
    }

    private static void writeConfig(OutputStream stream, File cacheDirectory) throws IOException {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(stream, StandardCharsets.UTF_8.name());
        serializer.startDocument(StandardCharsets.UTF_8.name(), true);
        serializer.startTag(null, "fontconfig");
        writeTextTag(serializer, "dir", "/system/fonts/");
        writeTextTag(serializer, "dir", "/product/fonts/");
        writeTextTag(serializer, "cachedir", cacheDirectory.getAbsolutePath());
        writeFontAlias(serializer, "serif", "Noto Serif");
        writeFontAlias(serializer, "sans-serif", "Roboto", "Noto Sans");
        writeFontAlias(serializer, "monospace", "Droid Sans Mono");
        serializer.endTag(null, "fontconfig");
        serializer.endDocument();
        serializer.flush();
    }

    private static void writeFontAlias(XmlSerializer serializer, String family, String... preferredFamilies) throws IOException {
        serializer.startTag(null, "alias");
        writeTextTag(serializer, "family", family);
        serializer.startTag(null, "prefer");
        for (String preferredFamily : preferredFamilies) writeTextTag(serializer, "family", preferredFamily);
        serializer.endTag(null, "prefer");
        serializer.endTag(null, "alias");
    }

    private static void writeTextTag(XmlSerializer serializer, String name, String value) throws IOException {
        serializer.startTag(null, name);
        serializer.text(value);
        serializer.endTag(null, name);
    }
}
