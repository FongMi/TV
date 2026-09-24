package com.fongmi.android.tv.player.engine;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.media.PlaySpec;

import java.util.List;

public interface PlayerEngine {

    Type getType();

    default boolean needsRebuild() {
        return false;
    }

    Player getPlayer();

    int getAudioChannelCount();

    void release();

    default List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return List.of();
    }

    @Nullable
    default DecoderMode getDecoderMode(@C.TrackType int trackType) {
        return null;
    }

    default void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
    }

    default void resetDecoderFallback(@C.TrackType int trackType) {
    }

    /** Restores persistent engine policy before loading a new playback request. */
    default void prepareForNewMedia() {
    }

    /** Selects an untried decoder; the caller restarts after a playback error. */
    default boolean switchDecoderForRetry(PlaybackException exception) {
        return false;
    }

    default PlayerEffect getEffect() {
        return PlayerEffect.NONE;
    }

    void start(PlaySpec spec, long startPositionMs);

    default void preload(PlaySpec spec, long startPositionMs) {
    }

    default void clearPreload() {
    }

    default void bindPlayerView(PlayerView playerView) {
    }

    void stop();

    default void applySubtitleStyle() {
    }

    default SubtitleSelectionState getSubtitleSelectionState() {
        return SubtitleSelectionState.EMPTY;
    }

    default SubtitleContent getSubtitleContent(TrackSelectionOverride selection) {
        return SubtitleContent.UNSUPPORTED;
    }

    default void setSubtitleContentEnabled(boolean enabled) {
    }

    default boolean canRetrySubtitleContent(Format format) {
        return false;
    }

    default void retrySubtitleContent(Format format) {
    }

    default boolean supportsSubtitleTranscript() {
        return false;
    }

    default SubtitleOffsets getSubtitleOffsets() {
        long offset = getPlayer().isCommandAvailable(Player.COMMAND_GET_TEXT_OFFSET) ? getPlayer().getTextOffsetMs() : 0;
        return new SubtitleOffsets(offset, offset);
    }

    default void setSubtitleOffsets(SubtitleOffsets offsets) {
    }

    default boolean supportsSubtitleOffsets() {
        return false;
    }

    void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection);

    /** Restores an exact per-media secondary selection without applying automatic selection. */
    void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection);

    default boolean addSubtitle(Sub sub) {
        return false;
    }

    String getErrorMessage(PlaybackException e);

    ErrorAction handleError(PlaybackException e);

    enum ErrorAction {
        RECOVERED,
        DECODE,
        FATAL
    }

    enum Type {
        EXO,
        MPV
    }

}
