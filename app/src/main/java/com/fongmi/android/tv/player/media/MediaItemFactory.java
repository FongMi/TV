package com.fongmi.android.tv.player.media;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.Util;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MediaItemFactory {

    public static MediaMetadata buildMetadata(String title, String artist, String artUri, String displayName) {
        title = TextUtils.isEmpty(title) ? "" : title;
        artist = TextUtils.isEmpty(artist) ? "" : artist;
        return new MediaMetadata.Builder().setTitle(title).setArtist(artist).setDisplayTitle(formatDisplayTitle(title, displayName)).setArtworkUri(TextUtils.isEmpty(artUri) ? null : Uri.parse(artUri)).build();
    }

    public static String formatDisplayTitle(String title, String name) {
        if (TextUtils.isEmpty(title)) return TextUtils.isEmpty(name) ? "" : name;
        if (TextUtils.isEmpty(name) || TextUtils.equals(title, name)) return title;
        return ResUtil.getString(R.string.detail_title, title, name);
    }

    public static String getDefaultUserAgent() {
        return Util.getUserAgent(App.get(), BuildConfig.APPLICATION_ID);
    }

    public static MediaItem from(PlaySpec spec) {
        return buildUpon(spec).build();
    }

    private static MediaItem.Builder buildUpon(PlaySpec spec) {
        return new MediaItem.Builder().setUri(spec.getUri())
                .setSubtitleConfigurations(buildSubtitleConfigs(spec.getSubs()))
                .setDrmConfiguration(buildDrmConfig(spec.getDrm()))
                .setRequestMetadata(buildRequestMetadata(spec))
                .setMediaMetadata(spec.getMetadata())
                .setAdblock(Setting.isAdblock())
                .setMimeType(spec.getFormat())
                .setImageDurationMs(15000)
                .setMediaId(spec.getKey());
    }

    private static MediaItem.RequestMetadata buildRequestMetadata(PlaySpec spec) {
        return new MediaItem.RequestMetadata.Builder().setMediaUri(spec.getUri()).setExtras(toBundle(spec.getHeaders())).build();
    }

    private static Bundle toBundle(Map<String, String> headers) {
        Bundle bundle = new Bundle();
        if (headers != null) headers.forEach(bundle::putString);
        return bundle;
    }

    private static List<MediaItem.SubtitleConfiguration> buildSubtitleConfigs(List<Sub> subs) {
        List<MediaItem.SubtitleConfiguration> configs = new ArrayList<>();
        if (subs == null || subs.isEmpty()) return configs;
        SubtitleFlags flags = SubtitleFlags.create(subs);
        for (int i = 0; i < subs.size(); i++) configs.add(buildSubConfig(subs.get(i), flags.get(subs.get(i), i)));
        return configs;
    }

    public static MediaItem.SubtitleConfiguration buildSubConfig(Sub sub) {
        return buildSubConfig(sub, sub.getFlag());
    }

    private static MediaItem.SubtitleConfiguration buildSubConfig(Sub sub, int flag) {
        return new MediaItem.SubtitleConfiguration.Builder(sub.getUri()).setLabel(sub.getName()).setMimeType(sub.getFormat()).setSelectionFlags(flag).setLanguage(sub.getLang()).build();
    }

    private static int findPreferredSubtitleIndex(List<Sub> subs) {
        int bestIndex = C.INDEX_UNSET;
        int bestScore = 0;
        for (int i = 0; i < subs.size(); i++) {
            int score = LangUtil.getPreferredTextLanguageScore(subs.get(i).getLang());
            if (score > bestScore) {
                bestIndex = i;
                bestScore = score;
            }
        }
        return bestIndex;
    }

    private static MediaItem.DrmConfiguration buildDrmConfig(Drm drm) {
        return drm == null ? null : new MediaItem.DrmConfiguration.Builder(drm.getUUID()).setMultiSession(!C.CLEARKEY_UUID.equals(drm.getUUID())).setForceDefaultLicenseUri(drm.isForceKey()).setLicenseRequestHeaders(drm.getHeader()).setLicenseUri(drm.getKey()).build();
    }

    private record SubtitleFlags(boolean hasExplicitFlags, int defaultIndex) {

        static SubtitleFlags create(List<Sub> subs) {
            if (subs.size() == 1) return new SubtitleFlags(false, C.INDEX_UNSET);
            if (hasExplicitFlags(subs)) return new SubtitleFlags(true, C.INDEX_UNSET);
            int preferredIndex = findPreferredSubtitleIndex(subs);
            return new SubtitleFlags(false, preferredIndex == C.INDEX_UNSET ? 0 : preferredIndex);
        }

        private static boolean hasExplicitFlags(List<Sub> subs) {
            for (Sub sub : subs) if (sub.getRawFlag() != 0) return true;
            return false;
        }

        int get(Sub sub, int index) {
            if (sub.getRawFlag() != 0) return sub.getFlag();
            if (hasExplicitFlags) return C.SELECTION_FLAG_AUTOSELECT;
            if (defaultIndex == C.INDEX_UNSET) return sub.getFlag();
            return index == defaultIndex ? C.SELECTION_FLAG_DEFAULT : C.SELECTION_FLAG_AUTOSELECT;
        }
    }
}
