package com.fongmi.android.tv.browse;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.LiveApi;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

class LiveBrowse {

    static final String LIVE_GROUP = "LG:";
    static final String LIVE_CH = "LC:";

    @NonNull
    static ImmutableList<MediaItem> getGroups() {
        return liveHome().getGroups().stream().filter(group -> !group.isHidden() && !group.isKeep()).map(group -> BrowseTree.folder(LIVE_GROUP + group.getName(), group.getName(), R.drawable.ic_browse_folder)).collect(ImmutableList.toImmutableList());
    }

    static List<MediaItem> search(String keyword) {
        Set<Channel> seen = new HashSet<>();
        ImmutableList.Builder<MediaItem> items = ImmutableList.builder();
        Live live = liveHome();
        for (Group group : live.getGroups()) {
            if (group.isHidden() || group.isKeep()) continue;
            List<Channel> channels = group.getChannel();
            for (int i = 0; i < channels.size(); i++) {
                Channel channel = channels.get(i);
                if (!BrowseSearch.key(channel.getName()).contains(keyword) && !BrowseSearch.key(channel.getShow()).contains(keyword) && !BrowseSearch.key(channel.getNumber()).contains(keyword)) continue;
                if (seen.add(channel)) items.add(channelItem(live, group.getName(), channel, i));
            }
        }
        return items.build();
    }

    @NonNull
    static ImmutableList<MediaItem> getFavorites() {
        Set<String> names = new HashSet<>();
        Set<String> keeps = new HashSet<>();
        Keep.getLive().forEach(keep -> keeps.add(keep.getKey()));
        ImmutableList.Builder<MediaItem> items = ImmutableList.builder();
        Live live = liveHome();
        for (Group group : live.getGroups()) {
            if (group.isHidden() || group.isKeep()) continue;
            List<Channel> channels = group.getChannel();
            for (int i = 0; i < channels.size(); i++) {
                Channel channel = channels.get(i);
                if (keeps.contains(channel.getName()) && names.add(channel.getName())) items.add(channelItem(live, group.getName(), channel, i));
            }
        }
        return items.build();
    }

    @Nullable
    static MediaMetadata loadMetadata(String mediaId) {
        LiveEntry entry = findEntry(mediaId);
        if (entry == null) return null;
        Live live = entry.live;
        Channel channel = entry.channel;
        if (channel.getData(live.getZoneId()).getList().isEmpty() && !live.getEpgXml().isEmpty()) LiveApi.parseXml(live);
        LiveApi.getEpg(channel, live.getZoneId());
        return metadata(entry.live, channel);
    }

    private static MediaMetadata metadata(Live live, Channel channel) {
        Epg epg = channel.getData(live.getZoneId());
        int current = epg.getInRange();
        EpgData data = current < 0 ? null : epg.getList().get(current);
        return MediaItemFactory.buildLiveMetadata(channel.getShow(), data, BrowseTree.artwork(channel.getLogo(), R.drawable.ic_browse_tv)).buildUpon().setIsBrowsable(false).setIsPlayable(true).build();
    }

    @NonNull
    static ImmutableList<MediaItem> getChannels(@NonNull String parentId) {
        Live live = liveHome();
        Group group = findGroup(live, parentId.substring(LIVE_GROUP.length()));
        if (group == null) return ImmutableList.of();
        List<Channel> channels = group.getChannel();
        return IntStream.range(0, channels.size()).mapToObj(i -> channelItem(live, group.getName(), channels.get(i), i)).collect(ImmutableList.toImmutableList());
    }

    private static MediaItem channelItem(Live live, String groupName, Channel channel, int index) {
        String id = LIVE_CH + groupName + '|' + index;
        String title = channel.getNumber() + " " + channel.getShow();
        return new MediaItem.Builder().setMediaId(id)
                .setMediaMetadata(metadata(live, channel).buildUpon().setTitle(title).setDisplayTitle(title).build()).build();
    }

