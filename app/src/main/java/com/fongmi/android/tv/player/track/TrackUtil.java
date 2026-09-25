package com.fongmi.android.tv.player.track;

import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;

import com.fongmi.android.tv.bean.Track;

import java.util.ArrayList;
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
        if (format.sampleRate != Format.NO_VALUE) joiner.add(String.valueOf(format.sampleRate));
        if (format.channelCount != C.LENGTH_UNSET) joiner.add(String.valueOf(format.channelCount));
        if (format.bitrate != Format.NO_VALUE) joiner.add(String.valueOf(format.bitrate));
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

    public static Track createTrack(int type, String name, Format format, int ordinal) {
        Track track = new Track(type, name, describeFormat(format));
        track.setLabel(format.label == null ? null : format.label.toString());
        track.setLanguage(format.language);
        track.setMimeType(format.sampleMimeType);
        track.setOrdinal(ordinal);
        return track;
    }

    @Nullable
    public static Track fromSelection(Tracks tracks, TrackSelectionOverride selection, int role) {
        if (selection.trackIndices.size() != 1) return null;
        int selectionIndex = selection.trackIndices.get(0);
        int ordinal = 0;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != selection.getType()) continue;
            for (int i = 0; i < group.length; i++) {
                if (group.getMediaTrackGroup().equals(selection.mediaTrackGroup) && i == selectionIndex) {
                    Format format = group.getTrackFormat(i);
                    String name = format.label == null ? "" : format.label.toString();
                    Track track = createTrack(group.getType(), name, format, ordinal);
                    track.setRole(role);
                    track.setSelected(true);
                    return track;
                }
                ordinal++;
            }
        }
        return null;
    }

    public static void reset(Player player) {
        player.setTrackSelectionParameters(createResetBuilder(player).build());
    }

    private static TrackSelectionParameters.Builder createResetBuilder(Player player) {
        return player.getTrackSelectionParameters().buildUpon().clearOverrides().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false).setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false);
    }

    @Nullable
    public static TrackSelectionOverride findSelection(Player player, Track track) {
        return findSelection(player.getCurrentTracks(), track);
    }

    @Nullable
    public static TrackSelectionOverride findSelection(Tracks tracks, Track track) {
        TrackInfo info = find(tracks, track);
        return info == null ? null : new TrackSelectionOverride(info.trackGroup.getMediaTrackGroup(), info.trackIndex);
    }

    @Nullable
    private static TrackInfo find(Tracks tracks, Track track) {
        TrackInfo exactMatch = null;
        TrackInfo ordinalMatch = null;
        TrackInfo semanticMatch = null;
        int bestScore = 0;
        int bestDistance = Integer.MAX_VALUE;
        int ordinal = 0;
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() != track.getType()) continue;
            for (int i = 0; i < trackGroup.length; i++) {
                Format format = trackGroup.getTrackFormat(i);
                TrackInfo candidate = new TrackInfo(trackGroup, i);
                if (track.getFormat() != null && track.getFormat().equals(describeFormat(format))) {
                    if (ordinal == track.getOrdinal()) return candidate;
                    if (exactMatch == null) exactMatch = candidate;
                }
                if (ordinal == track.getOrdinal()) ordinalMatch = candidate;
                if (track.getType() == C.TRACK_TYPE_TEXT) {
                    int score = getMatchScore(track, format);
                    int distance = Math.abs(ordinal - track.getOrdinal());
                    if (score > bestScore || (score == bestScore && score > 0 && distance < bestDistance)) {
                        semanticMatch = candidate;
                        bestScore = score;
                        bestDistance = distance;
                    }
                }
                ordinal++;
            }
        }
        if (exactMatch != null) return exactMatch;
        if (semanticMatch != null) return semanticMatch;
        if (normalize(track.getLabel()) != null || normalizeLanguage(track.getLanguage()) != null) return null;
        return ordinalMatch;
    }

    private static int getMatchScore(Track track, Format format) {
        String label = normalize(track.getLabel());
        String language = normalizeLanguage(track.getLanguage());
        String mimeType = normalize(track.getMimeType());
        String candidateLanguage = normalizeLanguage(format.language);
        boolean labelMatches = label != null && label.equals(normalize(format.label));
        boolean languageMatches = language != null && language.equals(candidateLanguage);
        if (language != null && candidateLanguage != null && !languageMatches) return 0;
        boolean mimeTypeMatches = mimeType != null && mimeType.equals(normalize(format.sampleMimeType));
        if (languageMatches) return 4 + (labelMatches ? 2 : 0) + (mimeTypeMatches ? 1 : 0);
        if (labelMatches) return 2 + (mimeTypeMatches ? 1 : 0);
        if (label == null && language == null && mimeTypeMatches) return 1;
        return 0;
    }

    @Nullable
    private static String normalize(@Nullable CharSequence value) {
        if (value == null) return null;
        String normalized = value.toString().trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static String normalizeLanguage(@Nullable String language) {
        String normalized = normalize(language);
        return C.LANGUAGE_UNDETERMINED.equals(normalized) ? null : normalized;
    }

    public static boolean setTrackSelection(Player player, Track track) {
        TrackSelectionOverride override = findRestorationOverride(player.getCurrentTracks(), track);
        if (override == null) return false;
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(override).build());
        return true;
    }

    @Nullable
    private static TrackSelectionOverride findRestorationOverride(Tracks tracks, Track track) {
        if (track.isSecondary()) return null;
        TrackInfo info = find(tracks, track);
        if (info != null) return createOverride(track, info);
        if (track.isSelected() || track.getType() != C.TRACK_TYPE_TEXT) return null;
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_TEXT) return new TrackSelectionOverride(group.getMediaTrackGroup(), List.of());
        }
        return null;
    }

    public static List<Track> setTrackSelection(Player player, List<Track> tracks) {
        TrackSelectionParameters.Builder builder = createResetBuilder(player);
        Map<Integer, TrackSelectionOverride> overridesByType = new HashMap<>();
        List<Track> pending = new ArrayList<>();
        Tracks currentTracks = player.getCurrentTracks();
        for (Track track : tracks) {
            if (track.isSecondary()) continue;
            TrackSelectionOverride override = findRestorationOverride(currentTracks, track);
            if (override == null) {
                pending.add(track);
                continue;
            }
            if (track.isSelected()) overridesByType.put(override.getType(), override);
            else overridesByType.putIfAbsent(override.getType(), override);
        }
        overridesByType.values().forEach(builder::setOverrideForType);
        player.setTrackSelectionParameters(builder.build());
        return pending;
    }

    private static TrackSelectionOverride createOverride(Track track, TrackInfo info) {
        TrackGroup group = info.trackGroup.getMediaTrackGroup();
        return track.isSelected() ? new TrackSelectionOverride(group, info.trackIndex) : new TrackSelectionOverride(group, List.of());
    }

    private record TrackInfo(Tracks.Group trackGroup, int trackIndex) {
    }
}
