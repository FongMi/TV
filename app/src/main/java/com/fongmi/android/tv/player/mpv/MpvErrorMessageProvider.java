package com.fongmi.android.tv.player.mpv;

import androidx.media3.common.PlaybackException;

public class MpvErrorMessageProvider {

    public String get(PlaybackException e) {
        return switch (e.errorCode) {
            case PlaybackException.ERROR_CODE_BAD_VALUE -> "MPV Bad Value";
            case PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "MPV Runtime Error";
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "MPV IO Error";
            case PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "MPV Container Unsupported";
            case PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "MPV Audio Init Failed";
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "MPV Decoder Init Failed";
            case PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "MPV Decoder Query Failed";
            case PlaybackException.ERROR_CODE_DECODING_FAILED -> "MPV Decoding Failed";
            default -> "MPV Playback Error";
        };
    }
}
