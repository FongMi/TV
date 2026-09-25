package com.fongmi.android.tv.api;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.player.media.LocalSubtitleScanner;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Trans;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
        SpiderDebug.log("detail", "key=%s,id=%s", key, id);
        if (LOCAL.equals(key)) return directDetail(id, "", R.string.local);
        Site site = VodConfig.get().getSite(key);
        if (site.isEmpty() && PUSH.equals(key)) return directDetail(id, id, R.string.push);
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
        Source.get().parse(result.getVod().setFlags());
        return result;
    }

    @NonNull
    public static Result playerContent(@NonNull String key, @NonNull String flag, @NonNull String id) throws Exception {
        SpiderDebug.log("player", "key=%s,flag=%s,id=%s", key, flag, id);
        Source.get().stop();
        if (LOCAL.equals(key)) return directPlayer(key, flag, id);
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
            if (result.getFlag().isEmpty()) result.setFlag(flag);
            result.setUrl(Source.get().fetch(result));
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
        result.setUrl(Source.get().fetch(result));
        SpiderDebug.log("player", result.toString());
        return result;
    }

    private static Result directDetail(String id, String name, @StringRes int source) throws Exception {
        Vod vod = new Vod();
        vod.setId(id);
        vod.setName(name);
        vod.setPlayUrl(id);
        vod.setPlayFrom(ResUtil.getString(source));
        vod.setPic(ResUtil.getString(R.string.push_image));
        Source.get().parse(vod.setFlags());
        return Result.vod(vod);
    }

    private static Result directPlayer(String key, String flag, String id) throws Exception {
        Result result = new Result();
        result.setUrl(id);
        result.setKey(key);
        result.setFlag(flag);
        result.setParse(PUSH.equals(key) && isWebPage(id) ? 1 : 0);
        if (LOCAL.equals(key)) result.setSubs(LocalSubtitleScanner.scan(id));
        result.setUrl(Source.get().fetch(result));
        SpiderDebug.log("player", result.toString());
        return result;
    }

    static boolean isWebPage(String url) {
        String path = UrlUtil.stripQueryAndFragment(url).toLowerCase(Locale.ROOT);
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
