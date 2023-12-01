package com.fongmi.android.tv.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.LiveParser;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.github.catvod.net.OkHttp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveViewModel extends ViewModel {

    private static final int LIVE = 0;
    private static final int EPG = 1;
    private static final int URL = 2;

    private final SimpleDateFormat formatDate;
    private final SimpleDateFormat formatSeek;
    private final SimpleDateFormat formatTime;

    public MutableLiveData<Channel> url;
    public MutableLiveData<Live> live;
    public MutableLiveData<Epg> epg;

    private ExecutorService executor1;
    private ExecutorService executor2;
    private ExecutorService executor3;

    public LiveViewModel() {
        this.formatTime = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault());
        this.formatSeek = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        this.formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.live = new MutableLiveData<>();
        this.epg = new MutableLiveData<>();
        this.url = new MutableLiveData<>();
    }

    public void getLive(Live item) {
        execute(LIVE, () -> {
            LiveParser.start(item);
            verify(item);
            return item;
        });
    }

    public void getEpg(Channel item) {
        String date = formatDate.format(new Date());
        if (item.getData().equal(date)) return;
        String url = item.getEpg().replace("{date}", date);
        execute(EPG, () -> {
            Epg epg = Epg.objectFrom(OkHttp.string(url), item.getName(), formatTime);
            item.setData(epg);
            return epg;
        });
    }

    public void getUrl(Channel item) {
        execute(URL, () -> {
            item.setMsg(null);
            Source.get().stop();
            item.setUrl(Source.get().fetch(item));
            //checkPLTV(item);
            return item;
        });
    }

    private void verify(Live item) {
        Iterator<Group> iterator = item.getGroups().iterator();
        while (iterator.hasNext()) if (iterator.next().isEmpty()) iterator.remove();
    }

    private void checkPLTV(Channel item) {
        if (!item.getUrl().contains("/PLTV/")) return;
        Calendar calendar = Calendar.getInstance();
        String endTime = formatSeek.format(calendar.getTime());
        String startTime = formatSeek.format(calendar.getTime());
        item.setUrl(item.getUrl().replace("/PLTV/", "/TVOD/") + "?playseek=" + startTime + "-" + endTime);
    }

    private void execute(int type, Callable<?> callable) {
        switch (type) {
            case LIVE:
                if (executor1 != null) executor1.shutdownNow();
                executor1 = Executors.newFixedThreadPool(2);
                executor1.execute(runnable(type, callable, executor1));
                break;
            case EPG:
                if (executor2 != null) executor2.shutdownNow();
                executor2 = Executors.newFixedThreadPool(2);
                executor2.execute(runnable(type, callable, executor2));
                break;
            case URL:
                if (executor3 != null) executor3.shutdownNow();
                executor3 = Executors.newFixedThreadPool(2);
                executor3.execute(runnable(type, callable, executor3));
                break;
        }
    }

    private Runnable runnable(int type, Callable<?> callable, ExecutorService executor) {
        return () -> {
            try {
                if (Thread.interrupted()) return;
                if (type == EPG) epg.postValue((Epg) executor.submit(callable).get(Constant.TIMEOUT_EPG, TimeUnit.MILLISECONDS));
                if (type == LIVE) live.postValue((Live) executor.submit(callable).get(Constant.TIMEOUT_LIVE, TimeUnit.MILLISECONDS));
                if (type == URL) url.postValue((Channel) executor.submit(callable).get(Constant.TIMEOUT_PARSE_LIVE, TimeUnit.MILLISECONDS));
            } catch (Throwable e) {
                if (e instanceof InterruptedException || Thread.interrupted()) return;
                if (e.getCause() instanceof ExtractException) url.postValue(Channel.error(e.getCause().getMessage()));
                else if (type == URL) url.postValue(new Channel());
                if (type == LIVE) live.postValue(new Live());
                if (type == EPG) epg.postValue(new Epg());
                e.printStackTrace();
            }
        };
    }

    @Override
    protected void onCleared() {
        if (executor1 != null) executor1.shutdownNow();
        if (executor2 != null) executor2.shutdownNow();
        if (executor3 != null) executor3.shutdownNow();
    }
}
