package com.fongmi.android.tv.browse;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.ImmutableList;

public class BrowseTree {

    private static final String CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";
    private static final int CONTENT_STYLE_LIST = 1;
    private static final String CONTENT_STYLE_SINGLE_ITEM = "android.media.browse.CONTENT_STYLE_SINGLE_ITEM_HINT";
    private static final int CONTENT_STYLE_CATEGORY_LIST = 3;

    private static final String ROOT = "ROOT";
    private static final String VOD = "VOD";
    private static final String LIVE = "LIVE";
    public static final String VOD_KEEP = "VOD_KEEP";
    public static final String LIVE_KEEP = "LIVE_KEEP";
    private static final MediaItem ROOT_ITEM = folder(ROOT, "影視");
    private static final MediaItem VOD_FOLDER = build(VOD, true, false, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, "點播", null, icon(R.drawable.ic_nav_vod), null);
    private static final MediaItem LIVE_FOLDER;

    static {
        Bundle extras = new Bundle();
        extras.putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST);
        MediaMetadata meta = new MediaMetadata.Builder().setTitle("直播").setArtworkUri(Uri.parse(icon(R.drawable.ic_nav_live))).setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).setExtras(extras).build();
        LIVE_FOLDER = new MediaItem.Builder().setMediaId(LIVE).setMediaMetadata(meta).build();
    }

    public static void clear() {
        clearVod();
    }

    public static void clearVod() {
        clearSearch();
        VodBrowse.clearCache();
    }

    public static void clearSearch() {
        BrowseSearch.clear();
    }

    public static MediaItem getRootItem() {
        return ROOT_ITEM;
    }

    @NonNull
    public static ImmutableList<MediaItem> getChildren(@NonNull String parentId, int page, int pageSize) throws Exception {
        return page(getChildrenInternal(parentId), page, pageSize);
    }

    public static int getChildCount(@NonNull String parentId) throws Exception {
        return parentId.startsWith(VodBrowse.VOD_DETAIL) ? Integer.MAX_VALUE : getChildrenInternal(parentId).size();
    }

    @NonNull
    private static ImmutableList<MediaItem> getChildrenInternal(@NonNull String parentId) throws Exception {
        return switch (parentId) {
            case ROOT -> ImmutableList.of(VOD_FOLDER, LIVE_FOLDER);
            case VOD -> ImmutableList.<MediaItem>builder().add(getItem(VOD_KEEP)).addAll(VodBrowse.getHistory()).build();
            case LIVE -> ImmutableList.<MediaItem>builder().add(getItem(LIVE_KEEP)).addAll(LiveBrowse.getGroups()).build();
            case VOD_KEEP -> VodBrowse.getFavorites();
            case LIVE_KEEP -> LiveBrowse.getFavorites();
            default -> {
                if (parentId.startsWith(LiveBrowse.LIVE_GROUP)) yield LiveBrowse.getChannels(parentId);
                if (parentId.startsWith(VodBrowse.VOD_DETAIL)) yield VodBrowse.getEpisodes(parentId);
                yield ImmutableList.of();
            }
        };
    }

    @Nullable
    public static MediaItem getItem(@NonNull String mediaId) throws Exception {
        return switch (mediaId) {
            case ROOT -> ROOT_ITEM;
            case VOD -> VOD_FOLDER;
            case LIVE -> LIVE_FOLDER;
            case VOD_KEEP, LIVE_KEEP -> folder(mediaId, ResUtil.getString(R.string.keep), R.drawable.ic_browse_keep);
            default -> mediaId.startsWith(LiveBrowse.LIVE_GROUP) || mediaId.startsWith(LiveBrowse.LIVE_CH) ? LiveBrowse.getItem(mediaId) : VodBrowse.getItem(mediaId);
        };
    }

    @NonNull
    public static ImmutableList<MediaItem> search(@NonNull String query) {
        return BrowseSearch.search(query);
    }

    @NonNull
    public static ImmutableList<MediaItem> getSearchResult(@NonNull String query, int page, int pageSize) {
        return BrowseSearch.getResults(query, page, pageSize);
    }

    @Nullable
    public static MediaItem resolve(@NonNull String mediaId) throws Exception {
        if (mediaId.startsWith(LiveBrowse.LIVE_CH)) return LiveBrowse.resolve(mediaId);
        return VodBrowse.resolve(mediaId);
    }

    @NonNull
    public static MediaItem resolveForPlayback(@NonNull MediaItem item, @Nullable History bookmark) throws Exception {
        if (TextUtils.isEmpty(item.mediaId) && !TextUtils.isEmpty(item.requestMetadata.searchQuery)) {
            ImmutableList<MediaItem> results = search(item.requestMetadata.searchQuery);
            if (results.isEmpty()) throw new IllegalArgumentException();
            item = results.get(0);
        }
        MediaItem resolved = resolve(item.mediaId);
        if (resolved == null || resolved.localConfiguration == null) throw new IllegalArgumentException();
        Result result = getBrowseResult(resolved);
        if (result == null) throw new IllegalArgumentException();
        if (result.hasMsg()) throw new IllegalStateException(result.getMsg());
        return BrowsePlayback.withBookmark(resolved, bookmark);
    }

    public static boolean canNavigate(String mediaId) {
        return mediaId.startsWith(VodBrowse.VOD_EP) || mediaId.startsWith(LiveBrowse.LIVE_CH);
    }

    @Nullable
    public static MediaItem navigate(@NonNull String mediaId, int delta) throws Exception {
        MediaItem vod = VodBrowse.navigate(mediaId, delta);
        if (vod != null || mediaId.startsWith(VodBrowse.VOD_EP)) return vod;
        return LiveBrowse.navigate(mediaId, delta);
    }

    @Nullable
    public static MediaMetadata loadLiveMetadata(String mediaId) {
        return LiveBrowse.loadMetadata(mediaId);
    }

    @Nullable
    public static String getLiveGroupId(String mediaId) {
        int separator = mediaId.lastIndexOf('|');
        return mediaId.startsWith(LiveBrowse.LIVE_CH) && separator >= LiveBrowse.LIVE_CH.length()
                ? LiveBrowse.LIVE_GROUP + mediaId.substring(LiveBrowse.LIVE_CH.length(), separator) : null;
    }

    @Nullable
    public static Result getBrowseResult(@NonNull MediaItem item) {
        BrowsePlayback playback = BrowsePlayback.from(item);
        return playback == null ? null : playback.getResult();
    }

    static int wrapIndex(int current, int delta, int count) {
        return ((current + delta) % count + count) % count;
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

    static MediaItem folder(@NonNull String id, @NonNull String title, @DrawableRes int icon) {
        Bundle extras = new Bundle();
        extras.putInt(CONTENT_STYLE_SINGLE_ITEM, CONTENT_STYLE_CATEGORY_LIST);
        MediaItem item = folder(id, title);
        return item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkUri(Uri.parse(icon(icon))).setExtras(extras).build()).build();
    }

    static String icon(@DrawableRes int resId) {
        return "android.resource://" + BuildConfig.APPLICATION_ID + "/" + resId;
    }

    static String artwork(@Nullable String art, @DrawableRes int fallback) {
        return TextUtils.isEmpty(art) ? icon(fallback) : art;
    }

    static MediaItem playable(@NonNull String id, @NonNull String title, @Nullable String subtitle, @Nullable String art) {
        return build(id, false, true, MediaMetadata.MEDIA_TYPE_VIDEO, title, subtitle, artwork(art, R.drawable.ic_browse_movie), null);
    }

    static MediaItem stream(@NonNull BrowsePlayback playback, @NonNull String title, @Nullable String subtitle, @Nullable String art) {
        return build(playback.getMediaId(), false, true, MediaMetadata.MEDIA_TYPE_VIDEO, title, subtitle, artwork(art, R.drawable.ic_browse_movie), Uri.parse(playback.getResult().getRealUrl())).buildUpon().setTag(playback).build();
    }

    private static MediaItem build(@NonNull String id, boolean browsable, boolean playable, int mediaType, @NonNull String title, @Nullable String subtitle, @Nullable String art, @Nullable Uri uri) {
        MediaMetadata.Builder metadata = new MediaMetadata.Builder().setTitle(title).setIsBrowsable(browsable).setIsPlayable(playable).setMediaType(mediaType);
        if (!TextUtils.isEmpty(subtitle)) metadata.setSubtitle(subtitle);
        if (!TextUtils.isEmpty(art)) metadata.setArtworkUri(MediaItemFactory.getArtworkUri(art));
        if (uri != null && !TextUtils.isEmpty(subtitle)) metadata.setArtist(subtitle);
        MediaItem.Builder builder = new MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata.build());
        if (uri != null) builder.setUri(uri);
        return builder.build();
    }
}
