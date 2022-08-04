package com.fongmi.android.tv.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<Result> result;
    public ExecutorService service;

    public SiteViewModel() {
        this.result = new MutableLiveData<>();
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
                if (home.getType() == 0) return Result.fromXml(body);
                else if (home.getType() == 1) return Result.fromJson(body);
                else return new Result();
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
                HttpUrl url = HttpUrl.parse(home.getApi()).newBuilder().addQueryParameter("ac", home.getType() == 0 ? "videolist" : "detail").addQueryParameter("t", tid).addQueryParameter("pg", page).build();
                String body = OKHttp.newCall(url).execute().body().string();
                SpiderDebug.log(body);
                if (home.getType() == 0) return Result.fromXml(body);
                else if (home.getType() == 1) return Result.fromJson(body);
                return new Result();
            }
        });
    }

    public void detailContent(String key, String id) {
        Site site = ApiConfig.get().getSite(key);
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String detailContent = spider.detailContent(List.of(id));
                SpiderDebug.log(detailContent);
                Result result = Result.fromJson(detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                return result;
            } else {
                HttpUrl url = HttpUrl.parse(site.getApi()).newBuilder().addQueryParameter("ac", site.getType() == 0 ? "videolist" : "detail").addQueryParameter("ids", id).build();
                String body = OKHttp.newCall(url).execute().body().string();
                SpiderDebug.log(body);
                Result result;
                if (site.getType() == 0) result = Result.fromXml(body);
                else if (site.getType() == 1) result = Result.fromJson(body);
                else result = new Result();
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                return result;
            }
        });
    }

    public void playerContent(String key, String flag, String id) {
        Site site = ApiConfig.get().getSite(key);
        execute(result, () -> {
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
