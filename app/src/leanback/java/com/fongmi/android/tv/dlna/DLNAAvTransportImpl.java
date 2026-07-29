package com.fongmi.android.tv.dlna;

import android.content.Context;
import android.content.Intent;

import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.ui.activity.CastActivity;
import com.google.gson.reflect.TypeToken;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AbstractAVTransportService;
import org.jupnp.support.avtransport.lastchange.AVTransportVariable;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.DIDLObject;
import org.jupnp.support.model.DeviceCapabilities;
import org.jupnp.support.model.MediaInfo;
import org.jupnp.support.model.PlayMode;
import org.jupnp.support.model.PositionInfo;
import org.jupnp.support.model.RecordQualityMode;
import org.jupnp.support.model.SeekMode;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportSettings;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.model.TransportStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DLNAAvTransportImpl extends AbstractAVTransportService {

    private final Context context;

    private volatile PlayerManager player;
    private volatile PlayMode currentPlayMode = PlayMode.NORMAL;
    private volatile TransportState currentState = TransportState.NO_MEDIA_PRESENT;

    private volatile String nextURI = "";
    private volatile String currentURI = "";
    private volatile String nextMetaData = "";
    private volatile String currentMetaData = "";

    private volatile boolean dlnaActive;
    private volatile long pendingSeekMs = -1;
    private volatile long cachedPosition = -1;
    private volatile long cachedDuration = -1;

    public DLNAAvTransportImpl(Context context) {
        this.context = context;
    }

    public void setPlayerManager(PlayerManager player) {
        this.player = player;
        if (player == null) reset();
    }

    public void setDlnaActive(boolean active) {
        this.dlnaActive = active;
    }

    public void reset() {
        nextURI = "";
        currentURI = "";
        nextMetaData = "";
        pendingSeekMs = -1;
        cachedPosition = -1;
        cachedDuration = -1;
        currentMetaData = "";
        currentPlayMode = PlayMode.NORMAL;
        fireStateChange(RenderState.IDLE);
    }

    public void updatePositionCache(long position, long duration) {
        cachedPosition = position;
        cachedDuration = duration;
    }

    public long consumePendingSeekMs() {
        long ms = pendingSeekMs;
        pendingSeekMs = -1;
        return ms;
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        return new UnsignedIntegerFourBytes[]{getDefaultInstanceID()};
    }

    @Override
    public synchronized void setAVTransportURI(UnsignedIntegerFourBytes instanceId, String currentURI, String currentURIMetaData) {
        this.nextURI = "";
        this.nextMetaData = "";
        this.dlnaActive = false;
        this.pendingSeekMs = -1;
        this.currentURI = currentURI != null ? currentURI : "";
        this.currentMetaData = currentURIMetaData != null ? currentURIMetaData : "";
        startCastActivity(new CastAction(this.currentURI, this.currentMetaData, parseHeaders(this.currentMetaData)));
    }

    private Map<String, String> parseHeaders(String metaData) {
        try {
            DIDLContent content = new DIDLParser().parse(metaData);
            return content.getItems().stream().flatMap(item -> item.getProperties().stream()).filter(p -> p instanceof DIDLObject.Property.DC.DESCRIPTION).findFirst().map(p -> App.gson().<Map<String, String>>fromJson(p.getValue().toString(), TypeToken.getParameterized(Map.class, String.class, String.class).getType())).orElse(new HashMap<>());
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    @Override
    public synchronized void setNextAVTransportURI(UnsignedIntegerFourBytes instanceId, String nextURI, String nextURIMetaData) {
        this.nextURI = nextURI != null ? nextURI : "";
        this.nextMetaData = nextURIMetaData != null ? nextURIMetaData : "";
    }

    @Override
    public MediaInfo getMediaInfo(UnsignedIntegerFourBytes instanceId) {
        String nURI = nextURI.isEmpty() ? "" : nextURI;
        String nMeta = nextURI.isEmpty() ? "" : nextMetaData;
        String durStr = cachedDuration > 0 ? formatMs(cachedDuration) : "00:00:00";
        return new MediaInfo(currentURI, currentMetaData, nURI, nMeta, new UnsignedIntegerFourBytes(1), durStr, StorageMedium.NETWORK);
    }

    @Override
    public TransportInfo getTransportInfo(UnsignedIntegerFourBytes instanceId) {
        String speed = (player != null && dlnaActive) ? String.valueOf(player.getSpeed()) : "1";
        return new TransportInfo(currentState, TransportStatus.OK, speed);
    }

    @Override
    public PositionInfo getPositionInfo(UnsignedIntegerFourBytes instanceId) {
        long posMs = cachedPosition;
        long durMs = cachedDuration;
        if (posMs < 0) return new PositionInfo(1, currentMetaData, currentURI);
        String relTime = formatMs(posMs);
        String durStr = durMs > 0 ? formatMs(durMs) : "00:00:00";
        return new PositionInfo(1, durStr, currentMetaData, currentURI, relTime, relTime, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public DeviceCapabilities getDeviceCapabilities(UnsignedIntegerFourBytes instanceId) {
        return new DeviceCapabilities(new StorageMedium[]{StorageMedium.NETWORK});
    }

    @Override
    public TransportSettings getTransportSettings(UnsignedIntegerFourBytes instanceId) {
        return new TransportSettings(currentPlayMode, RecordQualityMode.NOT_IMPLEMENTED);
    }

    @Override
    public void stop(UnsignedIntegerFourBytes instanceId) {
        fireStateChange(RenderState.STOPPED);
        App.post(() -> {
            if (player != null && dlnaActive) player.stop();
        });
    }

    @Override
    public void play(UnsignedIntegerFourBytes instanceId, String speed) {
        App.post(() -> {
            if (player == null || !dlnaActive) return;
            int state = player.getPlaybackState();
            if (!currentURI.isEmpty() && (state == Player.STATE_ENDED || state == Player.STATE_IDLE)) {
                startCastActivity(new CastAction(currentURI, currentMetaData, parseHeaders(currentMetaData)));
            } else {
                player.play();
            }
        });
    }

    @Override
    public void pause(UnsignedIntegerFourBytes instanceId) {
        App.post(() -> {
            if (player != null && dlnaActive) player.pause();
        });
    }

    @Override
    public void record(UnsignedIntegerFourBytes instanceId) {
        throw new UnsupportedOperationException("Recording not supported");
    }

    @Override
    public void seek(UnsignedIntegerFourBytes instanceId, String unit, String target) {
        if (!SeekMode.REL_TIME.toString().equals(unit) && !SeekMode.ABS_TIME.toString().equals(unit)) return;
        long ms = parseTimeToMs(target);
        if (dlnaActive) {
            PlayerManager local = player;
            if (local != null) App.post(() -> local.seekTo(ms));
        } else {
            pendingSeekMs = ms;
        }
    }

    @Override
    public void next(UnsignedIntegerFourBytes instanceId) {
        if (!canNext()) return;
        CastAction action = popNext();
        if (action != null) startCastActivity(action);
    }

    @Override
    public void previous(UnsignedIntegerFourBytes instanceId) {
        App.post(() -> {
            if (player != null && dlnaActive) player.seekTo(0);
        });
    }

    @Override
    public void setPlayMode(UnsignedIntegerFourBytes instanceId, String newPlayMode) {
        boolean repeat = PlayMode.REPEAT_ONE.name().equals(newPlayMode);
        currentPlayMode = repeat ? PlayMode.REPEAT_ONE : PlayMode.NORMAL;
        App.post(() -> {
            if (player != null && dlnaActive) player.setRepeatOne(repeat);
        });
    }

    @Override
    public void setRecordQualityMode(UnsignedIntegerFourBytes instanceId, String newRecordQualityMode) {
        throw new UnsupportedOperationException("Recording not supported");
    }

    @Override
    protected TransportAction[] getCurrentTransportActions(UnsignedIntegerFourBytes instanceId) {
        return switch (currentState) {
            case PLAYING -> withNext(TransportAction.Pause, TransportAction.Stop, TransportAction.Seek);
            case PAUSED_PLAYBACK -> withNext(TransportAction.Play, TransportAction.Stop, TransportAction.Seek);
            default -> withNext(TransportAction.Play);
        };
    }

    private boolean canNext() {
        return dlnaActive && !currentURI.isEmpty() && hasNext();
    }

    private TransportAction[] withNext(TransportAction... actions) {
        if (!canNext()) return actions;
        TransportAction[] result = Arrays.copyOf(actions, actions.length + 1);
        result[actions.length] = TransportAction.Next;
        return result;
    }

    private void startCastActivity(CastAction action) {
        Intent intent = new Intent(context, CastActivity.class);
        intent.putExtra(CastAction.KEY_EXTRA, action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public boolean hasNext() {
        return !nextURI.isEmpty();
    }

    public synchronized CastAction popNext() {
        if (nextURI.isEmpty()) return null;
        currentURI = nextURI;
        currentMetaData = nextMetaData;
        nextURI = "";
        nextMetaData = "";
        return new CastAction(currentURI, currentMetaData, parseHeaders(currentMetaData));
    }

    public void fireStateChange(RenderState state) {
        try {
            currentState = toTransportState(state);
            getLastChange().setEventedValue(AbstractAVTransportService.getDefaultInstanceID(), new AVTransportVariable.TransportState(currentState));
            getLastChange().fire(propertyChangeSupport);
        } catch (Exception ignored) {
        }
    }

    private TransportState toTransportState(RenderState state) {
        return switch (state) {
            case PLAYING -> TransportState.PLAYING;
            case PAUSED -> TransportState.PAUSED_PLAYBACK;
            case STOPPED -> TransportState.STOPPED;
            case PREPARING -> TransportState.TRANSITIONING;
            default -> TransportState.NO_MEDIA_PRESENT;
        };
    }

    private long parseTimeToMs(String time) {
        try {
            String[] parts = time.split(":");
            if (parts.length != 3) return 0;
            int dot = parts[2].indexOf('.');
            long secs = Long.parseLong(dot >= 0 ? parts[2].substring(0, dot) : parts[2]);
            long frac = dot >= 0 ? Math.round(Double.parseDouble("0" + parts[2].substring(dot)) * 1000) : 0;
            return (Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + secs) * 1000 + frac;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String formatMs(long ms) {
        if (ms <= 0) return "00:00:00";
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
