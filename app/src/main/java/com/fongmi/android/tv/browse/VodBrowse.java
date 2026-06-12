package com.fongmi.android.tv.browse;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.utils.Trans;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

class VodBrowse {

    static final String VOD_EP = "VE:";
    static final String VOD_PLAY = "VP:";
    static final String VOD_SEARCH = "VS:";

    private static final int SEARCH_LIMIT = 50;
    private static final int SEARCH_TIMEOUT = 5;
    private static final Map<String, Vod> vodCache = new ConcurrentHashMap<>();
    private static final Map<String, String> epNavMap = new ConcurrentHashMap<>();
    private static final Map<String, EpEntry> epEntries = new ConcurrentHashMap<>();
    private static final Map<String, Integer> epCountMap = new ConcurrentHashMap<>();

    private static volatile History browseHistory;
    private static volatile ImmutableList<MediaItem> searchCache = ImmutableList.of();

    static void clear() {
        vodCache.clear();
        epNavMap.clear();
        epEntries.clear();
        epCountMap.clear();
        browseHistory = null;
        searchCache = ImmutableList.of();
    }

    @NonNull
    static ImmutableList<MediaItem> getHistory() {
        return History.get().stream().map(history -> BrowseTree.playable(VOD_PLAY + history.getKey(), history.getVodName(), history.getVodRemarks(), history.getVodPic())).collect(ImmutableList.toImmutableList());
    }

    @NonNull
    static ImmutableList<MediaItem> search(@NonNull String query) {
        VodConfig.get().ensureLoaded();
        String keyword = Trans.t2s(query);
        List<Site> sites = VodConfig.get().getSites().stream().filter(Site::isSearchable).toList();
        List<ListenableFuture<List<MediaItem>>> futures = sites.stream().map(site -> Task.largeExecutor().submit(() -> searchSite(site, keyword))).toList();
        List<MediaItem> items = collectResults(futures);
        items.sort((a, b) -> matchScore(b, keyword) - matchScore(a, keyword));
        searchCache = ImmutableList.copyOf(items.subList(0, Math.min(items.size(), SEARCH_LIMIT)));
        return searchCache;
    }

    private static List<MediaItem> searchSite(@NonNull Site site, @NonNull String keyword) throws Exception {
        Result result = SiteApi.searchContent(site, keyword, false, "1");
        return result.getList().stream().map(vod -> BrowseTree.playable(VOD_SEARCH + site.getKey() + "|" + vod.getId(), vod.getName(), vod.getRemarks(), vod.getPic())).toList();
    }

    private static List<MediaItem> collectResults(@NonNull List<ListenableFuture<List<MediaItem>>> futures) {
        List<MediaItem> items = new ArrayList<>();
        for (ListenableFuture<List<MediaItem>> future : futures) {
            try {
                List<MediaItem> result = future.get(SEARCH_TIMEOUT, TimeUnit.SECONDS);
                if (result != null) items.addAll(result);
                if (items.size() >= SEARCH_LIMIT) break;
            } catch (Exception ignored) {
            }
        }
        return items;
    }

    @NonNull
    static ImmutableList<MediaItem> getSearchResult() {
        return searchCache;
    }

    @Nullable
    static MediaItem resolve(@NonNull String mediaId) throws Exception {
        if (mediaId.startsWith(VOD_PLAY)) return resolvePlay(mediaId);
        if (mediaId.startsWith(VOD_EP)) return resolveEp(mediaId);
        if (mediaId.startsWith(VOD_SEARCH)) return resolveSearch(mediaId);
        return null;
    }

    @Nullable
    static MediaItem navigate(@NonNull String mediaId, int delta) throws Exception {
        if (mediaId.startsWith(VOD_EP)) return navigateEpisode(mediaId, delta);
        String epId = ensureEpLoaded(mediaId);
        if (epId != null) return navigateEpisode(epId, delta);
        return null;
    }

    static long consumeResumePosition() {
        if (browseHistory == null) return C.TIME_UNSET;
        History history = browseHistory;
        long position = history.getPosition();
        history.setPosition(0);
        return position > 0 ? position : C.TIME_UNSET;
    }

