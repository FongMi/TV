package com.fongmi.android.tv.player.engine;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.DiscMenuController;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
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
import com.google.gson.JsonObject;

import java.util.List;

public interface PlayerEngine {

    @Nullable
    default DiscMenuController getDiscMenuController() {
        return null;
    }

    Type getType();

    default boolean needsRebuild() {
        return false;
    }

    Player getPlayer();

    @Nullable
    default JsonObject getPlaybackLog() {
        return null;
    }

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

    default void prepareForNewMedia() {
    }

    default boolean switchDecoderForRetry(PlaybackException exception) {
        return false;
    }

    default PlayerEffect getEffect() {
        return PlayerEffect.NONE;
    }

    boolean setMediaItem(PlaySpec spec, long startPositionMs);

    default void start(PlaySpec spec, long startPositionMs) {
        if (!setMediaItem(spec, startPositionMs)) return;
        getPlayer().prepare();
        getPlayer().play();
    }

    default void setEnding(long endingMs) {
    }

    default boolean isEndingManaged() {
        return false;
    }

    default long getOriginalDurationMs() {
        return C.TIME_UNSET;
    }

    default boolean preload(PlaySpec spec, long startPositionMs) {
        return preload(spec, startPositionMs, () -> {});
    }

    default boolean preload(PlaySpec spec, long startPositionMs, Runnable onError) {
        return false;
    }

    @Nullable
    default PlaySpec consumePreload(MediaItem mediaItem) {
        return null;
    }

    default boolean seekToNext() {
        return false;
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

    void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection);

    default boolean addSubtitle(Sub sub) {
        return false;
    }

    String getErrorMessage(PlaybackException e);

    ErrorAction handleError(PlaybackException e);

    enum ErrorAction {
        RECOVERED,
        DECODE,
        REFRESH,
        FATAL
    }

    enum Type {
        EXO,
        MPV
    }
}
