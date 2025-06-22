package com.fongmi.android.tv.player.exo;

import android.content.res.Resources;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Util;
import androidx.media3.ui.R;

import com.fongmi.android.tv.App;

import java.util.Locale;

public class TrackNameProvider {

    private final Resources resources;

    public TrackNameProvider() {
        this.resources = App.get().getResources();
    }

    public String getTrackName(@NonNull Format format) {
        String trackName;
        int trackType = inferPrimaryTrackType(format);
        if (trackType == C.TRACK_TYPE_VIDEO) {
            trackName = joinWithSeparator(buildRoleString(format), buildResolutionString(format), buildFrameRateString(format), buildBitrateString(format));
        } else if (trackType == C.TRACK_TYPE_AUDIO) {
            trackName = joinWithSeparator(buildLanguageOrLabelString(format), buildAudioChannelString(format), buildBitrateString(format));
        } else {
            trackName = joinWithSeparator(buildLanguageString(format), buildLabelString(format));
        }
        return TextUtils.isEmpty(trackName) ? resources.getString(R.string.exo_track_unknown) : joinWithSeparator(trackName, buildMimeString(trackType, format));
    }

    private String buildResolutionString(Format format) {
        int width = format.width;
        int height = format.height;
        return width == Format.NO_VALUE || height == Format.NO_VALUE ? "" : resources.getString(R.string.exo_track_resolution, width, height);
    }

    private String buildBitrateString(Format format) {
        int bitrate = format.bitrate;
        return bitrate == Format.NO_VALUE ? "" : resources.getString(R.string.exo_track_bitrate, bitrate / 1000000f);
    }

    private String buildFrameRateString(Format format) {
        float fameRate = format.frameRate;
        return fameRate <= 0 ? "" : (int) Math.floor(fameRate) + "FPS";
    }

    private String buildAudioChannelString(Format format) {
        int channelCount = format.channelCount;
        if (channelCount < 1) return "";
        switch (channelCount) {
            case 1:
                return resources.getString(R.string.exo_track_mono);
            case 2:
                return resources.getString(R.string.exo_track_stereo);
            case 6:
            case 7:
                return resources.getString(R.string.exo_track_surround_5_point_1);
            case 8:
                return resources.getString(R.string.exo_track_surround_7_point_1);
            default:
                return resources.getString(R.string.exo_track_surround);
        }
    }

    private String buildLanguageOrLabelString(Format format) {
        String languageAndRole = joinWithSeparator(buildLanguageString(format), buildRoleString(format));
        return TextUtils.isEmpty(languageAndRole) ? buildLabelString(format) : languageAndRole;
    }

    private String buildLabelString(Format format) {
        return TextUtils.isEmpty(format.label) ? "" : format.label;
    }

    private String buildLanguageString(Format format) {
        String language = format.language;
        if ("chs".equals(language)) language = "zh-Hans";
        if ("cht".equals(language)) language = "zh-Hant";
        if (TextUtils.isEmpty(language) || C.LANGUAGE_UNDETERMINED.equals(language)) return "";
        Locale languageLocale = Util.SDK_INT >= 21 ? Locale.forLanguageTag(language) : new Locale(language);
        Locale displayLocale = Util.getDefaultDisplayLocale();
        String languageName = languageLocale.getDisplayName(displayLocale);
        if (TextUtils.isEmpty(languageName)) return "";
        try {
            int firstCodePointLength = languageName.offsetByCodePoints(0, 1);
            return languageName.substring(0, firstCodePointLength).toUpperCase(displayLocale) + languageName.substring(firstCodePointLength);
        } catch (IndexOutOfBoundsException e) {
            return languageName;
        }
    }

    private String buildRoleString(Format format) {
        String roles = "";
        if ((format.roleFlags & C.ROLE_FLAG_ALTERNATE) != 0) roles = resources.getString(R.string.exo_track_role_alternate);
        if ((format.roleFlags & C.ROLE_FLAG_SUPPLEMENTARY) != 0) roles = joinWithSeparator(roles, resources.getString(R.string.exo_track_role_supplementary));
        if ((format.roleFlags & C.ROLE_FLAG_COMMENTARY) != 0) roles = joinWithSeparator(roles, resources.getString(R.string.exo_track_role_commentary));
        if ((format.roleFlags & (C.ROLE_FLAG_CAPTION | C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND)) != 0) roles = joinWithSeparator(roles, resources.getString(R.string.exo_track_role_closed_captions));
        return roles;
    }

    private String joinWithSeparator(String... items) {
        String itemList = "";
        for (String item : items) {
            if (!item.isEmpty()) {
                if (TextUtils.isEmpty(itemList)) {
                    itemList = item;
                } else {
                    itemList = resources.getString(R.string.exo_item_list, itemList, item);
                }
            }
        }
        return itemList;
    }

