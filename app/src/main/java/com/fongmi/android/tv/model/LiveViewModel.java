package com.fongmi.android.tv.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.C;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.LiveApi;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.playback.live.LiveDataSource;
import com.fongmi.android.tv.playback.live.LivePlayRequest;
import com.fongmi.android.tv.playback.live.LivePlaybackController;
import com.fongmi.android.tv.playback.live.LivePlaybackHost;
import com.fongmi.android.tv.playback.live.LivePlaybackState;

import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class LiveViewModel extends ViewModel implements LiveDataSource {

    private final MutableLiveData<PlaybackResult<LivePlayRequest>> playback;
    private final MutableLiveData<String> error;
    private final MutableLiveData<Boolean> xml;
    private final MutableLiveData<Live> live;
    private final MutableLiveData<Epg> epg;

    private final ViewModelTaskRunner<TaskType> tasks;
    private final LivePlaybackState playbackState;
    private volatile ZoneId zoneId;

    public LiveViewModel() {
        this.epg = new MutableLiveData<>();
        this.xml = new MutableLiveData<>();
        this.live = new MutableLiveData<>();
        this.error = new MutableLiveData<>();
        this.playback = new MutableLiveData<>();
        this.playbackState = new LivePlaybackState();
        this.tasks = new ViewModelTaskRunner<>(TaskType.class);
        this.zoneId = ZoneId.systemDefault();
    }

    public LiveData<PlaybackResult<LivePlayRequest>> playback() {
        return playback;
    }

    public LiveData<String> error() {
        return error;
    }

    public LiveData<Boolean> xml() {
        return xml;
    }

    public LiveData<Epg> epg() {
        return epg;
    }

    public LiveData<Live> live() {
        return live;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public LivePlaybackController createPlaybackController(LivePlaybackHost host) {
        return new LivePlaybackController(host, this, playbackState);
    }

    public void parse(Live item) {
        error.setValue(null);
        execute(TaskType.LIVE, () -> {
            LiveApi.parse(item);
            return item;
        }, result -> {
            setTimeZone(result);
            live.postValue(result);
        }, this::handleParseError);
    }

    public void parseXml(Live item) {
        execute(TaskType.XML, () -> LiveApi.parseXml(item), xml::postValue, error -> xml.postValue(false));
    }

    public void getEpg(Channel item) {
        execute(TaskType.EPG, () -> LiveApi.getEpg(item, zoneId), epg::postValue, error -> epg.postValue(new Epg()));
    }

    @Override
    public void getUrl(LivePlayRequest request) {
        execute(TaskType.URL, () -> getUrlResult(request), result -> postUrl(request, result), error -> handleUrlError(request, error));
    }

    private Result getUrlResult(LivePlayRequest request) throws Exception {
        return request.isCatchup() ? LiveApi.getUrl(request.getChannel(), request.getCatchupData()) : LiveApi.getUrl(request.getChannel());
    }

    private void postUrl(LivePlayRequest request, Result result) {
        if (request.getPosition() != C.TIME_UNSET) result.setPosition(request.getPosition());
        playback.postValue(new PlaybackResult<>(request, result));
    }

    private void handleParseError(Throwable t) {
        if (t instanceof ExtractException) error.postValue(t.getMessage());
        else live.postValue(new Live());
    }

    private void handleUrlError(LivePlayRequest request, Throwable t) {
        if (t instanceof ExtractException) postUrl(request, Result.error(t.getMessage()));
        else postUrl(request, new Result());
    }

    private void setTimeZone(Live live) {
        this.zoneId = live.getZoneId();
    }

    private <T> void execute(TaskType type, Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        tasks.execute(type, type.timeout, callable, onSuccess, onError);
    }

    @Override
    protected void onCleared() {
        tasks.cancelAll();
        playbackState.reset();
    }

    private enum TaskType {

        LIVE(Constant.TIMEOUT_LIVE),
        EPG(Constant.TIMEOUT_EPG),
        XML(Constant.TIMEOUT_XML),
        URL(Constant.TIMEOUT_PARSE_LIVE);

        final long timeout;

        TaskType(long timeout) {
            this.timeout = timeout;
        }
    }
}
