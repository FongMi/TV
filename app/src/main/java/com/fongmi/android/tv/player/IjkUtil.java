package com.fongmi.android.tv.player;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkUtil {

    public static IjkMediaPlayer createPlayer() {
        int player = IjkMediaPlayer.OPT_CATEGORY_PLAYER;
        int codec = IjkMediaPlayer.OPT_CATEGORY_CODEC;
        int format = IjkMediaPlayer.OPT_CATEGORY_FORMAT;
        IjkMediaPlayer ijkPlayer = new IjkMediaPlayer();
        ijkPlayer.setOption(codec, "skip_loop_filter", 48);
        ijkPlayer.setOption(format, "probesize", 1024 * 800);
        ijkPlayer.setOption(player, "max-buffer-size", 1024 * 800);
        ijkPlayer.setOption(format, "analyzeduration", 30 * 1000 * 1000);
        ijkPlayer.setOption(format, "analyzemaxduration", 30 * 1000 * 1000);
        ijkPlayer.setOption(player, "soundtouch", 1);
        ijkPlayer.setOption(format, "flush_packets", 1);
        ijkPlayer.setOption(player, "packet-buffering", 0);
        ijkPlayer.setOption(player, "reconnect", 1);
        ijkPlayer.setOption(player, "framedrop", 1);
        ijkPlayer.setOption(player, "max-fps", 60);
        ijkPlayer.setOption(player, "enable-accurate-seek", 0);
        ijkPlayer.setOption(format, "fflags", "fastseek");
        ijkPlayer.setOption(format, "dns_cache_clear", 1);
        ijkPlayer.setOption(format, "timeout", 30 * 1000 * 1000);
        ijkPlayer.setOption(format, "rtsp_transport", "tcp");
        ijkPlayer.setOption(format, "rtsp_flags", "prefer_tcp");
        ijkPlayer.setOption(format, "buffer_size", 1024 * 800);
        ijkPlayer.setOption(format, "infbuf", 1);
        ijkPlayer.setOption(player, "videotoolbox", 0);
        ijkPlayer.setOption(player, "mediacodec", 0);
        ijkPlayer.setOption(player, "mediacodec-auto-rotate", 0);
        ijkPlayer.setOption(player, "mediacodec-handle-resolution-change", 0);
        return ijkPlayer;
    }
}
