package com.fongmi.android.tv.model;

import android.net.Uri;
import android.text.TextUtils;

import androidx.collection.ArrayMap;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Danmu;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Url;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Response;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<Episode> episode;
    public MutableLiveData<Result> result;
    public MutableLiveData<Result> player;
    public MutableLiveData<Result> search;
    public MutableLiveData<Danmu> danmaku;
    private ExecutorService executor;

    public SiteViewModel() {
        this.episode = new MutableLiveData<>();
        this.result = new MutableLiveData<>();
        this.player = new MutableLiveData<>();
        this.search = new MutableLiveData<>();
    }

    public void setEpisode(Episode value) {
        episode.setValue(value);
    }

    public void homeContent() {
        execute(result, () -> {
            Site site = VodConfig.get().getHome();
            if (site.getType() == 3) {
                Spider spider = VodConfig.get().getSpider(site);
                String homeContent = spider.homeContent(true);
                SpiderDebug.log(homeContent);
                VodConfig.get().setRecent(site);
                Result result = Result.fromJson(homeContent);
                if (result.getList().size() > 0) return result;
                String homeVideoContent = spider.homeVideoContent();
                SpiderDebug.log(homeVideoContent);
                result.setList(Result.fromJson(homeVideoContent).getList());
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("filter", "true");
                String homeContent = call(site, params, false);
                SpiderDebug.log(homeContent);
                return Result.fromJson(homeContent);
            } else {
                String homeContent = OkHttp.newCall(site.getApi(), site.getHeaders()).execute().body().string();
                SpiderDebug.log(homeContent);
                return fetchPic(site, Result.fromType(site.getType(), homeContent));
            }
        });
    }

    public void categoryContent(String key, String tid, String page, boolean filter, HashMap<String, String> extend) {
        execute(result, () -> {
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = VodConfig.get().getSpider(site);
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.log(categoryContent);
                VodConfig.get().setRecent(site);
                return Result.fromJson(categoryContent);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                if (site.getType() == 1 && !extend.isEmpty()) params.put("f", App.gson().toJson(extend));
                else if (site.getType() == 4) params.put("ext", Util.base64(App.gson().toJson(extend)));
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("t", tid);
                params.put("pg", page);
                String categoryContent = call(site, params, true);
                SpiderDebug.log(categoryContent);
                return Result.fromType(site.getType(), categoryContent);
            }
        });
    }

    public void detailContent(String key, String id) {
        execute(result, () -> {
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = VodConfig.get().getSpider(site);
                String detailContent = spider.detailContent(Arrays.asList(id));
                SpiderDebug.log(detailContent);
                VodConfig.get().setRecent(site);
                Result result = Result.fromJson(detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                if (!result.getList().isEmpty()) checkThunder(result.getList().get(0).getVodFlags());
                return result;
            } else if (site.isEmpty() && key.equals("push_agent")) {
                Vod vod = new Vod();
                vod.setVodId(id);
                vod.setVodName(id);
                vod.setVodPic("https://pic.rmb.bdstatic.com/bjh/1d0b02d0f57f0a42201f92caba5107ed.jpeg");
                vod.setVodFlags(Flag.create(ResUtil.getString(R.string.push), ResUtil.getString(R.string.play), id));
                checkThunder(vod.getVodFlags());
                return Result.vod(vod);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", id);
                String detailContent = call(site, params, true);
                SpiderDebug.log(detailContent);
                Result result = Result.fromType(site.getType(), detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                if (!result.getList().isEmpty()) checkThunder(result.getList().get(0).getVodFlags());
                return result;
            }
        });
    }

    public void playerContent(String key, String flag, String id) {
        execute(player, () -> {
            Source.get().stop();
            Site site = VodConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = VodConfig.get().getSpider(site);
                String playerContent = spider.playerContent(flag, id, VodConfig.get().getFlags());
                SpiderDebug.log(playerContent);
                VodConfig.get().setRecent(site);
                Result result = Result.fromJson(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                result.setUrl(Source.get().fetch(result));
                result.setHeader(site.getHeader());
                result.setKey(key);
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("play", id);
                params.put("flag", flag);
                String playerContent = call(site, params, true);
                SpiderDebug.log(playerContent);
                Result result = Result.fromJson(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                result.setUrl(Source.get().fetch(result));
                result.setHeader(site.getHeader());
                return result;
            } else if (site.isEmpty() && key.equals("push_agent")) {
                Result result = new Result();
                result.setParse(0);
                result.setFlag(flag);
                result.setUrl(Url.create().add(id));
                result.setUrl(Source.get().fetch(result));
                return result;
            } else {
                Url url = Url.create().add(id);
                String type = Uri.parse(id).getQueryParameter("type");
                if (type != null && type.equals("json")) url = Result.fromJson(OkHttp.newCall(id, site.getHeaders()).execute().body().string()).getUrl();
                Result result = new Result();
                result.setUrl(url);
                result.setFlag(flag);
                result.setHeader(site.getHeader());
                result.setPlayUrl(site.getPlayUrl());
                result.setParse(Sniffer.isVideoFormat(url.v()) && result.getPlayUrl().isEmpty() ? 0 : 1);
                SpiderDebug.log(result.toString());
                return result;
            }
        });
    }

    public void searchContent(Site site, String keyword, boolean quick) throws Throwable {
        if (site.getType() == 3) {
            Spider spider = VodConfig.get().getSpider(site);
            String searchContent = spider.searchContent(Trans.t2s(keyword), quick);
            SpiderDebug.log(site.getName() + "," + searchContent);
            post(site, Result.fromJson(searchContent));
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("wd", Trans.t2s(keyword));
            params.put("quick", String.valueOf(quick));
            String searchContent = call(site, params, true);
            SpiderDebug.log(site.getName() + "," + searchContent);
            post(site, fetchPic(site, Result.fromType(site.getType(), searchContent)));
        }
    }

    public void searchContent(Site site, String keyword, String page) {
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = VodConfig.get().getSpider(site);
                String searchContent = spider.searchContent(Trans.t2s(keyword), false, page);
                SpiderDebug.log(site.getName() + "," + searchContent);
                Result result = Result.fromJson(searchContent);
                for (Vod vod : result.getList()) vod.setSite(site);
                return result;
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("wd", Trans.t2s(keyword));
                params.put("pg", page);
                String searchContent = call(site, params, true);
                SpiderDebug.log(site.getName() + "," + searchContent);
                Result result = fetchPic(site, Result.fromType(site.getType(), searchContent));
                for (Vod vod : result.getList()) vod.setSite(site);
                return result;
            }
        });
    }

    private String call(Site site, ArrayMap<String, String> params, boolean limit) throws IOException {
        Call call = fetchExt(site, params, limit).length() <= 1000 ? OkHttp.newCall(site.getApi(), site.getHeaders(), params) : OkHttp.newCall(site.getApi(), site.getHeaders(), OkHttp.toBody(params));
        return call.execute().body().string();
    }

    private String fetchExt(Site site, ArrayMap<String, String> params, boolean limit) throws IOException {
        String extend = site.getExt();
        if (extend.startsWith("http")) extend = fetchExt(site);
        if (limit && extend.length() > 1000) extend = extend.substring(0, 1000);
        if (!extend.isEmpty()) params.put("extend", extend);
        return extend;
    }

    private String fetchExt(Site site) throws IOException {
        Response res = OkHttp.newCall(site.getExt(), site.getHeaders()).execute();
        if (res.code() != 200) return "";
        site.setExt(res.body().string());
        return site.getExt();
    }

    private Result fetchPic(Site site, Result result) throws Exception {
        if (result.getList().isEmpty() || result.getList().get(0).getVodPic().length() > 0) return result;
        ArrayList<String> ids = new ArrayList<>();
        for (Vod item : result.getList()) ids.add(item.getVodId());
        ArrayMap<String, String> params = new ArrayMap<>();
        params.put("ac", site.getType() == 0 ? "videolist" : "detail");
        params.put("ids", TextUtils.join(",", ids));
        String response = OkHttp.newCall(site.getApi(), site.getHeaders(), params).execute().body().string();
        result.setList(Result.fromType(site.getType(), response).getList());
        return result;
    }

    private void checkThunder(List<Flag> flags) throws Exception {
        for (Flag flag : flags) {
            ExecutorService executor = Executors.newFixedThreadPool(Constant.THREAD_POOL * 2);
            for (Future<List<Episode>> future : executor.invokeAll(flag.getMagnet(), 30, TimeUnit.SECONDS)) flag.getEpisodes().addAll(future.get());
            executor.shutdownNow();
        }
    }

    private void post(Site site, Result result) {
        if (result.getList().isEmpty()) return;
        for (Vod vod : result.getList()) vod.setSite(site);
        this.search.postValue(result);
    }

    private void execute(MutableLiveData<Result> result, Callable<Result> callable) {
        if (executor != null) executor.shutdownNow();
        executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            try {
                if (Thread.interrupted()) return;
                result.postValue(executor.submit(callable).get(Constant.TIMEOUT_VOD, TimeUnit.MILLISECONDS));
            } catch (Throwable e) {
                if (e instanceof InterruptedException || Thread.interrupted()) return;
                if (e.getCause() instanceof ExtractException) result.postValue(Result.error(e.getCause().getMessage()));
                else result.postValue(Result.empty());
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCleared() {
        if (executor != null) executor.shutdownNow();
    }
}
