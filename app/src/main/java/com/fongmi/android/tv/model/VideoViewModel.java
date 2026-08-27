package com.fongmi.android.tv.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.playback.vod.VodDataSource;
import com.fongmi.android.tv.playback.vod.VodDetailResult;
import com.fongmi.android.tv.playback.vod.VodPlayRequest;
import com.fongmi.android.tv.playback.vod.VodPlaybackController;
import com.fongmi.android.tv.playback.vod.VodPlaybackHost;
import com.fongmi.android.tv.playback.vod.VodPlaybackState;

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
        requestTasks.execute(
                TaskType.DETAIL,
                Constant.TIMEOUT_VOD,
                () -> new VodDetailResult(key, id, SiteApi.detailContent(key, id)),
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

    private void loadPlayback(VodPlayRequest request, TaskType type, MutableLiveData<PlaybackResult<VodPlayRequest>> output) {
        requestTasks.execute(
                type,
                Constant.TIMEOUT_VOD,
                () -> new PlaybackResult<>(request, SiteApi.playerContent(request.getKey(), request.getFlag(), request.getId())),
                output::postValue,
                error -> output.postValue(new PlaybackResult<>(request, handleError(error))));
    }

    private Result handleError(Throwable error) {
        error.printStackTrace();
        return error instanceof ExtractException ? Result.error(error.getMessage()) : Result.empty();
    }

    @Override
    protected void onCleared() {
        requestTasks.cancelAll();
        playbackState.reset();
        super.onCleared();
    }

    private enum TaskType {DETAIL, PLAYBACK, PRELOAD}
}
