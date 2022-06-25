package com.fongmi.bear.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Site;
import com.github.catvod.crawler.Spider;

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
        int type = site.getType();
        if (type == 3) {
            mService.execute(() -> {
                Spider spider = ApiConfig.get().getCSP(site);
                Result result = Result.objectFrom(spider.homeContent(false));
                if (result.getList().isEmpty()) result = Result.objectFrom(spider.homeVideoContent());
                mResult.postValue(result);
            });
        }
    }
}
