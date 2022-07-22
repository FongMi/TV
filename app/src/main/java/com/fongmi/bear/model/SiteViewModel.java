package com.fongmi.bear.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.net.OKHttp;
import com.fongmi.bear.utils.Utils;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Response;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<JsonObject> player;
    public MutableLiveData<Result> result;
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
        postResult(() -> {
            if (home.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(home);
                String homeContent = spider.homeContent(false);
                SpiderDebug.json(homeContent);
                Result result = Result.objectFrom(homeContent);
                if (result.getList().size() > 0) return result;
                String homeVideoContent = spider.homeVideoContent();
                SpiderDebug.json(homeVideoContent);
                result.setList(Result.objectFrom(homeVideoContent).getList());
                return result;
            } else {
                Response response = OKHttp.newCall(home.getApi()).execute();
                if (home.getType() == 1) {
                    String homeContent = response.body().string();
                    SpiderDebug.json(homeContent);
                    return Result.objectFrom(homeContent);
                }
                return new Result();
            }
        });
    }

    public void categoryContent(String tid, String page, boolean filter, HashMap<String, String> extend) {
        Site home = ApiConfig.get().getHome();
        postResult(() -> {
            if (home.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(home);
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.json(categoryContent);
                return Result.objectFrom(categoryContent);
            } else {
                HttpUrl url = HttpUrl.parse(home.getApi()).newBuilder().addQueryParameter("ac", home.getType() == 0 ? "videolist" : "detail").addQueryParameter("t", tid).addQueryParameter("pg", page).build();
                Response response = OKHttp.newCall(url).execute();
                if (home.getType() == 1) {
                    String categoryContent = response.body().string();
                    SpiderDebug.json(categoryContent);
                    return Result.objectFrom(categoryContent);
                }
                return new Result();
            }
        });
    }

    public void detailContent(String id) {
        Site home = ApiConfig.get().getHome();
        postResult(() -> {
            if (home.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(home);
                String detailContent = spider.detailContent(List.of(id));
                SpiderDebug.json(detailContent);
                Result result = Result.objectFrom(detailContent);
                if (result.getList().isEmpty()) return result;
                Vod vod = result.getList().get(0);
                vod.setVodFlags(getVodFlags(vod));
                return result;
            } else {
                HttpUrl url = HttpUrl.parse(home.getApi()).newBuilder().addQueryParameter("ac", home.getType() == 0 ? "videolist" : "detail").addQueryParameter("ids", id).build();
                Response response = OKHttp.newCall(url).execute();
                if (home.getType() == 1) {
                    String detailContent = response.body().string();
                    SpiderDebug.json(detailContent);
                    Result result = Result.objectFrom(detailContent);
                    if (result.getList().isEmpty()) return result;
                    Vod vod = result.getList().get(0);
                    vod.setVodFlags(getVodFlags(vod));
                    return result;
                }
            }
            return new Result();
        });
    }

    public void playerContent(String flag, String id) {
        Site home = ApiConfig.get().getHome();
        postPlayer(() -> {
            if (home.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(home);
                String playerContent = spider.playerContent(flag, id, ApiConfig.get().getFlags());
                SpiderDebug.json(playerContent);
                JsonObject object = JsonParser.parseString(playerContent).getAsJsonObject();
                if (!object.has("flag")) object.addProperty("flag", flag);
                return object;
            } else {
                JsonObject object = new JsonObject();
                object.addProperty("url", id);
                object.addProperty("flag", flag);
                object.addProperty("playUrl", home.getPlayerUrl());
                object.addProperty("parse", Utils.isVideoFormat(id) ? "0" : "1");
                return object;
            }
        });
    }

    private void initService(boolean close) {
        if (service != null && close) service.shutdownNow();
        service = Executors.newFixedThreadPool(2);
    }

    private void postResult(Callable<Result> callable) {
        initService(false);
        service.execute(() -> {
            try {
                if (!Thread.interrupted()) result.postValue(service.submit(callable).get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                if (!Thread.interrupted()) result.postValue(new Result());
            }
        });
    }

    private void postPlayer(Callable<JsonObject> callable) {
        initService(true);
        service.execute(() -> {
            try {
                if (!Thread.interrupted()) player.postValue(service.submit(callable).get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                if (!Thread.interrupted()) player.postValue(null);
            }
        });
    }

    private List<Vod.Flag> getVodFlags(Vod vod) {
        List<Vod.Flag> items = new ArrayList<>();
        String[] playFlags = vod.getVodPlayFrom().split("\\$\\$\\$");
        String[] playUrls = vod.getVodPlayUrl().split("\\$\\$\\$");
        for (int i = 0; i < playFlags.length; i++) {
            Vod.Flag item = new Vod.Flag(playFlags[i]);
            String[] urls = playUrls[i].contains("#") ? playUrls[i].split("#") : new String[]{playUrls[i]};
            for (String url : urls) {
                if (!url.contains("$")) continue;
                String[] split = url.split("\\$");
                if (split.length >= 2) item.getEpisodes().add(new Vod.Flag.Episode(split[0], split[1]));
            }
            items.add(item);
        }
        return items;
    }

    @Override
    protected void onCleared() {
        if (service != null) service.shutdownNow();
    }
}
