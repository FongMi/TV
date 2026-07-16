package com.fongmi.android.tv.player.effect.audio;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;

import com.fongmi.android.tv.setting.DecodeSetting;

public final class AudioTrackSupport {

    public static boolean isPassThroughSelected(Player player) {
        if (player == null || !DecodeSetting.isAudioPassThrough() || DecodeSetting.isAudioPrefer()) return false;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_AUDIO) continue;
            for (int i = 0; i < group.length; i++) {
                if (group.isTrackSelected(i) && isPassThroughMimeType(group.getTrackFormat(i).sampleMimeType)) return true;
            }
        }
        return false;
    }

    public static int getSelectedChannelCount(Player player) {
        if (player == null) return Format.NO_VALUE;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_AUDIO) continue;
            for (int i = 0; i < group.length; i++) {
                Format format = group.getTrackFormat(i);
                if (group.isTrackSelected(i) && format.channelCount != Format.NO_VALUE) return format.channelCount;
            }
        }
        return Format.NO_VALUE;
    }

    private static boolean isPassThroughMimeType(String mimeType) {
        return MimeTypes.AUDIO_AC3.equals(mimeType)
                || MimeTypes.AUDIO_E_AC3.equals(mimeType)
                || MimeTypes.AUDIO_E_AC3_JOC.equals(mimeType)
                || MimeTypes.AUDIO_AC4.equals(mimeType)
                || MimeTypes.AUDIO_TRUEHD.equals(mimeType)
                || MimeTypes.AUDIO_DTS.equals(mimeType)
                || MimeTypes.AUDIO_DTS_HD.equals(mimeType)
                || MimeTypes.AUDIO_DTS_HD_MA.equals(mimeType)
                || MimeTypes.AUDIO_DTS_EXPRESS.equals(mimeType);
    }
}