    @Nullable
    static MediaItem getItem(@NonNull String mediaId) {
        if (mediaId.startsWith(LIVE_GROUP)) {
            String name = mediaId.substring(LIVE_GROUP.length());
            return liveHome().getGroups().stream().filter(group -> !group.isHidden() && group.getName().equals(name)).findFirst().map(group -> BrowseTree.folder(mediaId, name, R.drawable.ic_browse_folder)).orElse(null);
        }
        LiveEntry entry = findEntry(mediaId);
        if (entry == null) return null;
        return channelItem(entry.live, entry.group.getName(), entry.channel, entry.index);
    }

    @Nullable
    static MediaItem resolve(@NonNull String mediaId) throws Exception {
        LiveEntry entry = findEntry(mediaId);
        return entry == null ? null : resolveChannel(entry, mediaId);
    }

    @Nullable
    private static LiveEntry findEntry(@NonNull String mediaId) {
        if (!mediaId.startsWith(LIVE_CH)) return null;
        int separator = mediaId.lastIndexOf('|');
        if (separator < LIVE_CH.length()) return null;
        Live live = liveHome();
        Group group = findGroup(live, mediaId.substring(LIVE_CH.length(), separator));
        if (group == null) return null;
        int index;
        try {
            index = Integer.parseInt(mediaId.substring(separator + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        List<Channel> channels = group.getChannel();
        return index < 0 || index >= channels.size() ? null : new LiveEntry(live, group, channels.get(index), index);
    }

    @Nullable
    private static Group findGroup(Live live, String name) {
        return live.getGroups().stream().filter(group -> !group.isHidden() && group.getName().equals(name)).findFirst().orElse(null);
    }

    @Nullable
    static MediaItem navigate(@NonNull String mediaId, int delta) throws Exception {
        if (mediaId.startsWith(LIVE_CH)) return navigateChannel(mediaId, delta);
        String liveId = ensureLoaded();
        if (liveId != null) return navigateChannel(liveId, delta);
        return null;
    }

    @Nullable
    private static MediaItem navigateChannel(@NonNull String mediaId, int delta) throws Exception {
        LiveEntry current = findEntry(mediaId);
        if (current == null) return null;
        List<Channel> channels = current.group.getChannel();
        int target = BrowseTree.wrapIndex(current.index, delta, channels.size());
        String id = LIVE_CH + current.group.getName() + '|' + target;
        return resolveChannel(new LiveEntry(current.live, current.group, channels.get(target), target), id);
    }

    @Nullable
    private static String ensureLoaded() {
        Live live = liveHome();
        String keep = live.getKeep();
        if (TextUtils.isEmpty(keep)) return null;
        String[] splits = keep.split(AppDatabase.SYMBOL);
        if (splits.length < 2) return null;
        String groupName = splits[0];
        String channelName = splits[1];
        Group group = findGroup(live, groupName);
        if (group == null) return null;
        List<Channel> channels = group.getChannel();
        for (int i = 0; i < channels.size(); i++) if (channels.get(i).getName().equals(channelName)) return LIVE_CH + groupName + '|' + i;
        return null;
    }

    @Nullable
    private static MediaItem resolveChannel(LiveEntry entry, @NonNull String mediaId) throws Exception {
        Channel channel = entry.channel;
        if (TextUtils.isEmpty(channel.getCurrent())) return null;
        Result result = LiveApi.getUrl(channel);
        if (TextUtils.isEmpty(result.getRealUrl())) return null;
        return BrowseTree.stream(new BrowsePlayback(mediaId, result, null, channel.getName()), channel.getShow(), null, channel.getLogo()).buildUpon().setMediaMetadata(metadata(entry.live, channel)).build();
    }

    private static Live liveHome() {
        LiveConfig.get().ensureLoaded();
        return LiveConfig.get().getHome();
    }

    private record LiveEntry(Live live, Group group, Channel channel, int index) {
    }
}
