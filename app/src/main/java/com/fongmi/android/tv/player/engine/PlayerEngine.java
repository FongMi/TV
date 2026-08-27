package com.fongmi.android.tv.player.engine;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.media.PlaySpec;

import java.util.List;

public interface PlayerEngine {

    int SOFT = C.DECODE_SOFTWARE;
    int HARD = C.DECODE_HARDWARE;

    Type getType();

    default boolean needsRebuild() {
        return false;
    }

    Player getPlayer();

    int getAudioChannelCount();

    void release();

    void setDecode(int decode);

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

    default SecondarySubtitleState getSecondarySubtitleState() {
        return SecondarySubtitleState.EMPTY;
    }

    default void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
    }

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

    record SecondarySubtitleState(@Nullable TrackSelectionOverride primarySelection, @Nullable TrackSelectionOverride explicitSelection, List<TrackSelectionOverride> secondaryCandidates, boolean secondaryPromotedToPrimary) {

        public static final SecondarySubtitleState EMPTY = new SecondarySubtitleState(null, null, List.of(), false);

        public SecondarySubtitleState {
            secondaryCandidates = List.copyOf(secondaryCandidates);
        }
    }
}
