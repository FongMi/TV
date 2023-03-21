package com.fongmi.android.tv.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.LiveParser;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.player.source.BiliBili;
import com.fongmi.android.tv.player.source.Force;
import com.fongmi.android.tv.player.source.TVBus;
import com.fongmi.android.tv.player.source.Youtube;
import com.fongmi.android.tv.player.source.ZLive;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveViewModel extends ViewModel {

    private static final int LIVE = 1;
    private static final int CHANNEL = 2;

    public MutableLiveData<Channel> channel;
    public MutableLiveData<Live> live;
    public ExecutorService executor;

    public LiveViewModel() {
        this.channel = new MutableLiveData<>();
        this.live = new MutableLiveData<>();
    }

    public void getLive(Live item) {
        execute(LIVE, () -> {
            LiveParser.start(item);
            return item;
        });
    }

    public void getUrl(Channel item) {
        execute(CHANNEL, () -> {
            TVBus.get().stop();
            String url = item.getCurrent().split("\\$")[0];
            if (item.isForce()) item.setUrl(Force.get().fetch(url));
            else if (item.isZLive()) item.setUrl(ZLive.get().fetch(url));
            else if (item.isTVBus()) item.setUrl(TVBus.get().fetch(url));
            else if (item.isYoutube()) item.setUrl(Youtube.get().fetch(url));
            else if (item.isBiliBili()) item.setUrl(BiliBili.get().fetch(url));
            else item.setUrl(url);
            return item;
        });
    }

    private void execute(int type, Callable<?> callable) {
        if (executor != null) executor.shutdownNow();
        executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> {
            try {
                if (!Thread.interrupted() && type == LIVE) live.postValue((Live) executor.submit(callable).get(Constant.TIMEOUT_HTTP, TimeUnit.MILLISECONDS));
                if (!Thread.interrupted() && type == CHANNEL) channel.postValue((Channel) executor.submit(callable).get(Constant.TIMEOUT_LIVE, TimeUnit.MILLISECONDS));
            } catch (Throwable e) {
                e.printStackTrace();
                if (e instanceof InterruptedException) return;
                if (!Thread.interrupted() && type == LIVE) live.postValue(new Live());
                if (!Thread.interrupted() && type == CHANNEL) channel.postValue(new Channel());
            }
        });
    }

    @Override
    protected void onCleared() {
        if (executor != null) executor.shutdownNow();
    }
}
