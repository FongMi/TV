package com.fongmi.android.tv.api;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.api.loader.SpiderTestSession;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.player.media.LocalSubtitleScanner;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Trans;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.UriUtil;
import com.github.catvod.utils.Util;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Response;

public class SiteApi {

    public static final String LOCAL = "local_file";
    public static final String PUSH = "push_agent";

    public static String call(@NonNull Site site, @NonNull ArrayMap<String, String> params) throws IOException {
        if (!site.getExt().isEmpty()) params.put("extend", site.getExt());
        Call call = site.getExt().length() <= 1000 ? OkHttp.newCall(site.getApi(), site.getHeader(), params) : OkHttp.newCall(site.getApi(), site.getHeader(), OkHttp.toBody(params));
        try (Response response = call.execute()) {
            return response.body().string();
        }
    }

    private static boolean isSpider(@NonNull Site site) {
        return site.getType() == 3;
    }

    private static String ac(int type) {
        return type == 0 ? "videolist" : "detail";
    }

    @NonNull
    public static AutoCloseable danmaku(@NonNull Uri uri, @NonNull Consumer<JSONObject> emit) throws Exception {
        String key = uri.getAuthority();
        List<String> ids = uri.getPathSegments();
        if (!"spider".equalsIgnoreCase(uri.getScheme()) || key == null || key.isEmpty() || ids.size() != 1 || ids.get(0).isEmpty() || uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException("Invalid live danmaku source");
        String id = ids.get(0);
        SpiderDebug.log("danmaku", "key=%s,id=%s", key, id);
        AutoCloseable subscription = BaseLoader.get().getSpider(key).danmaku(id, emit);
        if (subscription == null) throw new IllegalStateException("danmaku must return a subscription");
        return subscription;
    }

    @NonNull
    public static Result homeContent(@NonNull Site site) throws Exception {
        Result result;
        if (isSpider(site)) {
            Spider spider = site.recent().spider();
            boolean crash = Prefers.getBoolean("crash");
            String home = crash ? "" : spider.homeContent(true);
            String video = crash ? "" : spider.homeVideoContent();
            Prefers.put("crash", false);
            SpiderDebug.log("home", home);
            SpiderDebug.log("homeVideo", video);
            result = Result.fromJson(home);
            List<Vod> list = Result.fromJson(video).getList();
            if (!list.isEmpty()) result.setList(list);
        } else if (site.getType() == 4) {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("filter", "true");
            String homeContent = call(site.fetchExt(), params);
            SpiderDebug.log("home", homeContent);
            result = Result.fromJson(homeContent);
        } else {
            try (Response response = OkHttp.newCall(site.getApi(), site.getHeader()).execute()) {
                String homeContent = response.body().string();
                SpiderDebug.log("home", homeContent);
                result = fetchPic(site, Result.fromType(site.getType(), homeContent));
            }
        }
        setTypes(site, result);
        return result;
    }

    @NonNull
    public static Result categoryContent(@NonNull String key, @NonNull String tid, @NonNull String page, boolean filter, @NonNull HashMap<String, String> extend) throws Exception {
        SpiderDebug.log("category", "key=%s,tid=%s,page=%s,filter=%s,extend=%s", key, tid, page, filter, extend);
        Site site = VodConfig.get().getSite(key);
        boolean spider = isSpider(site);
        String categoryContent;
        if (spider) {
            categoryContent = site.recent().spider().categoryContent(tid, page, filter, extend);
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            if (site.getType() == 1 && !extend.isEmpty()) params.put("f", App.gson().toJson(extend));
            if (site.getType() == 4) params.put("ext", Util.base64(App.gson().toJson(extend), Util.URL_SAFE));
            params.put("ac", ac(site.getType()));
            params.put("t", tid);
            params.put("pg", page);
            categoryContent = call(site, params);
        }
        SpiderDebug.log("category", categoryContent);
        return spider ? Result.fromJson(categoryContent) : Result.fromType(site.getType(), categoryContent);
    }

    @NonNull
    public static Result detailContent(@NonNull String key, @NonNull String id) throws Exception {
        return detailContent(key, id, false);
    }

    @NonNull
    public static Result detailContent(@NonNull String key, @NonNull String id, boolean builtin) throws Exception {
        if (PUSH.equals(key) && SpiderTestSession.isPreview(id)) {
            SpiderTestSession.previewResult(id);
            return directDetail(id, id, ResUtil.getString(R.string.spider_test_title), R.string.push);
        }
        SpiderDebug.log("detail", "key=%s,id=%s", key, id);
        if (LOCAL.equals(key)) return directDetail(id, id, "", R.string.local);
        String originalId = id;
        boolean push = PUSH.equals(key) && Source.isPush(id);
        if (push) id = id.substring(7);
        if (PUSH.equals(key) && builtin && !push) return directDetail(originalId, id, id, R.string.push);
        Site site = VodConfig.get().getSite(key);
        if (site.isEmpty() && PUSH.equals(key)) return directDetail(originalId, id, id, R.string.push);
        boolean spider = isSpider(site);
        String detailContent;
        if (spider) {
            detailContent = site.recent().spider().detailContent(Arrays.asList(id));
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("ac", ac(site.getType()));
            params.put("ids", id);
            detailContent = call(site, params);
        }
        SpiderDebug.log("detail", detailContent);
        Result result = spider ? Result.fromJson(detailContent) : Result.fromType(site.getType(), detailContent);
        if (push) result.getVod().setId(originalId);
        Source.get().parse(result.getVod().setFlags());
        return result;
    }

    @NonNull
    public static Result playerContent(@NonNull String key, @NonNull String flag, @NonNull String id) throws Exception {
        return playerContent(key, flag, id, false);
    }

    @NonNull
    public static Result playerContent(@NonNull String key, @NonNull String flag, @NonNull String id, boolean builtin) throws Exception {
        return playerContent(key, flag, id, builtin, -1);
    }

    @NonNull
    public static Result playerContent(@NonNull String key, @NonNull String flag, @NonNull String id, boolean builtin, int qualityPosition) throws Exception {
        SpiderDebug.log("player", "key=%s,flag=%s,id=%s", key, flag, id);
        Source.get().stop();
        Result result = loadPlayerContent(key, flag, id, builtin);
        if (qualityPosition >= 0 && !result.getUrl().getValues().isEmpty()) result.getUrl().set(qualityPosition);
        result.setUrl(Source.get().fetch(result));
        SpiderDebug.log("player", result.toString());
        return result;
    }

    @NonNull
    public static Result preloadContent(@NonNull String key, @NonNull String flag, @NonNull String id) throws Exception {
        return preloadContent(key, flag, id, false);
    }

    @NonNull
    public static Result preloadContent(@NonNull String key, @NonNull String flag, @NonNull String id, boolean builtin) throws Exception {
        SpiderDebug.log("preload", "key=%s,flag=%s,id=%s", key, flag, id);
        return loadPlayerContent(key, flag, id, builtin);
    }

    @NonNull
    private static Result loadPlayerContent(@NonNull String key, @NonNull String flag, @NonNull String id, boolean builtin) throws Exception {
        if (PUSH.equals(key) && SpiderTestSession.isPreview(id)) {
            Result result = Result.fromJson(SpiderTestSession.previewResult(id));
            result.setKey(key);
            if (result.getFlag().isEmpty()) result.setFlag(flag);
            return result;
        }
        if (LOCAL.equals(key) || PUSH.equals(key) && builtin) return directPlayer(key, flag, id);
        Site site = VodConfig.get().getSite(key);
        if (site.getType() == 3 || site.getType() == 4) {
            String playerContent;
            if (site.getType() == 3) {
                playerContent = site.recent().spider().playerContent(flag, id, VodConfig.get().getFlags());
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("play", id);
                params.put("flag", flag);
                playerContent = call(site, params);
            }
            SpiderDebug.log("player", playerContent);
            Result result = Result.fromJson(playerContent);
            result.getVod().setFlags();
            if (result.getFlag().isEmpty()) result.setFlag(flag);
            result.setHeader(site.getHeader());
            result.setKey(key);
            return result;
        }
        if (site.isEmpty() && PUSH.equals(key)) return directPlayer(key, flag, id);
        Result result = new Result();
        result.setUrl(id);
        result.setKey(key);
        result.setFlag(flag);
        result.setHeader(site.getHeader());
        result.setPlayUrl(site.getPlayUrl());
        result.setParse(Sniffer.isVideoFormat(id) && result.getPlayUrl().isEmpty() ? 0 : 1);
        return result;
    }

    private static Result directDetail(String id, String target, String name, @StringRes int source) throws Exception {
        Vod vod = new Vod();
        vod.setId(id);
        vod.setName(name);
        vod.setPic(ResUtil.getString(R.string.push_image));
        Flag flag = Flag.create(ResUtil.getString(source));
        String[] lines = source == R.string.push ? target.split("\\R") : new String[]{target};
        for (String line : lines) addDirectEpisode(flag, line, source);
        vod.getFlags().add(flag);
        Source.get().parse(vod);
        return Result.vod(vod);
    }

    private static void addDirectEpisode(Flag flag, String line, @StringRes int source) {
        String url = line.trim();
        if (url.isEmpty()) return;
        String number = String.format(Locale.getDefault(), "%02d", flag.getEpisodes().size() + 1);
        int delimiter = url.indexOf('$');
        if (source == R.string.push && isNamedLine(url)) {
            String title = url.substring(0, delimiter).trim();
            flag.getEpisodes().add(Episode.create(title.isEmpty() ? number : title, url.substring(delimiter + 1).trim()));
        } else {
            flag.getEpisodes().add(Episode.create(number, url));
        }
    }

    private static boolean isNamedLine(String url) {
        int delimiter = url.indexOf('$');
        if (delimiter < 0) return false;
        int colon = url.indexOf(':');
        if (delimiter < colon) return true;
        if (colon >= 0 && url.startsWith("?", colon + 1)) return false;
        int scheme = url.indexOf("://");
        return scheme >= 0 ? delimiter < scheme : Sniffer.AI_PUSH.matcher(url.substring(delimiter + 1).trim()).lookingAt();
    }

    private static Result directPlayer(String key, String flag, String id) throws Exception {
        Result result = new Result();
        result.setUrl(id);
        result.setKey(key);
        result.setFlag(flag);
        result.setParse(PUSH.equals(key) && isWebPage(id) ? 1 : 0);
        if (LOCAL.equals(key)) result.setSubs(LocalSubtitleScanner.scan(id));
        return result;
    }

    static boolean isWebPage(String url) {
        String path = UriUtil.stripQueryAndFragment(url).toLowerCase(Locale.ROOT);
        return path.endsWith(".htm") || path.endsWith(".html") || path.endsWith(".shtm") || path.endsWith(".shtml");
    }

    @NonNull
    public static Result searchContent(@NonNull Site site, @NonNull String keyword, boolean quick, @NonNull String page) throws Exception {
        keyword = Trans.convert(keyword, site.getLang());
        SpiderDebug.log("search", "site=%s,keyword=%s,quick=%s,page=%s", site.getName(), keyword, quick, page);
        boolean hasPage = !page.equals("1");
        boolean spider = isSpider(site);
        String searchContent;
        if (spider) {
            searchContent = hasPage ? site.spider().searchContent(keyword, quick, page) : site.spider().searchContent(keyword, quick);
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("wd", keyword);
            params.put("quick", String.valueOf(quick));
            params.put("extend", "");
            if (hasPage) params.put("pg", page);
            searchContent = call(site, params);
        }
        SpiderDebug.log("search", searchContent);
        Result result = spider ? Result.fromJson(searchContent) : fetchPic(site, Result.fromType(site.getType(), searchContent));
        for (Vod vod : result.getList()) vod.setSite(site);
        return result;
    }

    @NonNull
    public static Result action(@NonNull String key, @NonNull String action) throws Exception {
        Site site = VodConfig.get().getSite(key);
        SpiderDebug.log("action", "key=%s,action=%s", key, action);
        if (site.getType() == 3) return Result.fromJson(site.recent().spider().action(action));
        if (site.getType() == 4) return Result.fromJson(OkHttp.string(action));
        return Result.empty();
    }

    @NonNull
    public static Result fetchPic(@NonNull Site site, @NonNull Result result) throws Exception {
        if (site.getType() > 2 || result.getList().isEmpty() || !result.getVod().getPic().isEmpty()) return result;
        ArrayList<String> ids = new ArrayList<>();
        boolean empty = site.getCategories().isEmpty();
        for (Vod item : result.getList()) if (empty || site.getCategories().contains(item.getTypeName())) ids.add(item.getId());
        if (ids.isEmpty()) return result.clear();
        ArrayMap<String, String> params = new ArrayMap<>();
        params.put("ac", ac(site.getType()));
        params.put("ids", TextUtils.join(",", ids));
        try (Response response = OkHttp.newCall(site.getApi(), site.getHeader(), params).execute()) {
            result.setList(Result.fromType(site.getType(), response.body().string()).getList());
            return result;
        }
    }

    private static void setTypes(@NonNull Site site, @NonNull Result result) {
        result.getTypes().stream().filter(type -> result.getFilters().containsKey(type.getTypeId())).forEach(type -> type.setFilters(result.getFilters().get(type.getTypeId())));
        if (site.getCategories().isEmpty()) return;
        Map<String, Class> typeByName = new HashMap<>();
        result.getTypes().forEach(type -> typeByName.put(type.getTypeName(), type));
        List<Class> types = site.getCategories().stream().map(typeByName::get).filter(Objects::nonNull).toList();
        if (!types.isEmpty()) result.setTypes(types);
    }
}
