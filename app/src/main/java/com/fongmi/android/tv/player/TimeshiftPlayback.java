package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.SimpleBasePlayer.MediaItemData;
import androidx.media3.common.SimpleBasePlayer.PositionSupplier;
import androidx.media3.common.SimpleBasePlayer.State;
import androidx.media3.common.Timeline;
import androidx.media3.common.util.Clock;
import androidx.media3.common.util.HandlerWrapper;
import androidx.media3.common.util.Util;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Timeshift;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.github.catvod.net.OkHttp;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public final class TimeshiftPlayback {

    private final PlayerManager owner;
    private final Call.Factory requests;
    private final Clock clock;
    private final HandlerWrapper handler;
    private final Runnable update = this::updateWindow;
    private final Timeline.Window sourceWindow = new Timeline.Window();
    private Timeshift config;
    private MediaItem mediaItem;
    private Object uid;
    private Call call;
    private SettableFuture<Void> pending;
    private long start;
    private long end;
    private long epochOffset = C.TIME_UNSET;
    private boolean replaying;

    TimeshiftPlayback(PlayerManager owner) {
        this(owner, OkHttp.client(), Clock.DEFAULT);
    }

    TimeshiftPlayback(PlayerManager owner, Call.Factory requests, Clock clock) {
        this.owner = owner;
        this.requests = requests;
        this.clock = clock;
        this.handler = clock.createHandler(owner.getPlayer().getApplicationLooper(), null);
    }

    void start(PlaySpec spec) {
        clear();
        Timeshift config = spec.getTimeshift();
        if (config == null || !config.isValid(clock.currentTimeMillis())) return;
        this.config = config;
        this.mediaItem = MediaItemFactory.from(spec);
        this.uid = new Object();
        updateWindow();
    }

    void clear() {
        cancel();
        handler.removeCallbacks(update);
        boolean active = isActive();
        config = null;
        mediaItem = null;
        uid = null;
        epochOffset = C.TIME_UNSET;
        replaying = false;
        if (active) owner.onTimeshiftChanged();
    }

    private void cancel() {
        if (call != null) call.cancel();
        if (pending != null) pending.cancel(false);
        call = null;
        pending = null;
    }

    public boolean isActive() {
        return config != null;
    }

    public boolean isReplaying() {
        return isActive() && replaying;
    }

    private void updateWindow() {
        if (!isActive()) return;
        end = clock.currentTimeMillis();
        start = config.getStart(end);
        owner.onTimeshiftChanged();
        handler.postDelayed(update, 1000);
    }

    long getDuration() {
        return end - start;
    }

    long getPosition() {
        return position(owner.getPlayer().getCurrentPosition());
    }

    private long position(long position) {
        Player player = owner.getPlayer();
        Timeline timeline = player.getCurrentTimeline();
        if (replaying && !timeline.isEmpty()) {
            timeline.getWindow(player.getCurrentMediaItemIndex(), sourceWindow);
            if (sourceWindow.windowStartTimeMs != C.TIME_UNSET) return clamp(sourceWindow.windowStartTimeMs + position - start);
        }
        if (epochOffset == C.TIME_UNSET) {
            if (player.getPlaybackState() != Player.STATE_READY) return getDuration();
            epochOffset = clock.currentTimeMillis() - player.getCurrentPosition();
        }
        return clamp(epochOffset + position - start);
    }

    private long clamp(long position) {
        return Math.clamp(position, 0, getDuration());
    }

    public State getState(State state) {
        if (!isActive()) return state;
        List<MediaItemData> playlist = state.getPlaylist();
        if (playlist.size() != 1) return state;
        State.Builder result = state.buildUpon().setPlaylist(List.of(buildItem(playlist.get(0)))).setCurrentMediaItemIndex(0)
                .setAvailableCommands(state.availableCommands.buildUpon().addAll(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD).build())
                .setContentPositionMs(getPosition()).setContentBufferedPositionMs(PositionSupplier.getConstant(position(owner.getPlayer().getBufferedPosition())));
        if (state.hasPositionDiscontinuity) result.setPositionDiscontinuity(state.positionDiscontinuityReason, position(state.discontinuityPositionMs));
        return result.build();
    }

    private MediaItemData buildItem(MediaItemData item) {
        return item.buildUpon().setUid(uid).setMediaItem(mediaItem)
                .setLiveConfiguration(MediaItem.LiveConfiguration.UNSET).setIsDynamic(true).setIsSeekable(true)
                .setWindowStartTimeMs(start).setDefaultPositionUs(Util.msToUs(getDuration())).setDurationUs(Util.msToUs(getDuration()))
                .setPositionInFirstPeriodUs(Util.msToUs(start)).setPeriods(List.of()).build();
    }

    public ListenableFuture<?> seekTo(long position) {
        cancel();
        long time = position == C.TIME_UNSET || position >= getDuration() ? 0 : start + clamp(position);
        SettableFuture<Void> future = SettableFuture.create();
        pending = future;
        call = requests.newCall(new Request.Builder().url(config.getUrl(time)).build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException error) {
                handler.post(() -> fail(future));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    Result result = readSource(response);
                    handler.post(() -> apply(future, result, time));
                } catch (Exception error) {
                    handler.post(() -> fail(future));
                }
            }
        });
        return future;
    }

    private Result readSource(Response response) throws IOException {
        if (!response.isSuccessful() || response.body() == null) throw new IOException("Invalid timeshift response");
        Result result = Result.fromJson(response.body().string());
        if (result.hasMsg() || result.needParse() || result.getRealUrl().isEmpty()) throw new IOException("Missing direct timeshift source");
        return result;
    }

    private void apply(SettableFuture<Void> future, Result result, long time) {
        if (pending != future) return;
        call = null;
        pending = null;
        replaying = time != 0;
        epochOffset = replaying ? time : C.TIME_UNSET;
        owner.applyTimeshift(result.getRealUrl(), result.getFormat(), result.getHeader());
        future.set(null);
    }

    private void fail(SettableFuture<Void> future) {
        if (pending != future) return;
        call = null;
        pending = null;
        owner.onTimeshiftError();
        future.set(null);
    }

}
