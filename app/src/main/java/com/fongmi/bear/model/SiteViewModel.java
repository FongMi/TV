package com.fongmi.bear.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Site;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SiteViewModel extends ViewModel {

    public static final ExecutorService mService = Executors.newFixedThreadPool(5);
    public MutableLiveData<Result> mResult;

    public SiteViewModel() {
        this.mResult = new MutableLiveData<>();
    }

    public void homeContent(String key) {
        Site site = ApiConfig.get().getSite(key);
        if (site.getType() == 3) {
            mService.execute(() -> {
                Spider spider = ApiConfig.get().getCSP(site);
                String homeContent = spider.homeContent(false);
                SpiderDebug.log(homeContent);
                Result result = Result.objectFrom(homeContent);
                if (result.getList().isEmpty()) {
                    String homeVideoContent = spider.homeVideoContent();
                    SpiderDebug.log(homeVideoContent);
                    result = Result.objectFrom(homeVideoContent);
                }
                mResult.postValue(result);
            });
        }
    }
}
