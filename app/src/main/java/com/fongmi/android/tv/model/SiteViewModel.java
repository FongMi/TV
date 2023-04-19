package com.fongmi.android.tv.model;

import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.Trans;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<Vod.Flag.Episode> episode;
    public MutableLiveData<Result> result;
    public MutableLiveData<Result> player;
    public MutableLiveData<Result> search;
    public ExecutorService executor;

    public SiteViewModel() {
        this.episode = new MutableLiveData<>();
        this.result = new MutableLiveData<>();
        this.player = new MutableLiveData<>();
        this.search = new MutableLiveData<>();
    }

    public void setEpisode(Vod.Flag.Episode value) {
        episode.setValue(value);
    }

    public void homeContent() {
        Site site = ApiConfig.get().getHome();
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String homeContent = spider.homeContent(true);
                SpiderDebug.log(homeContent);
                ApiConfig.get().setJar(site.getJar());
                Result result = Result.fromJson(homeContent);
                if (result.getList().size() > 0) return result;
                String homeVideoContent = spider.homeVideoContent();
                SpiderDebug.log(homeVideoContent);
                result.setList(Result.fromJson(homeVideoContent).getList());
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("filter", "true");
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                return Result.fromJson(body);
            } else {
                String body = OkHttp.newCall(site.getApi()).execute().body().string();
                SpiderDebug.log(body);
                Result result = site.getType() == 0 ? Result.fromXml(body) : Result.fromJson(body);
                if (result.getList().isEmpty() || result.getList().get(0).getVodPic().length() > 0) return result;
                ArrayList<String> ids = new ArrayList<>();
                for (Vod item : result.getList()) ids.add(item.getVodId());
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", TextUtils.join(",", ids));
                body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                List<Vod> items = site.getType() == 0 ? Result.fromXml(body).getList() : Result.fromJson(body).getList();
                result.setList(items);
                return result;
            }
        });
    }

    public void categoryContent(String key, String tid, String page, boolean filter, HashMap<String, String> extend) {
        Site site = ApiConfig.get().getSite(key);
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.log(categoryContent);
                ApiConfig.get().setJar(site.getJar());
                return Result.fromJson(categoryContent);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                if (site.getType() == 1 && !extend.isEmpty()) params.put("f", new Gson().toJson(extend));
                else if (site.getType() == 4) params.put("ext", Utils.getBase64(new Gson().toJson(extend)));
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("t", tid);
                params.put("pg", page);
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                return site.getType() == 0 ? Result.fromXml(body) : Result.fromJson(body);
            }
        });
    }

    public void detailContent(String key, String id) {
        Site site = ApiConfig.get().getSite(key);
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String detailContent = spider.detailContent(Arrays.asList(id));
                SpiderDebug.log(detailContent);
                ApiConfig.get().setJar(site.getJar());
                Result result = Result.fromJson(detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                return result;
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", id);
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                Result result = site.getType() == 0 ? Result.fromXml(body) : Result.fromJson(body);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                return result;
            }
        });
    }

    public void playerContent(String key, String flag, String id) {
        Site site = ApiConfig.get().getSite(key);
        execute(player, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String playerContent = spider.playerContent(flag, id, ApiConfig.get().getFlags());
                SpiderDebug.log(playerContent);
                ApiConfig.get().setJar(site.getJar());
                Result result = Result.objectFrom(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                result.setKey(key);
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("play", id);
                params.put("flag", flag);
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                Result result = Result.fromJson(body);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                return result;
            } else {
                String url = id;
                String type = Uri.parse(url).getQueryParameter("type");
                if (type != null && type.equals("json")) url = Result.fromJson(OkHttp.newCall(id).execute().body().string()).getUrl();
                Result result = new Result();
                result.setUrl(url);
                result.setFlag(flag);
                result.setPlayUrl(site.getPlayUrl());
                result.setParse(Utils.isVideoFormat(url) && result.getPlayUrl().isEmpty() ? 0 : 1);
                return result;
            }
        });
    }

    public void searchContent(Site site, String keyword) throws Throwable {
        if (site.getType() == 3) {
            Spider spider = ApiConfig.get().getCSP(site);
            String searchContent = spider.searchContent(Trans.t2s(keyword), false);
            SpiderDebug.log(searchContent);
            post(site, Result.fromJson(searchContent));
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("wd", Trans.t2s(keyword));
            if (site.getType() != 0) params.put("ac", "detail");
            String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
            SpiderDebug.log(site.getName() + "," + body);
            if (site.getType() == 0) post(site, Result.fromXml(body));
            else post(site, Result.fromJson(body));
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
                result.postValue(Result.empty());
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCleared() {
        if (executor != null) executor.shutdownNow();
    }
}
