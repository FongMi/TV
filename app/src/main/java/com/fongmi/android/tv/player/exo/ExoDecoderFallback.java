package com.fongmi.android.tv.player.exo;

import androidx.media3.common.C;
import androidx.media3.exoplayer.DecoderManager;
import androidx.media3.exoplayer.DecoderManager.DecoderFailure;
import androidx.media3.exoplayer.DecoderMode;
import androidx.media3.exoplayer.ExoPlaybackException;

import java.util.EnumSet;

/** Falls back from hardware to platform software to FFmpeg independently for audio and video. */
final class ExoDecoderFallback {

    private final EnumSet<DecoderMode> audioAttempts = EnumSet.noneOf(DecoderMode.class);
    private final EnumSet<DecoderMode> videoAttempts = EnumSet.noneOf(DecoderMode.class);
    private final DecoderManager decoderManager;

    ExoDecoderFallback(DecoderManager decoderManager) {
        this.decoderManager = decoderManager;
    }

    void reset(@C.TrackType int trackType) {
        if (trackType == C.TRACK_TYPE_AUDIO) audioAttempts.clear();
        else if (trackType == C.TRACK_TYPE_VIDEO) videoAttempts.clear();
    }

    boolean retry(ExoPlaybackException error) {
        DecoderFailure failure = decoderManager.getDecoderFailure(error);
        if (failure == null) return false;
        EnumSet<DecoderMode> attempts = failure.trackType == C.TRACK_TYPE_AUDIO ? audioAttempts : videoAttempts;
        attempts.addAll(failure.failedDecoderModes);
        if (attempts.contains(DecoderMode.FFMPEG)) return false;
        // Only a known hardware failure warrants trying platform software.
        DecoderMode nextMode = failure.decoderMode == DecoderMode.HARDWARE && !attempts.contains(DecoderMode.SOFTWARE) ? DecoderMode.SOFTWARE : DecoderMode.FFMPEG;
        attempts.add(nextMode);
        if (failure.trackType == C.TRACK_TYPE_AUDIO) decoderManager.selectAudioDecoder(nextMode);
        else decoderManager.selectVideoDecoder(nextMode);
        return true;
    }
}