    static boolean saveProgress(long position, long duration) {
        if (browseHistory == null || Setting.isIncognito()) return false;
        History history = browseHistory;
        history.setPosition(position);
        history.setDuration(duration);
        history.setCreateTime(System.currentTimeMillis());
        if (history.canSave()) Task.execute(() -> history.merge().save());
        return true;
    }

    @Nullable
    private static MediaItem resolvePlay(@NonNull String mediaId) throws Exception {
        String historyKey = mediaId.substring(VOD_PLAY.length());
        String currentEpId = ensureEpLoaded(historyKey);
        return currentEpId != null ? resolveEp(currentEpId) : null;
    }

    @Nullable
    private static MediaItem resolveSearch(@NonNull String mediaId) throws Exception {
        String body = mediaId.substring(VOD_SEARCH.length());
        int sep = body.indexOf('|');
        if (sep < 0) return null;
        String siteKey = body.substring(0, sep);
        String vodId = body.substring(sep + 1);
        String historyKey = siteKey + AppDatabase.SYMBOL + vodId;
        History existing = History.find(historyKey);
        if (existing != null) return resolveWithHistory(historyKey, existing);
        VodConfig.get().ensureLoaded();
        Vod vod = SiteApi.detailContent(siteKey, vodId).getVod();
        if (TextUtils.isEmpty(vod.getId()) || vod.getFlags().isEmpty()) return null;
        vodCache.put(historyKey, vod);
        History history = createHistory(historyKey, vod);
        Flag resumeFlag = findFlag(vod, history.getVodFlag());
        if (resumeFlag == null || resumeFlag.getEpisodes().isEmpty()) return null;
        int currentIdx = buildEpIndex(historyKey, resumeFlag, history);
        browseHistory = history;
        String epId = epNavMap.get(epNavKey(historyKey, resumeFlag.getFlag(), currentIdx));
        return epId != null ? resolveEp(epId) : null;
    }

    @Nullable
    private static MediaItem resolveWithHistory(@NonNull String historyKey, @NonNull History history) throws Exception {
        String epId = ensureEpLoaded(historyKey, history);
        return epId != null ? resolveEp(epId) : null;
    }

    private static History createHistory(@NonNull String historyKey, @NonNull Vod vod) {
        History history = new History();
        history.setKey(historyKey);
        history.setCid(VodConfig.getCid());
        history.setVodName(vod.getName());
        history.setVodPic(vod.getPic());
        history.findEpisode(vod.getFlags());
        return history;
    }

    @Nullable
    private static String ensureEpLoaded(@NonNull String historyKey) throws Exception {
        return ensureEpLoaded(historyKey, History.find(historyKey));
    }

    @Nullable
    private static String ensureEpLoaded(@NonNull String historyKey, @Nullable History history) throws Exception {
        if (history == null) return null;
        Vod vod = getOrFetchVod(historyKey, history);
        if (vod == null) return null;
        Flag flag = findFlag(vod, history.getVodFlag());
        if (flag == null || flag.getEpisodes().isEmpty()) return null;
        int currentIdx = buildEpIndex(historyKey, flag, history);
        browseHistory = history;
        return epNavMap.get(epNavKey(historyKey, flag.getFlag(), currentIdx));
    }

    private static int buildEpIndex(@NonNull String historyKey, @NonNull Flag flag, @NonNull History history) {
        String prefix = historyKey + "|";
        epEntries.values().removeIf(entry -> entry.historyKey.equals(historyKey));
        epNavMap.keySet().removeIf(key -> key.startsWith(prefix));
        epCountMap.keySet().removeIf(key -> key.startsWith(prefix));
        String flagName = flag.getFlag();
        List<Episode> episodes = flag.getEpisodes();
        epCountMap.put(historyKey + '|' + flagName, episodes.size());
        IntStream.range(0, episodes.size()).forEach(i -> indexEpisode(historyKey, flagName, i, history));
        return findCurrentIndex(flag, history);
    }

    private static void indexEpisode(@NonNull String historyKey, @NonNull String flagName, int index, @NonNull History history) {
        String key = epNavKey(historyKey, flagName, index);
        String id = VOD_EP + key;
        epEntries.put(id, new EpEntry(historyKey, flagName, index, history.getSiteKey(), history.getVodPic()));
        epNavMap.put(key, id);
    }

