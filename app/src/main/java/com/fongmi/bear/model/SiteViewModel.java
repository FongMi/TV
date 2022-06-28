package com.fongmi.bear.model;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Site;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SiteViewModel extends ViewModel {

    public MutableLiveData<Result> mResult;
    public ExecutorService mService;

    public SiteViewModel() {
        this.mService = Executors.newFixedThreadPool(2);
        this.mResult = new MutableLiveData<>();
    }

    public MutableLiveData<Result> getResult() {
        return mResult;
    }

    public void homeContent() {
        Site home = ApiConfig.get().getHome();
        if (TextUtils.isEmpty(home.getKey())) {
            mResult.postValue(new Result());
        } else {
            postResult(() -> {
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
    }

    public void categoryContent(String tid, String page, boolean filter, HashMap<String, String> extend) {
        Site home = ApiConfig.get().getHome();
        postResult(() -> {
            Spider spider = ApiConfig.get().getCSP(home);
            String categoryContent = spider.categoryContent(tid, page, filter, extend);
            SpiderDebug.json(categoryContent);
            return Result.objectFrom(categoryContent);
        });
    }

    private void postResult(Callable<Result> callable) {
        mService.execute(() -> {
            try {
                Future<Result> future = mService.submit(callable);
                Result result = future.get(10, TimeUnit.SECONDS);
                mResult.postValue(result);
            } catch (Exception e) {
                mResult.postValue(new Result());
            }
        });
    }
}
