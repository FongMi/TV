package com.fongmi.android.tv.player.exo;

import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter;
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer;

/**
 * Some hardware decoders (Rockchip) stall after a seek when skipped or dropped frames are released without being rendered,
 * so those frames are rendered instead.
 */
public class FirstFrameVideoRenderer extends MediaCodecVideoRenderer {

    public FirstFrameVideoRenderer(Builder builder) {
        super(builder);
    }

    @Override
    protected void skipOutputBuffer(MediaCodecAdapter codec, int index, long presentationTimeUs) {
        renderOutputBufferV21(codec, index, presentationTimeUs, System.nanoTime());
    }

    @Override
    protected void dropOutputBuffer(MediaCodecAdapter codec, int index, long presentationTimeUs) {
        renderOutputBufferV21(codec, index, presentationTimeUs, System.nanoTime());
    }
}
