package com.fongmi.android.tv.player.media;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ConcatenatingUrl {

    public static boolean matches(String url) {
        return url != null && url.contains("***") && url.contains("|||");
    }

    public static List<Segment> parse(String url) {
        if (!matches(url)) return List.of();
        List<Segment> segments = new ArrayList<>();
        long durationMs = 0;
        for (String entry : url.split("\\*\\*\\*", -1)) {
            Segment segment = parseSegment(entry, durationMs);
            durationMs += segment.durationMs();
            segments.add(segment);
        }
        return List.copyOf(segments);
    }

    private static Segment parseSegment(String entry, long durationMs) {
        int separator = entry.lastIndexOf("|||");
        if (separator <= 0) throw new IllegalArgumentException("Missing segment URL or duration");
        String source = entry.substring(0, separator).trim();
        long lengthMs = Long.parseLong(entry.substring(separator + 3).trim());
        if (source.isEmpty() || lengthMs <= 0 || lengthMs > Long.MAX_VALUE / 1000 - durationMs) throw new IllegalArgumentException("Invalid segment URL or duration");
        return new Segment(source, lengthMs);
    }

    public static String toEdl(String url) {
        List<Segment> segments = parse(url);
        if (segments.isEmpty()) return url;
        StringBuilder edl = new StringBuilder();
        for (Segment segment : segments) appendSegment(edl, segment);
        return "edl://" + edl;
    }

    private static void appendSegment(StringBuilder edl, Segment segment) {
        if (edl.length() > 0) edl.append(';');
        String source = segment.url();
        edl.append('%').append(source.getBytes(StandardCharsets.UTF_8).length).append('%').append(source);
        edl.append(String.format(Locale.ROOT, ",0,%d.%03d", segment.durationMs() / 1000, segment.durationMs() % 1000));
    }

    public record Segment(String url, long durationMs) {
    }
}