    private static int findCurrentIndex(@NonNull Flag flag, @NonNull History history) {
        String currentUrl = history.getEpisode() != null ? history.getEpisode().getUrl() : null;
        if (TextUtils.isEmpty(currentUrl)) return 0;
        List<Episode> episodes = flag.getEpisodes();
        return IntStream.range(0, episodes.size()).filter(i -> episodes.get(i).getUrl().equals(currentUrl)).findFirst().orElse(0);
    }

    @Nullable
    private static MediaItem navigateEpisode(@NonNull String mediaId, int delta) throws Exception {
        EpEntry current = epEntries.get(mediaId);
        if (current == null) return null;
        Integer count = epCountMap.get(current.historyKey + '|' + current.flagName);
        if (count == null || count == 0) return null;
        int target = BrowseTree.wrapIndex(current.index, delta, count);
        String nextId = epNavMap.get(epNavKey(current.historyKey, current.flagName, target));
        if (nextId == null) return null;
        if (browseHistory != null) browseHistory.setPosition(0);
        return resolveEp(nextId);
    }

    @Nullable
    private static MediaItem resolveEp(@NonNull String mediaId) throws Exception {
        EpEntry entry = epEntries.get(mediaId);
        if (entry == null) return null;
        Vod vod = getOrFetchVod(entry.historyKey);
        if (vod == null) return null;
        Flag flag = findFlag(vod, entry.flagName);
        if (flag == null || entry.index >= flag.getEpisodes().size()) return null;
        Episode episode = flag.getEpisodes().get(entry.index);
        Result result = SiteApi.playerContent(entry.siteKey, entry.flagName, episode.getUrl());
        if (TextUtils.isEmpty(result.getRealUrl())) return null;
        updateHistory(episode);
        BrowseTree.putBrowseResult(mediaId, result);
        String vodName = vod.getName();
        if (TextUtils.isEmpty(vodName) && browseHistory != null) vodName = browseHistory.getVodName();
        return BrowseTree.stream(mediaId, result.getRealUrl(), vodName, episode.getName(), entry.vodPic, episode.getUrl());
    }

    private static void updateHistory(@NonNull Episode episode) {
        if (browseHistory == null) return;
        browseHistory.setVodRemarks(episode.getName());
        browseHistory.setEpisodeUrl(episode.getUrl());
    }

    @Nullable
    private static Vod getOrFetchVod(@NonNull String historyKey) throws Exception {
        Vod vod = vodCache.get(historyKey);
        if (vod != null) return vod;
        return getOrFetchVod(historyKey, History.find(historyKey));
    }

    @Nullable
    private static Vod getOrFetchVod(@NonNull String historyKey, @Nullable History history) throws Exception {
        Vod vod = vodCache.get(historyKey);
        if (vod != null) return vod;
        if (history == null) return null;
        vod = fetchDetail(history);
        if (vod == null) return null;
        vodCache.put(historyKey, vod);
        return vod;
    }

    @Nullable
    private static Vod fetchDetail(@NonNull History history) throws Exception {
        VodConfig.get().ensureLoaded();
        Vod vod = SiteApi.detailContent(history.getSiteKey(), history.getVodId()).getVod();
        return TextUtils.isEmpty(vod.getId()) ? null : vod;
    }

    @Nullable
    private static Flag findFlag(@NonNull Vod vod, @Nullable String name) {
        if (vod.getFlags().isEmpty()) return null;
        if (!TextUtils.isEmpty(name)) return vod.getFlags().stream().filter(flag -> flag.getFlag().equals(name)).findFirst().orElse(vod.getFlags().get(0));
        return vod.getFlags().get(0);
    }

    private static int matchScore(@NonNull MediaItem item, @NonNull String keyword) {
        CharSequence title = item.mediaMetadata.title;
        if (title == null) return 0;
        String name = Trans.t2s(title.toString());
        if (name.equals(keyword)) return 2;
        if (name.contains(keyword)) return 1;
        return 0;
    }

    @NonNull
    private static String epNavKey(@NonNull String historyKey, @NonNull String flagName, int index) {
        return historyKey + '|' + flagName + '|' + index;
    }

    private record EpEntry(String historyKey, String flagName, int index, String siteKey, String vodPic) {
    }
}
