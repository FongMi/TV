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
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.playback.live.LiveDataSource;
import com.fongmi.android.tv.playback.live.LivePlayRequest;
import com.fongmi.android.tv.playback.live.LivePlaybackController;
import com.fongmi.android.tv.playback.live.LivePlaybackHost;
import com.fongmi.android.tv.playback.live.LivePlaybackState;
import com.fongmi.android.tv.utils.Notify;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class LiveViewModel extends ViewModel implements LiveDataSource {

    private final MutableLiveData<PlaybackResult<LivePlayRequest>> playback;
    private final MutableLiveData<String> error;
    private final MutableLiveData<Boolean> xml;
    private final MutableLiveData<Live> live;
    private final MutableLiveData<Epg> epg;
    private final MutableLiveData<GuideResult> guide;
    private final MutableLiveData<GuideResult> refreshed;

    public record GuideResult(Live source, Channel channel, ZoneId zone, LocalDate date,
                              Epg epg, long requestId, boolean success) {
        public boolean matches(Live source, Channel channel, ZoneId zone) {
            return this.source == source && this.channel == channel && this.zone.equals(zone);
        }
    }

    private final ViewModelTaskRunner<TaskType> tasks;
    private final LivePlaybackState playbackState;
    private volatile ZoneId zoneId;

    public LiveViewModel() {
        this.epg = new MutableLiveData<>();
        this.guide = new MutableLiveData<>();
        this.refreshed = new MutableLiveData<>();
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

    public LiveData<GuideResult> guide() {
        return guide;
    }

    public LiveData<GuideResult> refreshed() {
        return refreshed;
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
        tasks.cancel(TaskType.EPG);
        tasks.cancel(TaskType.EPG_DAY);
        tasks.cancel(TaskType.EPG_REFRESH);
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

    public void getEpg(Live source, Channel item) {
        ZoneId zone = zoneId;
        LocalDate date = LocalDate.now(zone);
        execute(TaskType.EPG, () -> LiveApi.getEpg(item, zone, date),
                value -> guide.postValue(new GuideResult(source, item, zone, date, value, 0, true)),
                error -> guide.postValue(new GuideResult(source, item, zone, date, item.getData(zone, date), 0, false)));
    }

    public void getEpg(Live source, Channel item, LocalDate day) {
        ZoneId zone = zoneId;
        execute(TaskType.EPG_DAY, () -> LiveApi.getEpg(item, zone, day),
                value -> guide.postValue(new GuideResult(source, item, zone, day, value, 0, true)),
                error -> guide.postValue(new GuideResult(source, item, zone, day, item.getData(zone, day), 0, false)));
    }

    public void refreshEpg(Live source, Channel item, LocalDate day, long token) {
        ZoneId zone = zoneId;
        execute(TaskType.EPG_REFRESH, () -> LiveApi.refreshEpg(source, item, zone, day),
                success -> refreshed.postValue(new GuideResult(source, item, zone, day, item.getData(zone, day), token, success)),
                error -> refreshed.postValue(new GuideResult(source, item, zone, day, item.getData(zone, day), token, false)));
    }

    @Override
    public void getUrl(LivePlayRequest request) {
        execute(TaskType.URL, () -> getUrlResult(request), result -> postUrl(request, result), error -> handleUrlError(request, error));
    }

    @Override
    public void cancelUrl() {
        tasks.cancel(TaskType.URL);
    }

    private Result getUrlResult(LivePlayRequest request) throws Exception {
        return request.isCatchup() ? LiveApi.getUrl(request.getChannel(), request.getCatchupData()) : LiveApi.getUrl(request.getChannel());
    }

    private void postUrl(LivePlayRequest request, Result result) {
        if (request.getPosition() != C.TIME_UNSET) result.setPosition(request.getPosition());
        playback.postValue(new PlaybackResult<>(request, result));
    }

    private void handleParseError(Throwable t) {
        String message = Notify.getError(t);
        if (message != null) error.postValue(message);
        else live.postValue(new Live());
    }

    private void handleUrlError(LivePlayRequest request, Throwable t) {
        String message = Notify.getError(t);
        if (message != null) postUrl(request, Result.error(message));
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
        EPG_DAY(Constant.TIMEOUT_EPG),
        EPG_REFRESH(45000),
        XML(Constant.TIMEOUT_XML),
        URL(Constant.TIMEOUT_PARSE_LIVE);

        final long timeout;

        TaskType(long timeout) {
            this.timeout = timeout;
        }
    }
}
