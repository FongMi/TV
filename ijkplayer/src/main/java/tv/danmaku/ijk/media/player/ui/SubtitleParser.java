package tv.danmaku.ijk.media.player.ui;

import android.text.Html;
import android.text.TextUtils;

import androidx.media3.common.text.Cue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SubtitleParser {

    private static final Pattern BRACES_PATTERN = Pattern.compile("\\{([^}]*)\\}");
    private static final String DIALOGUE_LINE_PREFIX = "Dialogue:";

    public static List<Cue> parse(String text) {
        if (TextUtils.isEmpty(text) || text.length() >= 512) return null;
        if (text.startsWith(DIALOGUE_LINE_PREFIX)) text = parseDialogueLine(text);
        text = text.replaceAll("\r\n", "<br>").replaceAll("\r", "<br>").replaceAll("\n", "<br>").replaceAll("\\{\\\\.*?\\}", "");
        if (text.endsWith("<br>")) text = text.substring(0, text.lastIndexOf("<br>"));
        return Arrays.asList(new Cue.Builder().setText(Html.fromHtml(text)).build());
    }

    private static String parseDialogueLine(String text) {
        String[] lineValues = text.substring(DIALOGUE_LINE_PREFIX.length()).split(",");
        String rawText = lineValues[lineValues.length - 1];
        rawText = BRACES_PATTERN.matcher(rawText).replaceAll("");
        rawText = rawText.replace("\\N", "\n").replace("\\n", "\n").replace("\\h", "\u00A0");
        return rawText;
    }
}
