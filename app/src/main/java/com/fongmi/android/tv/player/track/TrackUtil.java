package com.fongmi.android.tv.player.track;

import android.text.TextUtils;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;

import com.fongmi.android.tv.bean.Track;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class TrackUtil {

    public static String describeFormat(Format format) {
        StringJoiner joiner = new StringJoiner(",");
        if (format.id != null) joiner.add(format.id);
        if (format.label != null) joiner.add(format.label);
        if (format.codecs != null) joiner.add(format.codecs);
        if (format.language != null) joiner.add(format.language);
        if (format.sampleMimeType != null) joiner.add(format.sampleMimeType);
        if (format.containerMimeType != null) joiner.add(format.containerMimeType);
        if (format.width != C.LENGTH_UNSET) joiner.add(String.valueOf(format.width));
        if (format.height != C.LENGTH_UNSET) joiner.add(String.valueOf(format.height));
        if (format.sampleRate != C.RATE_UNSET_INT) joiner.add(String.valueOf(format.sampleRate));
        if (format.channelCount != C.LENGTH_UNSET) joiner.add(String.valueOf(format.channelCount));
        if (format.averageBitrate != C.LENGTH_UNSET) joiner.add(String.valueOf(format.averageBitrate));
        return joiner.toString();
    }

    public static String getSubtitleMimeType(String path) {
        if (TextUtils.isEmpty(path)) return "";
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".vtt")) return MimeTypes.TEXT_VTT;
        if (lower.endsWith(".ssa") || lower.endsWith(".ass")) return MimeTypes.TEXT_SSA;
        if (lower.endsWith(".ttml") || lower.endsWith(".xml") || lower.endsWith(".dfxp")) return MimeTypes.APPLICATION_TTML;
        return MimeTypes.APPLICATION_SUBRIP;
    }

    public static int count(Tracks tracks, int type) {
        return tracks.getGroups().stream().filter(trackGroup -> trackGroup.getType() == type).mapToInt(trackGroup -> trackGroup.length).sum();
    }

    public static void reset(Player player) {
        player.setTrackSelectionParameters(createResetBuilder(player).build());
    }

    private static TrackSelectionParameters.Builder createResetBuilder(Player player) {
        return player.getTrackSelectionParameters().buildUpon().clearOverrides().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false).setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
    }

    private static TrackInfo find(Player player, Track track) {
        if (track.getFormat() == null) return null;
        Tracks currentTracks = player.getCurrentTracks();
        for (Tracks.Group trackGroup : currentTracks.getGroups()) {
            if (trackGroup.getType() != track.getType()) continue;
            for (int i = 0; i < trackGroup.length; i++) {
                Format format = trackGroup.getTrackFormat(i);
                if (track.getFormat().equals(describeFormat(format))) {
                    return new TrackInfo(trackGroup, i);
                }
            }
        }
        return null;
    }

    public static void setTrackSelection(Player player, Track track) {
        applyTrackSelection(player, player.getTrackSelectionParameters().buildUpon(), List.of(track));
    }

    public static void setTrackSelection(Player player, List<Track> tracks) {
        applyTrackSelection(player, createResetBuilder(player), tracks);
    }

    private static void applyTrackSelection(Player player, TrackSelectionParameters.Builder builder, List<Track> tracks) {
        Map<Integer, TrackSelectionOverride> overridesByType = new HashMap<>();
        tracks.forEach(track -> putOverride(player, track, overridesByType));
        overridesByType.values().forEach(builder::setOverrideForType);
        player.setTrackSelectionParameters(builder.build());
    }

    private static void putOverride(Player player, Track track, Map<Integer, TrackSelectionOverride> overridesByType) {
        TrackInfo info = find(player, track);
        if (info == null) return;
        TrackSelectionOverride override = createOverride(track, info);
        if (track.isSelected()) overridesByType.put(override.getType(), override);
        else overridesByType.putIfAbsent(override.getType(), override);
    }

    private static TrackSelectionOverride createOverride(Track track, TrackInfo info) {
        TrackGroup group = info.trackGroup.getMediaTrackGroup();
        return track.isSelected() ? new TrackSelectionOverride(group, info.trackIndex) : new TrackSelectionOverride(group, List.of());
    }

    private record TrackInfo(Tracks.Group trackGroup, int trackIndex) {
    }
}
