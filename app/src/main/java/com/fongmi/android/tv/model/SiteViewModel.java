package com.fongmi.android.tv.model;

import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.net.OKHttp;
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

    public MutableLiveData<Result> result;
    public MutableLiveData<Result> player;
    public ExecutorService executor;

    public SiteViewModel() {
        this.result = new MutableLiveData<>();
        this.player = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getResult() {
        return result;
    }

    public MutableLiveData<Result> getPlayer() {
        return player;
    }

    public void homeContent() {
        Site site = ApiConfig.get().getHome();
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String homeContent = spider.homeContent(true);
                SpiderDebug.log(homeContent);
                Result result = Result.fromJson(homeContent);
                if (result.getList().size() > 0) return result;
                String homeVideoContent = spider.homeVideoContent();
                SpiderDebug.log(homeVideoContent);
                result.setList(Result.fromJson(homeVideoContent).getList());
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("filter", "true");
                String body = OKHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                return Result.fromJson(body);
            } else {
                String body = OKHttp.newCall(site.getApi()).execute().body().string();
                SpiderDebug.log(body);
                Result result = site.getType() == 0 ? Result.fromXml(body) : Result.fromJson(body);
                if (result.getList().isEmpty() || result.getList().get(0).getVodPic().length() > 0) return result;
                ArrayList<String> ids = new ArrayList<>();
                for (Vod item : result.getList()) ids.add(item.getVodId());
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", TextUtils.join(",", ids));
                body = OKHttp.newCall(site.getApi(), params).execute().body().string();
                List<Vod> items = site.getType() == 0 ? Result.fromXml(body).getList() : Result.fromJson(body).getList();
                result.setList(items);
                return result;
            }
        });
    }

    public void categoryContent(String tid, String page, boolean filter, HashMap<String, String> extend) {
        Site site = ApiConfig.get().getHome();
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.log(categoryContent);
                return Result.fromJson(categoryContent);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                if (site.getType() == 4) params.put("ext", Utils.getBase64(new Gson().toJson(extend)));
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("t", tid);
                params.put("pg", page);
                String body = OKHttp.newCall(site.getApi(), params).execute().body().string();
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
                Result result = Result.fromJson(detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                return result;
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", id);
                String body = OKHttp.newCall(site.getApi(), params).execute().body().string();
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
                Result result = Result.objectFrom(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                return result;
            } else if (site.getType() == 4) {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("play", id);
                params.put("flag", flag);
                String body = OKHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                return Result.fromJson(body);
            } else {
                Result result = new Result();
                result.setUrl(id);
                result.setFlag(flag);
                result.setPlayUrl(site.getPlayUrl());
                result.setParse(Utils.isVideoFormat(id) ? 0 : 1);
                return result;
            }
        });
    }

    public void searchContent(Site site, String keyword) {
        try {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String searchContent = spider.searchContent(keyword, false);
                SpiderDebug.log(searchContent);
                post(site, Result.fromJson(searchContent));
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("wd", keyword);
                if (site.getType() != 0) params.put("ac", "detail");
                String body = OKHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(site.getName() + "," + body);
                if (site.getType() == 0) post(site, Result.fromXml(body));
                else post(site, Result.fromJson(body));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void post(Site site, Result result) {
        if (result.getList().isEmpty()) return;
        for (Vod vod : result.getList()) vod.setSite(site);
        this.result.postValue(result);
    }

    private void execute(MutableLiveData<Result> result, Callable<Result> callable) {
        if (executor != null) executor.shutdownNow();
        executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            try {
                if (!Thread.interrupted()) result.postValue(executor.submit(callable).get(15, TimeUnit.SECONDS));
            } catch (Throwable e) {
                e.printStackTrace();
                if (e instanceof InterruptedException) return;
                if (!Thread.interrupted()) result.postValue(Result.empty());
            }
        });
    }

    @Override
    protected void onCleared() {
        if (executor != null) executor.shutdownNow();
    }
}
