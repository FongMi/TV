package com.fongmi.android.tv.browse;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.google.common.collect.ImmutableList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrowseTree {

    private static final String CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";
    private static final int CONTENT_STYLE_LIST = 1;

    private static final String ROOT = "ROOT";
    private static final String VOD = "VOD";
    private static final String LIVE = "LIVE";
    private static final Map<String, Result> browseResultMap = new ConcurrentHashMap<>();
    private static final MediaItem ROOT_ITEM = folder(ROOT, "影視");
    private static final MediaItem VOD_FOLDER = folder(VOD, "點播");
    private static final MediaItem LIVE_FOLDER;

    static {
        Bundle extras = new Bundle();
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST);
        MediaMetadata meta = new MediaMetadata.Builder().setTitle("直播").setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).setExtras(extras).build();
        LIVE_FOLDER = new MediaItem.Builder().setMediaId(LIVE).setMediaMetadata(meta).build();
    }

    public static void clear() {
        clearVod();
        clearLive();
    }

    public static void clearVod() {
        clearPending(VodBrowse.VOD_EP);
        VodBrowse.clear();
    }

    public static void clearLive() {
        clearPending(LiveBrowse.LIVE_CH);
        LiveBrowse.clear();
    }

    public static MediaItem getRootItem() {
        return ROOT_ITEM;
    }

    @NonNull
    public static ImmutableList<MediaItem> getChildren(@NonNull String parentId, int page, int pageSize) {
        return page(getChildrenInternal(parentId), page, pageSize);
    }

    @NonNull
    private static ImmutableList<MediaItem> getChildrenInternal(@NonNull String parentId) {
        return switch (parentId) {
            case ROOT -> ImmutableList.of(VOD_FOLDER, LIVE_FOLDER);
            case VOD -> VodBrowse.getHistory();
            case LIVE -> LiveBrowse.getGroups();
            default -> {
                if (parentId.startsWith(LiveBrowse.LIVE_GROUP)) yield LiveBrowse.getChannels(parentId);
                yield ImmutableList.of();
            }
        };
    }

    @Nullable
    public static MediaItem getItem(@NonNull String mediaId) {
        return switch (mediaId) {
            case ROOT -> ROOT_ITEM;
            case VOD -> VOD_FOLDER;
            case LIVE -> LIVE_FOLDER;
            default -> mediaId.startsWith(LiveBrowse.LIVE_GROUP) || mediaId.startsWith(LiveBrowse.LIVE_CH) ? LiveBrowse.getItem(mediaId) : VodBrowse.getItem(mediaId);
        };
    }

    @NonNull
    public static ImmutableList<MediaItem> search(@NonNull String query) {
        return VodBrowse.search(query);
    }

    @NonNull
    public static ImmutableList<MediaItem> getSearchResult(@NonNull String query, int page, int pageSize) {
        return VodBrowse.getSearchResult(query, page, pageSize);
    }

    @Nullable
    public static MediaItem resolve(@NonNull String mediaId) throws Exception {
        if (mediaId.startsWith(LiveBrowse.LIVE_CH)) return LiveBrowse.resolve(mediaId);
        return VodBrowse.resolve(mediaId);
    }

    @NonNull
    public static MediaItem resolveOrKeep(@NonNull MediaItem item) {
        try {
            MediaItem resolved = resolve(item.mediaId);
            return resolved != null ? resolved : item;
        } catch (Exception e) {
            return item;
        }
    }

    @Nullable
    public static MediaItem navigate(@NonNull String mediaId, int delta) throws Exception {
        MediaItem vod = VodBrowse.navigate(mediaId, delta);
        if (vod != null) return vod;
        return LiveBrowse.navigate(mediaId, delta);
    }

    public static long consumeResumePosition() {
        return VodBrowse.consumeResumePosition();
    }

    public static boolean saveProgress(long position, long duration) {
        return VodBrowse.saveProgress(position, duration);
    }

    @Nullable
    public static Result consumeBrowseResult(@NonNull String mediaId) {
        return browseResultMap.remove(mediaId);
    }

    static void putBrowseResult(@NonNull String mediaId, @NonNull Result result) {
        browseResultMap.put(mediaId, result);
    }

    static int wrapIndex(int current, int delta, int count) {
        return ((current + delta) % count + count) % count;
    }

    private static void clearPending(@NonNull String prefix) {
        browseResultMap.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @NonNull
    static ImmutableList<MediaItem> page(@NonNull ImmutableList<MediaItem> items, int page, int pageSize) {
        if (pageSize <= 0) return items;
        long fromLong = Math.max(0, page) * (long) pageSize;
        if (fromLong >= items.size()) return ImmutableList.of();
        int from = (int) fromLong;
        return ImmutableList.copyOf(items.subList(from, Math.min(items.size(), from + pageSize)));
    }

    static MediaItem folder(@NonNull String id, @NonNull String title) {
        return build(id, true, false, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, title, null, null, null);
    }

    static MediaItem playable(@NonNull String id, @NonNull String title, @Nullable String subtitle, @Nullable String art) {
        return build(id, false, true, MediaMetadata.MEDIA_TYPE_VIDEO, title, subtitle, art, null);
    }

    static MediaItem stream(@NonNull String id, @NonNull String url, @NonNull String title, @Nullable String subtitle, @Nullable String art) {
        return build(id, false, true, MediaMetadata.MEDIA_TYPE_VIDEO, title, subtitle, art, Uri.parse(url));
    }

    private static MediaItem build(@NonNull String id, boolean browsable, boolean playable, int mediaType, @NonNull String title, @Nullable String subtitle, @Nullable String art, @Nullable Uri uri) {
        MediaMetadata.Builder metadata = new MediaMetadata.Builder().setTitle(title).setIsBrowsable(browsable).setIsPlayable(playable).setMediaType(mediaType);
        if (!TextUtils.isEmpty(subtitle)) metadata.setSubtitle(subtitle);
        if (!TextUtils.isEmpty(art)) metadata.setArtworkUri(MediaItemFactory.getArtworkUri(art));
        if (uri != null && !TextUtils.isEmpty(subtitle)) metadata.setArtist(subtitle);
        if (uri != null) metadata.setDisplayTitle(MediaItemFactory.formatDisplayTitle(title, subtitle));
        MediaItem.Builder builder = new MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata.build());
        if (uri != null) builder.setUri(uri);
        return builder.build();
    }
}
