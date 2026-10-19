package com.fongmi.android.tv.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.playback.vod.VodDataSource;
import com.fongmi.android.tv.playback.vod.VodDetailResult;
import com.fongmi.android.tv.playback.vod.VodPlayRequest;
import com.fongmi.android.tv.playback.vod.VodPlaybackController;
import com.fongmi.android.tv.playback.vod.VodPlaybackHost;
import com.fongmi.android.tv.playback.vod.VodPlaybackState;
import com.fongmi.android.tv.utils.Notify;

public class VideoViewModel extends SiteViewModel implements VodDataSource {

    private final MutableLiveData<VodDetailResult> detail;
    private final MutableLiveData<PlaybackResult<VodPlayRequest>> preload;
    private final MutableLiveData<PlaybackResult<VodPlayRequest>> playback;
    private final ViewModelTaskRunner<TaskType> requestTasks;
    private final VodPlaybackState playbackState;

    public VideoViewModel() {
        detail = new MutableLiveData<>();
        preload = new MutableLiveData<>();
        playback = new MutableLiveData<>();
        requestTasks = new ViewModelTaskRunner<>(TaskType.class);
        playbackState = new VodPlaybackState();
    }

    public LiveData<VodDetailResult> getDetail() {
        return detail;
    }

    public LiveData<PlaybackResult<VodPlayRequest>> getPreload() {
        return preload;
    }

    public LiveData<PlaybackResult<VodPlayRequest>> getPlayback() {
        return playback;
    }

    public VodPlaybackController createPlaybackController(VodPlaybackHost host) {
        return new VodPlaybackController(host, this, playbackState);
    }

    @Override
    public void detailContent(String key, String id) {
        detailContent(key, id, false);
    }

    @Override
    public void detailContent(String key, String id, boolean builtin) {
        requestTasks.execute(
                TaskType.DETAIL,
                Constant.TIMEOUT_VOD,
                () -> new VodDetailResult(key, id, SiteApi.detailContent(key, id, builtin)),
                detail::postValue,
                error -> detail.postValue(new VodDetailResult(key, id, handleError(error))));
    }

    @Override
    public void playerContent(VodPlayRequest request) {
        loadPlayback(request, TaskType.PLAYBACK, playback);
    }

    @Override
    public void preloadContent(VodPlayRequest request) {
        loadPlayback(request, TaskType.PRELOAD, preload);
    }

    @Override
    public void cancelPreloadContent() {
        requestTasks.cancel(TaskType.PRELOAD);
    }

    @Override
    public void cancelContent() {
        for (TaskType type : TaskType.values()) requestTasks.cancel(type);
        stopSearch();
    }

    private void loadPlayback(VodPlayRequest request, TaskType type, MutableLiveData<PlaybackResult<VodPlayRequest>> output) {
        requestTasks.execute(
                type,
                Constant.TIMEOUT_VOD,
                () -> getPlaybackResult(request, type),
                output::postValue,
                error -> output.postValue(new PlaybackResult<>(request, handleError(error))));
    }

    private PlaybackResult<VodPlayRequest> getPlaybackResult(VodPlayRequest request, TaskType type) throws Exception {
        Result result = type == TaskType.PRELOAD
                ? SiteApi.preloadContent(request.getKey(), request.getFlag(), request.getId(), request.isBuiltin())
                : SiteApi.playerContent(request.getKey(), request.getFlag(), request.getId(), request.isBuiltin(), request.getQualityPosition());
        return new PlaybackResult<>(request, result);
    }

    private Result handleError(Throwable error) {
        error.printStackTrace();
        String message = Notify.getError(error);
        return message != null ? Result.error(message) : Result.empty();
    }

    @Override
    protected void onCleared() {
        requestTasks.cancelAll();
        playbackState.reset();
        super.onCleared();
    }

    private enum TaskType {DETAIL, PLAYBACK, PRELOAD}
}
