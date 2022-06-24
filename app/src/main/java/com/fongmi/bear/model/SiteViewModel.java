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

    public static final ExecutorService service = Executors.newFixedThreadPool(5);

    public MutableLiveData<Result> result;

    public SiteViewModel() {
        this.result = new MutableLiveData<>();
    }

    public void homeContent(String key) {
        Site site = ApiConfig.get().getSite(key);
        int type = site.getType();
        if (type == 3) {
            service.execute(() -> {
                Spider spider = ApiConfig.get().getCSP(site);
                result.postValue(Result.objectFrom(spider.homeContent(false)));
            });
        }
    }
}
