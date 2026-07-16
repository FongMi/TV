package com.fongmi.android.tv.browse;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.playback.vod.VodHistoryPolicy;
import com.fongmi.android.tv.setting.Setting;
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
    private static final char KEY_SEPARATOR = '|';
    private static final VodHistoryPolicy policy = new VodHistoryPolicy();
    private static final Map<String, Vod> vodCache = new ConcurrentHashMap<>();
    private static final Map<String, String> epNavMap = new ConcurrentHashMap<>();
    private static final Map<String, EpEntry> epEntries = new ConcurrentHashMap<>();
    private static final Map<String, Integer> epCountMap = new ConcurrentHashMap<>();
    private static final Map<String, MediaItem> searchItemMap = new ConcurrentHashMap<>();
    private static final Map<String, ImmutableList<MediaItem>> searchCacheMap = new ConcurrentHashMap<>();

    private static volatile History browseHistory;

    static void clear() {
        vodCache.clear();
        epNavMap.clear();
        epEntries.clear();
        epCountMap.clear();
        browseHistory = null;
        searchItemMap.clear();
        searchCacheMap.clear();
    }

    @NonNull
    static ImmutableList<MediaItem> getHistory() {
        return History.get().stream().map(VodBrowse::historyItem).collect(ImmutableList.toImmutableList());
    }

    @NonNull
    static ImmutableList<MediaItem> search(@NonNull String query) {
        VodConfig.get().ensureLoaded();
        String keyword = searchKey(query);
        if (TextUtils.isEmpty(keyword)) return ImmutableList.of();
        List<Site> sites = VodConfig.get().getSites().stream().filter(Site::isSearchable).toList();
        List<ListenableFuture<List<MediaItem>>> futures = sites.stream().map(site -> Task.largeExecutor().submit(() -> searchSite(site, keyword))).toList();
        List<MediaItem> items = collectResults(futures);
        items.sort((a, b) -> matchScore(b, keyword) - matchScore(a, keyword));
        ImmutableList<MediaItem> results = ImmutableList.copyOf(items.subList(0, Math.min(items.size(), SEARCH_LIMIT)));
        searchCacheMap.put(keyword, results);
        results.forEach(item -> searchItemMap.put(item.mediaId, item));
        return results;
    }

    private static List<MediaItem> searchSite(@NonNull Site site, @NonNull String keyword) throws Exception {
        Result result = SiteApi.searchContent(site, keyword, false, "1");
        return result.getList().stream().map(vod -> BrowseTree.playable(searchId(site.getKey(), vod.getId()), vod.getName(), vod.getRemarks(), vod.getPic())).toList();
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
    static ImmutableList<MediaItem> getSearchResult(@NonNull String query, int page, int pageSize) {
        return BrowseTree.page(searchCacheMap.getOrDefault(searchKey(query), ImmutableList.of()), page, pageSize);
    }

    @Nullable
    static MediaItem getItem(@NonNull String mediaId) {
        if (mediaId.startsWith(VOD_EP)) return getEpisodeItem(mediaId);
        if (mediaId.startsWith(VOD_PLAY)) return getHistoryItem(mediaId);
        if (mediaId.startsWith(VOD_SEARCH)) return getSearchItem(mediaId);
        return null;
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
        policy.save(history);
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
        SearchEntry search = SearchEntry.parse(mediaId);
        if (search == null) return null;
        String historyKey = historyKey(search.siteKey, search.vodId);
        History existing = History.find(historyKey);
        if (existing != null) return resolveWithHistory(historyKey, existing);
        VodConfig.get().ensureLoaded();
        Vod vod = SiteApi.detailContent(search.siteKey, search.vodId).getVod();
        if (TextUtils.isEmpty(vod.getId()) || vod.getFlags().isEmpty()) return null;
        vodCache.put(historyKey, vod);
        History history = createHistory(historyKey, vod);
        Flag resumeFlag = findFlag(vod, history.getVodFlag());
        if (resumeFlag == null || resumeFlag.getEpisodes().isEmpty()) return null;
        int currentIdx = buildEpIndex(historyKey, resumeFlag, history);
        browseHistory = history;
        String epId = epNavMap.get(epNavKey(historyKey, resumeFlag.getFlag(), currentIdx));
        MediaItem item = epId != null ? resolveEp(epId) : null;
        if (item != null) saveCreatedHistory(history);
        return item;
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
        String prefix = historyKey + KEY_SEPARATOR;
        epEntries.values().removeIf(entry -> entry.historyKey.equals(historyKey));
        epNavMap.keySet().removeIf(key -> key.startsWith(prefix));
        epCountMap.keySet().removeIf(key -> key.startsWith(prefix));
        String flagName = flag.getFlag();
        List<Episode> episodes = flag.getEpisodes();
        epCountMap.put(epCountKey(historyKey, flagName), episodes.size());
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
        Integer count = epCountMap.get(epCountKey(current.historyKey, current.flagName));
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
        return BrowseTree.stream(mediaId, result.getRealUrl(), vodName(vod), episode.getName(), entry.vodPic);
    }

    private static void updateHistory(@NonNull Episode episode) {
        if (browseHistory == null) return;
        browseHistory.setVodRemarks(episode.getName());
        browseHistory.setEpisodeUrl(episode.getUrl());
    }

    private static void saveCreatedHistory(@NonNull History history) {
        if (Setting.isIncognito()) return;
        long position = history.getPosition();
        long duration = history.getDuration();
        history.setCreateTime(System.currentTimeMillis());
        history.setPosition(duration > 0 ? Math.max(0, position) : 0);
        history.setDuration(Math.max(0, duration));
        history.save();
        history.setPosition(position);
        history.setDuration(duration);
    }

    @Nullable
    private static MediaItem getHistoryItem(@NonNull String mediaId) {
        History history = History.find(mediaId.substring(VOD_PLAY.length()));
        return history == null ? null : historyItem(history);
    }

    @Nullable
    private static MediaItem getSearchItem(@NonNull String mediaId) {
        return searchItemMap.get(mediaId);
    }

    @Nullable
    private static MediaItem getEpisodeItem(@NonNull String mediaId) {
        EpEntry entry = epEntries.get(mediaId);
        if (entry == null) return null;
        Vod vod = vodCache.get(entry.historyKey);
        if (vod == null) return null;
        Flag flag = findFlag(vod, entry.flagName);
        if (flag == null || entry.index >= flag.getEpisodes().size()) return null;
        Episode episode = flag.getEpisodes().get(entry.index);
        return BrowseTree.playable(mediaId, vodName(vod), episode.getName(), entry.vodPic);
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
    private static String searchKey(@NonNull String query) {
        return Trans.t2s(query).trim();
    }

    @NonNull
    private static String historyKey(@NonNull String siteKey, @NonNull String vodId) {
        return siteKey + AppDatabase.SYMBOL + vodId + AppDatabase.SYMBOL + VodConfig.getCid();
    }

    @NonNull
    private static String epNavKey(@NonNull String historyKey, @NonNull String flagName, int index) {
        return epCountKey(historyKey, flagName) + KEY_SEPARATOR + index;
    }

    @NonNull
    private static String epCountKey(@NonNull String historyKey, @NonNull String flagName) {
        return historyKey + KEY_SEPARATOR + flagName;
    }

    @NonNull
    private static String searchId(@NonNull String siteKey, @NonNull String vodId) {
        return VOD_SEARCH + siteKey + KEY_SEPARATOR + vodId;
    }

    @NonNull
    private static MediaItem historyItem(@NonNull History history) {
        return BrowseTree.playable(VOD_PLAY + history.getKey(), history.getVodName(), history.getVodRemarks(), history.getVodPic());
    }

    @NonNull
    private static String vodName(@NonNull Vod vod) {
        if (!TextUtils.isEmpty(vod.getName())) return vod.getName();
        String name = browseHistory != null ? browseHistory.getVodName() : "";
        return name == null ? "" : name;
    }

    private record SearchEntry(String siteKey, String vodId) {

        @Nullable
        static SearchEntry parse(@NonNull String mediaId) {
            String body = mediaId.substring(VOD_SEARCH.length());
            int sep = body.indexOf(KEY_SEPARATOR);
            if (sep < 0) return null;
            return new SearchEntry(body.substring(0, sep), body.substring(sep + 1));
        }
    }

    private record EpEntry(String historyKey, String flagName, int index, String siteKey, String vodPic) {
    }
}
