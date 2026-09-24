package com.fongmi.android.tv.player.exo;

import androidx.annotation.Nullable;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.libass.LibassSubtitleController;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.media3.ui.libass.LibassPlayerViewController;

import com.fongmi.android.tv.setting.SubtitleSetting;

final class ExoSubtitleController {

    private final LibassSubtitleController libassSubtitleController;
    private final LibassPlaybackSession libassPlaybackSession;
    private final LibassPlayerViewController playerViewController;

    ExoSubtitleController(ExoPlayerSession session) {
        libassPlaybackSession = session.libassPlaybackSession();
        libassSubtitleController = session.libassSubtitleController();
        playerViewController = new LibassPlayerViewController(session.player(), libassPlaybackSession, libassSubtitleController);
        applySubtitleStyle();
        applySecondarySubtitleMode();
    }

    void release() {
        playerViewController.close();
    }

    void bindPlayerView(PlayerView playerView) {
        playerViewController.bind(playerView);
    }

    void applySubtitleStyle() {
        libassPlaybackSession.setBottomPositionFraction(SubtitleSetting.getPosition() / 100.0f);
        libassPlaybackSession.setSecondaryBottomPositionFraction(getSecondaryBottomPositionFraction());
        libassPlaybackSession.setFontScale(SubtitleSetting.isScaleApplied() ? SubtitleSetting.getAppliedScale() : 1.0f, true);
        playerViewController.setStyleOverride(SubtitleSetting.isStyleForced() ? SubtitleSetting.getStyle() : null, SubtitleSetting.getFontFamily());
        playerViewController.setSecondarySubtitleViewConfigurator(this::applySecondarySubtitleStyle);
    }

    SubtitleSelectionState getSubtitleSelectionState() {
        return libassSubtitleController.getSubtitleSelectionState();
    }

    void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        applySecondarySubtitleMode();
        libassSubtitleController.setSecondaryTextTrackSelectionOverride(selection);
    }

    void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        libassSubtitleController.setSecondaryTextTrackAutoSelectionEnabled(false);
        libassSubtitleController.setSecondaryTextTrackSelectionOverride(selection);
    }

    void prepareForNewMedia() {
        applySecondarySubtitleMode();
    }

    private void applySecondarySubtitleMode() {
        libassSubtitleController.setSecondaryTextTrackAutoSelectionEnabled(SubtitleSetting.getSecondaryMode() == SubtitleSetting.SECONDARY_MODE_AUTO);
    }

    private void applySecondarySubtitleStyle(SubtitleView subtitleView) {
        SubtitleSetting.applyStyle(subtitleView);
        // VTT cues have line positions, so bottom padding alone cannot move them.
        subtitleView.setBottomPaddingFraction(0.0f);
        subtitleView.setBottomPosition(getSecondaryBottomPositionFraction());
    }

    private static float getSecondaryBottomPositionFraction() {
        return (100.0f - SubtitleSetting.getSecondaryPosition()) / 100.0f;
    }
}
