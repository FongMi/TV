package com.fongmi.android.tv.player.subtitle;

import androidx.annotation.Nullable;
import androidx.media3.exoplayer.libass.LibassFontConfig;

import com.fongmi.android.tv.player.mpv.MpvConfigFile;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class AndroidFontConfig {

    private static final String FONTS_CONF = "fonts.conf";

    @Nullable
    public static synchronized File prepare() {
        File output = Path.mpv(FONTS_CONF);
        File temporary = new File(output.getParentFile(), UUID.randomUUID().toString());
        try {
            LibassFontConfig.write(temporary, Path.mpvCache(), ExternalFont.getDirectories(MpvConfigFile.getSubtitleFontsDirectory()));
            Path.move(temporary, output);
            return output;
        } catch (IOException | SecurityException e) {
            return null;
        } finally {
            Path.clear(temporary);
        }
    }
}
