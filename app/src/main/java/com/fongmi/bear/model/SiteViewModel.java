package com.fongmi.bear.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<Result> mResult;
    public ExecutorService mService;

    private enum Func {
        HOME, CATEGORY, DETAIL, PLAYER, SEARCH
    }

    public SiteViewModel() {
        this.mService = Executors.newFixedThreadPool(2);
        this.mResult = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getResult() {
        return mResult;
    }

    public void homeContent() {
        Site home = ApiConfig.get().getHome();
        postResult(Func.HOME, () -> {
            Spider spider = ApiConfig.get().getCSP(home);
            String homeContent = spider.homeContent(false);
            SpiderDebug.json(homeContent);
            Result result = Result.objectFrom(homeContent);
            if (result.getList().size() > 0) return result;
            String homeVideoContent = spider.homeVideoContent();
            SpiderDebug.json(homeVideoContent);
            result.setList(Result.objectFrom(homeVideoContent).getList());
            return result;
        });
    }

    public void categoryContent(String tid, String page, boolean filter, HashMap<String, String> extend) {
        Site home = ApiConfig.get().getHome();
        postResult(Func.CATEGORY, () -> {
            Spider spider = ApiConfig.get().getCSP(home);
            String categoryContent = spider.categoryContent(tid, page, filter, extend);
            SpiderDebug.json(categoryContent);
            return Result.objectFrom(categoryContent);
        });
    }

    public void detailContent(String id) {
        Site home = ApiConfig.get().getHome();
        postResult(Func.DETAIL, () -> {
            Spider spider = ApiConfig.get().getCSP(home);
            String detailContent = spider.detailContent(List.of(id));
            SpiderDebug.json(detailContent);
            return Result.objectFrom(detailContent);
        });
    }

    public void playerContent(String flag, String id) {
        Site home = ApiConfig.get().getHome();
        postResult(Func.PLAYER, () -> {
            Spider spider = ApiConfig.get().getCSP(home);
            String playerContent = spider.playerContent(flag, id, ApiConfig.get().getFlags());
            SpiderDebug.json(playerContent);
            return Result.objectFrom(playerContent);
        });
    }

    private void postResult(Func func, Callable<Result> callable) {
        mService.execute(() -> {
            try {
                Future<Result> future = mService.submit(callable);
                Result result = future.get(10, TimeUnit.SECONDS);
                checkResult(func, result);
                mResult.postValue(result);
            } catch (Exception e) {
                mResult.postValue(new Result());
            }
        });
    }

    private void checkResult(Func func, Result result) {
        if (func.equals(Func.DETAIL) && result.getList().size() > 0) {
            Vod vod = result.getList().get(0);
            vod.setVodFlags(getVodFlags(vod));
        }
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
}
