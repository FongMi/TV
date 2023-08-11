package com.fongmi.android.tv.model;

import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.player.extractor.Magnet;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Trans;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
        execute(result, () -> {
            Site site = ApiConfig.get().getHome();
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String homeContent = spider.homeContent(true);
                SpiderDebug.log(homeContent);
                ApiConfig.get().setRecent(site);
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
                return fetchPic(site, Result.fromType(site.getType(), body));
            }
        });
    }

    public void categoryContent(String key, String tid, String page, boolean filter, HashMap<String, String> extend) {
        execute(result, () -> {
            Site site = ApiConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String categoryContent = spider.categoryContent(tid, page, filter, extend);
                SpiderDebug.log(categoryContent);
                ApiConfig.get().setRecent(site);
                return Result.fromJson(categoryContent);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                if (site.getType() == 1 && !extend.isEmpty()) params.put("f", App.gson().toJson(extend));
                else if (site.getType() == 4) params.put("ext", Util.base64(App.gson().toJson(extend)));
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("t", tid);
                params.put("pg", page);
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                return Result.fromType(site.getType(), body);
            }
        });
    }

    public void detailContent(String key, String id) {
        execute(result, () -> {
            Site site = ApiConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String detailContent = spider.detailContent(Arrays.asList(id));
                SpiderDebug.log(detailContent);
                ApiConfig.get().setRecent(site);
                Result result = Result.fromJson(detailContent);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                if (!result.getList().isEmpty()) checkThunder(result.getList().get(0).getVodFlags());
                return result;
            } else if (key.equals("push_agent")) {
                Vod vod = new Vod();
                vod.setVodId(id);
                vod.setVodName(id);
                vod.setVodPic("https://pic.rmb.bdstatic.com/bjh/1d0b02d0f57f0a42201f92caba5107ed.jpeg");
                vod.setVodFlags(Vod.Flag.create(ResUtil.getString(R.string.push), ResUtil.getString(R.string.play), id));
                checkThunder(vod.getVodFlags());
                return Result.vod(vod);
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("ac", site.getType() == 0 ? "videolist" : "detail");
                params.put("ids", id);
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(body);
                Result result = Result.fromType(site.getType(), body);
                if (!result.getList().isEmpty()) result.getList().get(0).setVodFlags();
                if (!result.getList().isEmpty()) checkThunder(result.getList().get(0).getVodFlags());
                return result;
            }
        });
    }

    public void playerContent(String key, String flag, String id) {
        execute(player, () -> {
            Source.get().stop();
            Site site = ApiConfig.get().getSite(key);
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String playerContent = spider.playerContent(flag, id, ApiConfig.get().getFlags());
                SpiderDebug.log(playerContent);
                ApiConfig.get().setRecent(site);
                Result result = Result.objectFrom(playerContent);
                if (result.getFlag().isEmpty()) result.setFlag(flag);
                result.setUrl(Source.get().fetch(result.getUrl()));
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
                result.setUrl(Source.get().fetch(result.getUrl()));
                return result;
            } else if (key.equals("push_agent")) {
                Result result = new Result();
                result.setParse(0);
                result.setFlag(flag);
                result.setUrl(Source.get().fetch(id));
                return result;
            } else {
                String url = id;
                String type = Uri.parse(url).getQueryParameter("type");
                if (type != null && type.equals("json")) url = Result.fromJson(OkHttp.newCall(id).execute().body().string()).getUrl();
                Result result = new Result();
                result.setUrl(url);
                result.setFlag(flag);
                result.setPlayUrl(site.getPlayUrl());
                result.setParse(Sniffer.isVideoFormat(url) && result.getPlayUrl().isEmpty() ? 0 : 1);
                return result;
            }
        });
    }

    public void searchContent(Site site, String keyword, boolean quick) throws Throwable {
        if (site.getType() == 3) {
            Spider spider = ApiConfig.get().getCSP(site);
            String searchContent = spider.searchContent(Trans.t2s(keyword), quick);
            SpiderDebug.log(site.getName() + "," + searchContent);
            post(site, Result.fromJson(searchContent));
        } else {
            ArrayMap<String, String> params = new ArrayMap<>();
            params.put("wd", Trans.t2s(keyword));
            String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
            SpiderDebug.log(site.getName() + "," + body);
            post(site, fetchPic(site, Result.fromType(site.getType(), body)));
        }
    }

    public void searchContent(Site site, String keyword, String page) {
        execute(result, () -> {
            if (site.getType() == 3) {
                Spider spider = ApiConfig.get().getCSP(site);
                String searchContent = spider.searchContent(Trans.t2s(keyword), false, page);
                SpiderDebug.log(site.getName() + "," + searchContent);
                Result result = Result.fromJson(searchContent);
                for (Vod vod : result.getList()) vod.setSite(site);
                return result;
            } else {
                ArrayMap<String, String> params = new ArrayMap<>();
                params.put("wd", Trans.t2s(keyword));
                params.put("pg", page);
                String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
                SpiderDebug.log(site.getName() + "," + body);
                Result result = fetchPic(site, Result.fromType(site.getType(), body));
                for (Vod vod : result.getList()) vod.setSite(site);
                return result;
            }
        });
    }

    private Result fetchPic(Site site, Result result) throws Exception {
        if (result.getList().isEmpty() || result.getList().get(0).getVodPic().length() > 0) return result;
        ArrayList<String> ids = new ArrayList<>();
        for (Vod item : result.getList()) ids.add(item.getVodId());
        ArrayMap<String, String> params = new ArrayMap<>();
        params.put("ac", site.getType() == 0 ? "videolist" : "detail");
        params.put("ids", TextUtils.join(",", ids));
        String body = OkHttp.newCall(site.getApi(), params).execute().body().string();
        result.setList(Result.fromType(site.getType(), body).getList());
        return result;
    }

    private void checkThunder(List<Vod.Flag> flags) throws Exception {
        for (Vod.Flag flag : flags) {
            List<Magnet> magnets = new ArrayList<>();
            List<Vod.Flag.Episode> items = new ArrayList<>();
            for (Vod.Flag.Episode episode : flag.getEpisodes()) if (Sniffer.isThunder(episode.getUrl())) magnets.add(Magnet.get(episode.getUrl()));
            ExecutorService executor = Executors.newFixedThreadPool(Constant.THREAD_POOL * 2);
            for (Future<List<Vod.Flag.Episode>> future : executor.invokeAll(magnets, 30, TimeUnit.SECONDS)) Magnet.addAll(items, future);
            if (items.size() > 0) flag.createEpisode(items);
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
