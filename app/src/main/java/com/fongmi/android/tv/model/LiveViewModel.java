package com.fongmi.android.tv.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.player.source.Force;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveViewModel extends ViewModel {

    public MutableLiveData<Channel> result;
    public ExecutorService executor;

    public LiveViewModel() {
        this.result = new MutableLiveData<>();
    }

    public MutableLiveData<Channel> getResult() {
        return result;
    }

    public void getUrl(Channel item) {
        execute(() -> {
            String url = item.getUrls().get(item.getLine());
            if (item.isForce()) item.setUrl(Force.get().fetch(url));
            else item.setUrl(url);
            return item;
        });
    }

    private void execute(Callable<Channel> callable) {
        if (executor != null) executor.shutdownNow();
        executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            try {
                if (!Thread.interrupted()) result.postValue(executor.submit(callable).get(5, TimeUnit.SECONDS));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCleared() {
        if (executor != null) executor.shutdownNow();
    }
}
