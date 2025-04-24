package com.fongmi.android.tv.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.EpgParser;
import com.fongmi.android.tv.api.LiveParser;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.github.catvod.net.OkHttp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveViewModel extends ViewModel {

    private static final int LIVE = 0;
    private static final int EPG = 1;
    private static final int URL = 2;
    private static final int XML = 3;

    private final SimpleDateFormat formatDate;
    private final SimpleDateFormat formatTime;

    public MutableLiveData<Channel> url;
    public MutableLiveData<Boolean> xml;
    public MutableLiveData<Live> live;
    public MutableLiveData<Epg> epg;

    private ExecutorService executor1;
    private ExecutorService executor2;
    private ExecutorService executor3;
    private ExecutorService executor4;

    public LiveViewModel() {
        this.formatTime = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault());
        this.formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.live = new MutableLiveData<>();
        this.epg = new MutableLiveData<>();
        this.url = new MutableLiveData<>();
        this.xml = new MutableLiveData<>();
    }

    public void getLive(Live item) {
        execute(LIVE, () -> {
            LiveParser.start(item.recent());
            setTimeZone(item);
            verify(item);
            return item;
        });
    }

    public void getXml(Live item) {
        execute(XML, () -> {
            boolean result = false;
            for (String url : item.getEpgXml()) if (EpgParser.start(item, url)) result = true;
            return result;
        });
    }

    public void getEpg(Channel item) {
        String date = formatDate.format(new Date());
        String url = item.getEpg().replace("{date}", date);
        execute(EPG, () -> {
            if (url.startsWith("http") && !item.getData().equal(date)) item.setData(Epg.objectFrom(OkHttp.string(url), item.getTvgId(), formatTime));
            return item.getData().selected();
        });
    }

    public void getUrl(Channel item) {
        execute(URL, () -> {
            item.setMsg(null);
            Source.get().stop();
            item.setUrl(Source.get().fetch(item));
            return item;
        });
    }

    public void getUrl(Channel item, EpgData data) {
        execute(URL, () -> {
            item.setUrl(item.getCatchup().format(item.getCurrent(), data));
            return item;
        });
    }

    private void setTimeZone(Live live) {
        try {
            TimeZone timeZone = live.getTimeZone().isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(live.getTimeZone());
            formatDate.setTimeZone(timeZone);
            formatTime.setTimeZone(timeZone);
        } catch (Exception ignored) {
        }
    }

    private void verify(Live item) {
        Iterator<Group> iterator = item.getGroups().iterator();
        while (iterator.hasNext()) if (iterator.next().isEmpty()) iterator.remove();
        if (item.getGroups().isEmpty() || item.getGroups().get(0).isKeep()) return;
        item.getGroups().add(0, Group.create(R.string.keep));
        LiveConfig.get().setKeep(item.getGroups());
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
            case XML:
                if (executor4 != null) executor4.shutdownNow();
                executor4 = Executors.newFixedThreadPool(2);
                executor4.execute(runnable(type, callable, executor4));
                break;
        }
    }

    private Runnable runnable(int type, Callable<?> callable, ExecutorService executor) {
        return () -> {
            try {
                if (Thread.interrupted()) return;
                if (type == EPG) epg.postValue((Epg) executor.submit(callable).get(Constant.TIMEOUT_EPG, TimeUnit.MILLISECONDS));
                if (type == LIVE) live.postValue((Live) executor.submit(callable).get(Constant.TIMEOUT_LIVE, TimeUnit.MILLISECONDS));
                if (type == XML) xml.postValue((Boolean) executor.submit(callable).get(Constant.TIMEOUT_XML, TimeUnit.MILLISECONDS));
                if (type == URL) url.postValue((Channel) executor.submit(callable).get(Constant.TIMEOUT_PARSE_LIVE, TimeUnit.MILLISECONDS));
            } catch (Throwable e) {
                if (e instanceof InterruptedException || Thread.interrupted()) return;
                if (e.getCause() instanceof ExtractException) url.postValue(Channel.error(e.getCause().getMessage()));
                else if (type == URL) url.postValue(new Channel());
                if (type == LIVE) live.postValue(new Live());
                if (type == EPG) epg.postValue(new Epg());
                if (type == XML) xml.postValue(false);
                e.printStackTrace();
            }
        };
    }

    @Override
    protected void onCleared() {
        if (executor1 != null) executor1.shutdownNow();
        if (executor2 != null) executor2.shutdownNow();
        if (executor3 != null) executor3.shutdownNow();
        if (executor4 != null) executor4.shutdownNow();
    }
}
