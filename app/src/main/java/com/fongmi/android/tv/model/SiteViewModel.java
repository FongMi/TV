package com.fongmi.android.tv.model;

import android.text.TextUtils;

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
    public ExecutorService service;

    public SiteViewModel() {
        this.result = new MutableLiveData<>();
        this.player = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getResult() {
        return result;
    }

    public void homeContent() {
        Site home = ApiConfig.get().getHome();
        execute(result, () -> {
            if (home.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(home);
                String homeContent = spider.homeContent(true);
                SpiderDebug.log(homeContent);
                Result result = Result.fromJson(homeContent);
                if (result.getList().size() > 0) return result;
                String homeVideoContent = spider.homeVideoContent();
                SpiderDebug.log(homeVideoContent);
                result.setList(Result.fromJson(homeVideoContent).getList());
                return result;
            } else {
                String body = OKHttp.newCall(home.getApi()).execute().body().string();
                SpiderDebug.log(body);
                Result result = home.getType() == 0 ? Result.fromXml(body) : Result.fromJson(body);
                if (result.getList().isEmpty() || result.getList().get(0).getVodPic().length() > 0) return result;
                ArrayList<String> ids = new ArrayList<>();
                for (Vod item : result.getList()) ids.add(item.getVodId());
                HashMap<String, String> params = new HashMap<>();
                params.put("ac", home.getType() == 0 ? "videolist" : "detail");
                params.put("ids", TextUtils.join(",", ids));
                body = OKHttp.newCall(home.getApi(), params).execute().body().string();
                List<Vod> items = home.getType() == 0 ? Result.fromXml(body).getList() : Result.fromJson(body).getList();
                result.setList(items);
                return result;
            }
        });
    }

    public void categoryContent(String tid, String page, boolean filter, HashMap<String, String> extend) {
        Site home = ApiConfig.get().getHome();
        execute(result, () -> {
            if (home.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(home);
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.log(categoryContent);
                return Result.fromJson(categoryContent);
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put("ac", home.getType() == 0 ? "videolist" : "detail");
                params.put("t", tid);
                params.put("pg", page);
                String body = OKHttp.newCall(home.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                return home.getType() == 0 ? Result.fromXml(body) : Result.fromJson(body);
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
                HashMap<String, String> params = new HashMap<>();
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
            } else {
                Result result = new Result();
                result.setUrl(id);
                result.setFlag(flag);
                result.setPlayUrl(site.getPlayerUrl());
                result.setParse(Utils.isVideoFormat(id) ? "0" : "1");
                return result;
            }
        });
    }

    public void searchContent(String key, String keyword) {
        try {
            Site site = ApiConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String searchContent = spider.searchContent(keyword, false);
                SpiderDebug.log(searchContent);
                postSearch(site, Result.fromJson(searchContent));
            } else {
                HashMap<String, String> params = new HashMap<>();
                if (site.getType() == 1) params.put("ac", "detail");
                params.put("wd", keyword);
                String body = OKHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                if (site.getType() == 0) postSearch(site, Result.fromXml(body));
                else postSearch(site, Result.fromJson(body));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postSearch(Site site, Result item) {
        for (Vod vod : item.getList()) vod.setSite(site);
        if (!item.getList().isEmpty()) result.postValue(item);
    }

    private void execute(MutableLiveData<Result> result, Callable<Result> callable) {
        if (service != null) service.shutdownNow();
        service = Executors.newFixedThreadPool(2);
        service.execute(() -> {
            try {
                if (!Thread.interrupted()) result.postValue(service.submit(callable).get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                if (!Thread.interrupted()) result.postValue(new Result());
            }
        });
    }

    @Override
    protected void onCleared() {
        if (service != null) service.shutdownNow();
    }
}
