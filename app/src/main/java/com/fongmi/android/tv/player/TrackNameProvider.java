package com.fongmi.android.tv.player;

import android.content.res.Resources;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Util;
import androidx.media3.ui.R;

import com.fongmi.android.tv.App;

import java.util.Locale;

import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;

public class TrackNameProvider {

    private final Resources resources;

    public TrackNameProvider() {
        this.resources = App.get().getResources();
    }

    public String getTrackName(@NonNull Format format) {
        String trackName;
        int trackType = inferPrimaryTrackType(format);
        if (trackType == C.TRACK_TYPE_VIDEO) {
            trackName = joinWithSeparator(buildRoleString(format), buildResolutionString(format), buildBitrateString(format));
        } else if (trackType == C.TRACK_TYPE_AUDIO) {
            trackName = joinWithSeparator(buildLanguageOrLabelString(format), buildAudioChannelString(format), buildMimeString(format), buildBitrateString(format));
        } else {
            trackName = joinWithSeparator(buildLanguageString(format), buildLabelString(format), buildMimeString(format));
        }
        return TextUtils.isEmpty(trackName) ? resources.getString(R.string.exo_track_unknown) : trackName;
    }

    public String getTrackName(IjkTrackInfo trackInfo) {
        String trackName;
        int trackType = trackInfo.getTrackType();
        if (trackType == C.TRACK_TYPE_VIDEO) {
            trackName = joinWithSeparator(buildResolutionString(trackInfo.getWidth(), trackInfo.getHeight()), buildBitrateString(trackInfo.getBitrate()));
        } else if (trackType == C.TRACK_TYPE_AUDIO) {
            trackName = joinWithSeparator(buildLanguageString(trackInfo.getLanguage()), buildAudioChannelString(trackInfo.getChannelCount()), buildBitrateString(trackInfo.getBitrate()));
        } else {
            trackName = buildLanguageString(trackInfo.getLanguage());
        }
        return TextUtils.isEmpty(trackName) ? resources.getString(R.string.exo_track_unknown) : trackName;
    }

    private String buildResolutionString(Format format) {
        return buildResolutionString(format.width, format.height);
    }

    private String buildResolutionString(int width, int height) {
        return width == Format.NO_VALUE || height == Format.NO_VALUE ? "" : resources.getString(R.string.exo_track_resolution, width, height);
    }

    private String buildBitrateString(Format format) {
        return buildBitrateString(format.bitrate);
    }

    private String buildBitrateString(int bitrate) {
        return bitrate <= 0 ? "" : resources.getString(R.string.exo_track_bitrate, bitrate / 1000000f);
    }

    private String buildAudioChannelString(Format format) {
        return buildAudioChannelString(format.channelCount);
    }

    private String buildAudioChannelString(int channelCount) {
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
        return buildLanguageString(format.language);
    }

    private String buildLanguageString(@Nullable String language) {
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
            if (item.length() > 0) {
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

    private String buildMimeString(Format format) {
        if (format.sampleMimeType == null) return "";
        switch (format.sampleMimeType) {
            case MimeTypes.AUDIO_DTS:
                return "DTS";
            case MimeTypes.AUDIO_DTS_HD:
                return "DTS-HD";
            case MimeTypes.AUDIO_DTS_EXPRESS:
                return "DTS Express";
            case MimeTypes.AUDIO_TRUEHD:
                return "TrueHD";
            case MimeTypes.AUDIO_AC3:
                return "AC-3";
            case MimeTypes.AUDIO_E_AC3:
                return "E-AC-3";
            case MimeTypes.AUDIO_E_AC3_JOC:
                return "E-AC-3-JOC";
            case MimeTypes.AUDIO_AC4:
                return "AC-4";
            case MimeTypes.AUDIO_AAC:
                return "AAC";
            case MimeTypes.AUDIO_MPEG:
                return "MP3";
            case MimeTypes.AUDIO_MPEG_L2:
                return "MP2";
            case MimeTypes.AUDIO_VORBIS:
                return "Vorbis";
            case MimeTypes.AUDIO_OPUS:
                return "Opus";
            case MimeTypes.AUDIO_FLAC:
                return "FLAC";
            case MimeTypes.AUDIO_ALAC:
                return "ALAC";
            case MimeTypes.AUDIO_WAV:
                return "WAV";
            case MimeTypes.AUDIO_AMR:
                return "AMR";
            case MimeTypes.AUDIO_AMR_NB:
                return "AMR-NB";
            case MimeTypes.AUDIO_AMR_WB:
                return "AMR-WB";
            case MimeTypes.APPLICATION_PGS:
                return "PGS";
            case MimeTypes.APPLICATION_SUBRIP:
                return "SRT";
            case MimeTypes.TEXT_SSA:
                return "SSA";
            case MimeTypes.TEXT_VTT:
                return "VTT";
            case MimeTypes.APPLICATION_TTML:
                return "TTML";
            case MimeTypes.APPLICATION_TX3G:
                return "TX3G";
            case MimeTypes.APPLICATION_DVBSUBS:
                return "DVB";
            default:
                return "";
        }
    }
}