    private int inferPrimaryTrackType(Format format) {
        int trackType = MimeTypes.getTrackType(format.sampleMimeType);
        if (trackType != C.TRACK_TYPE_UNKNOWN) return trackType;
        if (MimeTypes.getVideoMediaMimeType(format.codecs) != null) return C.TRACK_TYPE_VIDEO;
        if (MimeTypes.getAudioMediaMimeType(format.codecs) != null) return C.TRACK_TYPE_AUDIO;
        if (format.width != Format.NO_VALUE || format.height != Format.NO_VALUE) return C.TRACK_TYPE_VIDEO;
        if (format.channelCount != Format.NO_VALUE || format.sampleRate != Format.NO_VALUE) return C.TRACK_TYPE_AUDIO;
        return C.TRACK_TYPE_UNKNOWN;
    }

    private String buildMimeString(int trackType, Format format) {
        if (trackType == C.TRACK_TYPE_TEXT && format.codecs != null) return buildMimeString(format.codecs);
        if (format.sampleMimeType != null) return buildMimeString(format.sampleMimeType);
        return "";
    }

    private String buildMimeString(String mimeType) {
        if (mimeType.contains(MimeTypes.AUDIO_DTS)) return "DTS";
        else if (mimeType.contains(MimeTypes.AUDIO_DTS_HD)) return "DTS-HD";
        else if (mimeType.contains(MimeTypes.AUDIO_DTS_EXPRESS)) return "DTS Express";
        else if (mimeType.contains(MimeTypes.AUDIO_TRUEHD)) return "TrueHD";
        else if (mimeType.contains(MimeTypes.AUDIO_AC3)) return "AC-3";
        else if (mimeType.contains(MimeTypes.AUDIO_E_AC3)) return "E-AC-3";
        else if (mimeType.contains(MimeTypes.AUDIO_E_AC3_JOC)) return "E-AC-3-JOC";
        else if (mimeType.contains(MimeTypes.AUDIO_AC4)) return "AC-4";
        else if (mimeType.contains(MimeTypes.AUDIO_AAC)) return "AAC";
        else if (mimeType.contains(MimeTypes.AUDIO_MPEG)) return "MP3";
        else if (mimeType.contains(MimeTypes.AUDIO_MPEG_L2)) return "MP2";
        else if (mimeType.contains(MimeTypes.AUDIO_VORBIS)) return "Vorbis";
        else if (mimeType.contains(MimeTypes.AUDIO_OPUS)) return "Opus";
        else if (mimeType.contains(MimeTypes.AUDIO_FLAC)) return "FLAC";
        else if (mimeType.contains(MimeTypes.AUDIO_ALAC)) return "ALAC";
        else if (mimeType.contains(MimeTypes.AUDIO_WAV)) return "WAV";
        else if (mimeType.contains(MimeTypes.AUDIO_AMR)) return "AMR";
        else if (mimeType.contains(MimeTypes.AUDIO_AMR_NB)) return "AMR-NB";
        else if (mimeType.contains(MimeTypes.AUDIO_AMR_WB)) return "AMR-WB";
        else if (mimeType.contains(MimeTypes.VIDEO_MP4)) return "MP4";
        else if (mimeType.contains(MimeTypes.VIDEO_FLV)) return "FLV";
        else if (mimeType.contains(MimeTypes.VIDEO_AV1)) return "AV1";
        else if (mimeType.contains(MimeTypes.VIDEO_AVI)) return "AVI";
        else if (mimeType.contains(MimeTypes.VIDEO_MPEG)) return "MPEG";
        else if (mimeType.contains(MimeTypes.VIDEO_MPEG2)) return "MPEG2";
        else if (mimeType.contains(MimeTypes.VIDEO_H263)) return "H263";
        else if (mimeType.contains(MimeTypes.VIDEO_H264)) return "H264";
        else if (mimeType.contains(MimeTypes.VIDEO_H265)) return "H265";
        else if (mimeType.contains(MimeTypes.VIDEO_VC1)) return "VC1";
        else if (mimeType.contains(MimeTypes.VIDEO_VP8)) return "VP8";
        else if (mimeType.contains(MimeTypes.VIDEO_VP9)) return "VP9";
        else if (mimeType.contains(MimeTypes.VIDEO_DIVX)) return "DIVX";
        else if (mimeType.contains(MimeTypes.VIDEO_DOLBY_VISION)) return "DOLBY";
        else if (mimeType.contains(MimeTypes.TEXT_SSA)) return "SSA";
        else if (mimeType.contains(MimeTypes.TEXT_VTT)) return "VTT";
        else if (mimeType.contains(MimeTypes.APPLICATION_PGS)) return "PGS";
        else if (mimeType.contains(MimeTypes.APPLICATION_SUBRIP)) return "SRT";
        else if (mimeType.contains(MimeTypes.APPLICATION_TTML)) return "TTML";
        else if (mimeType.contains(MimeTypes.APPLICATION_TX3G)) return "TX3G";
        else if (mimeType.contains(MimeTypes.APPLICATION_DVBSUBS)) return "DVB";
        else if (mimeType.contains(MimeTypes.APPLICATION_MEDIA3_CUES)) return "CUES";
        else return mimeType;
    }
}
