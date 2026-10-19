package com.fongmi.android.tv.dlna;

import android.content.Context;
import android.content.Intent;

import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.ui.activity.CastActivity;
import com.google.gson.reflect.TypeToken;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.support.avtransport.AbstractAVTransportService;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DLNAAvTransportImpl extends AbstractAVTransportService {

    private final Context context;
    private final Runnable onChange;

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
    private volatile String cachedSpeed = "1";
    private TransportState pendingState;
    private boolean launching;
    private boolean playerPrepared;
    private long generation;

    public DLNAAvTransportImpl(Context context, Runnable onChange) {
        this.context = context;
        this.onChange = onChange;
    }

    public synchronized void setPlayerManager(PlayerManager player) {
        this.player = player;
        if (player == null) reset();
    }

    public synchronized void setDlnaActive(boolean active) {
        this.dlnaActive = active;
        if (!active) generation++;
    }

    public synchronized boolean isDlnaActive() {
        return dlnaActive;
    }

    public synchronized void reset() {
        generation++;
        dlnaActive = false;
        launching = false;
        playerPrepared = false;
        pendingState = null;
        nextURI = "";
        currentURI = "";
        nextMetaData = "";
        pendingSeekMs = -1;
        cachedPosition = -1;
        cachedDuration = -1;
        cachedSpeed = "1";
        currentMetaData = "";
        currentPlayMode = PlayMode.NORMAL;
        fireStateChange(RenderState.IDLE);
    }

    public synchronized void updatePositionCache(String uri, long position, long duration) {
        if (!isCurrentURI(uri)) return;
        cachedPosition = position;
        cachedDuration = duration;
    }

    public synchronized void updateState(String uri, long position, long duration, float speed, RenderState state) {
        if (!isCurrentURI(uri)) return;
        updatePositionCache(uri, position, duration);
        BigDecimal value = new BigDecimal(Float.toString(speed)).stripTrailingZeros();
        cachedSpeed = value.scale() <= 0 ? value.toPlainString() : value.unscaledValue() + "/" + BigInteger.TEN.pow(value.scale());
        if (state != null) fireStateChange(state);
    }

    public synchronized boolean isCurrentURI(String uri) {
        return !currentURI.isEmpty() && currentURI.equals(uri);
    }

    public synchronized boolean ownsPlayer() {
        return dlnaActive && playerPrepared && player != null && isCurrentURI(player.getKey()) && !player.isEmpty();
    }

    public synchronized void onPrepared(String uri) {
        if (!isCurrentURI(uri)) return;
        playerPrepared = true;
        applyPendingCommands();
    }

    public synchronized void applyPendingCommands() {
        if (!ownsPlayer()) return;
        launching = false;
        player.setRepeatOne(currentPlayMode == PlayMode.REPEAT_ONE);
        if (pendingSeekMs >= 0 && player.getPlaybackState() == Player.STATE_READY) {
            player.seekTo(pendingSeekMs);
            pendingSeekMs = -1;
        }
        TransportState state = pendingState;
        pendingState = null;
        if (state == TransportState.STOPPED) player.stop();
        else if (state == TransportState.PAUSED_PLAYBACK) player.pause();
        else if (state == TransportState.PLAYING) player.play();
    }

    private void postCommand(Runnable action) {
        long expectedGeneration = generation;
        App.post(() -> {
            synchronized (DLNAAvTransportImpl.this) {
                if (generation == expectedGeneration) action.run();
            }
        });
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        return new UnsignedIntegerFourBytes[]{getDefaultInstanceID()};
    }

    @Override
    public synchronized void setAVTransportURI(UnsignedIntegerFourBytes instanceId, String currentURI, String currentURIMetaData) throws AVTransportException {
        validateURI(currentURI);
        String previousURI = this.currentURI;
        generation++;
        this.nextURI = "";
        this.nextMetaData = "";
        this.dlnaActive = false;
        this.pendingSeekMs = -1;
        this.pendingState = TransportState.PLAYING;
        this.launching = true;
        this.playerPrepared = false;
        this.cachedPosition = -1;
        this.cachedDuration = -1;
        this.cachedSpeed = "1";
        this.currentURI = currentURI != null ? currentURI : "";
        this.currentMetaData = currentURIMetaData != null ? currentURIMetaData : "";
        fireStateChange(this.currentURI.isEmpty() ? RenderState.IDLE : RenderState.PREPARING);
        if (this.currentURI.isEmpty()) {
            postCommand(() -> {
                if (!previousURI.isEmpty() && player != null && previousURI.equals(player.getKey())) player.stop();
            });
        } else {
            postCommand(() -> startCastActivity(new CastAction(this.currentURI, this.currentMetaData, parseHeaders(this.currentMetaData))));
        }
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
    public synchronized void setNextAVTransportURI(UnsignedIntegerFourBytes instanceId, String nextURI, String nextURIMetaData) throws AVTransportException {
        validateURI(nextURI);
        this.nextURI = nextURI != null ? nextURI : "";
        this.nextMetaData = nextURIMetaData != null ? nextURIMetaData : "";
        fireMediaChange();
    }

    @Override
    public synchronized MediaInfo getMediaInfo(UnsignedIntegerFourBytes instanceId) {
        String nURI = nextURI.isEmpty() ? "" : nextURI;
        String nMeta = nextURI.isEmpty() ? "" : nextMetaData;
        String durStr = cachedDuration > 0 ? formatMs(cachedDuration) : "00:00:00";
        return new MediaInfo(currentURI, currentMetaData, nURI, nMeta, new UnsignedIntegerFourBytes(currentURI.isEmpty() ? 0 : 1), durStr, StorageMedium.NETWORK);
    }

    @Override
    public synchronized TransportInfo getTransportInfo(UnsignedIntegerFourBytes instanceId) {
        return new TransportInfo(currentState, TransportStatus.OK, cachedSpeed);
    }

    @Override
    public synchronized PositionInfo getPositionInfo(UnsignedIntegerFourBytes instanceId) {
        long posMs = cachedPosition;
        long durMs = cachedDuration;
        int track = currentURI.isEmpty() ? 0 : 1;
        if (posMs < 0) return new PositionInfo(track, currentMetaData, currentURI);
        String relTime = formatMs(posMs);
        String durStr = durMs > 0 ? formatMs(durMs) : "00:00:00";
        return new PositionInfo(track, durStr, currentMetaData, currentURI, relTime, relTime, Integer.MAX_VALUE, Integer.MAX_VALUE);
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
    public synchronized void stop(UnsignedIntegerFourBytes instanceId) {
        pendingState = TransportState.STOPPED;
        fireStateChange(RenderState.STOPPED);
        postCommand(this::applyPendingCommands);
    }

    @Override
    public synchronized void play(UnsignedIntegerFourBytes instanceId, String speed) throws AVTransportException {
        if (currentURI.isEmpty()) throw new AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE);
        pendingState = TransportState.PLAYING;
        postCommand(this::onPlay);
    }

    private void onPlay() {
        if (pendingState != TransportState.PLAYING) {
            applyPendingCommands();
            return;
        }
        if (ownsPlayer() && player.getPlaybackState() != Player.STATE_ENDED && player.getPlaybackState() != Player.STATE_IDLE) {
            applyPendingCommands();
        } else if (!launching) {
            launching = true;
            fireStateChange(RenderState.PREPARING);
            startCastActivity(new CastAction(currentURI, currentMetaData, parseHeaders(currentMetaData)));
        }
    }

    @Override
    public synchronized void pause(UnsignedIntegerFourBytes instanceId) {
        pendingState = TransportState.PAUSED_PLAYBACK;
        postCommand(this::applyPendingCommands);
    }

    @Override
    public void record(UnsignedIntegerFourBytes instanceId) {
        throw new UnsupportedOperationException("Recording not supported");
    }

    @Override
    public synchronized void seek(UnsignedIntegerFourBytes instanceId, String unit, String target) throws AVTransportException {
        if (!SeekMode.REL_TIME.toString().equals(unit) && !SeekMode.ABS_TIME.toString().equals(unit)) throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED);
        pendingSeekMs = parseTimeToMs(target);
        postCommand(this::applyPendingCommands);
    }

    @Override
    public synchronized void next(UnsignedIntegerFourBytes instanceId) {
        if (!canNext()) return;
        CastAction action = popNext();
        postCommand(() -> startCastActivity(action));
    }

    @Override
    public synchronized void previous(UnsignedIntegerFourBytes instanceId) {
        pendingSeekMs = 0;
        postCommand(this::applyPendingCommands);
    }

    @Override
    public synchronized void setPlayMode(UnsignedIntegerFourBytes instanceId, String newPlayMode) {
        boolean repeat = PlayMode.REPEAT_ONE.name().equals(newPlayMode);
        currentPlayMode = repeat ? PlayMode.REPEAT_ONE : PlayMode.NORMAL;
        fireMediaChange();
        postCommand(this::applyPendingCommands);
    }

    @Override
    public void setRecordQualityMode(UnsignedIntegerFourBytes instanceId, String newRecordQualityMode) {
        throw new UnsupportedOperationException("Recording not supported");
    }

    @Override
    protected TransportAction[] getCurrentTransportActions(UnsignedIntegerFourBytes instanceId) {
        if (currentURI.isEmpty()) return new TransportAction[0];
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
        generation++;
        dlnaActive = false;
        currentURI = nextURI;
        currentMetaData = nextMetaData;
        nextURI = "";
        nextMetaData = "";
        cachedPosition = -1;
        cachedDuration = -1;
        pendingSeekMs = -1;
        pendingState = TransportState.PLAYING;
        playerPrepared = false;
        launching = true;
        cachedSpeed = "1";
        fireMediaChange();
        return new CastAction(currentURI, currentMetaData, parseHeaders(currentMetaData));
    }

    public synchronized void fireStateChange(RenderState state) {
        currentState = toTransportState(state);
        getLastChange().setEventedValue(getDefaultInstanceID(), new AVTransportVariable.TransportState(currentState));
        fireMediaChange();
    }

    private void fireMediaChange() {
        getLastChange().setEventedValue(getDefaultInstanceID(),
                new AVTransportVariable.AVTransportURI(URI.create(currentURI)),
                new AVTransportVariable.AVTransportURIMetaData(currentMetaData),
                new AVTransportVariable.NextAVTransportURI(URI.create(nextURI)),
                new AVTransportVariable.NextAVTransportURIMetaData(nextMetaData),
                new AVTransportVariable.TransportPlaySpeed(cachedSpeed),
                new AVTransportVariable.CurrentPlayMode(currentPlayMode),
                new AVTransportVariable.CurrentTransportActions(getCurrentTransportActions(getDefaultInstanceID())));
        onChange.run();
    }

    private void validateURI(String uri) throws AVTransportException {
        try {
            if (uri != null) URI.create(uri);
        } catch (IllegalArgumentException e) {
            throw new AVTransportException(ErrorCode.ARGUMENT_VALUE_INVALID, "Invalid transport URI");
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

    private long parseTimeToMs(String time) throws AVTransportException {
        try {
            if (time == null || !time.matches("[0-9]+:[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?")) throw new NumberFormatException();
            String[] parts = time.split(":");
            int dot = parts[2].indexOf('.');
            long secs = Long.parseLong(dot >= 0 ? parts[2].substring(0, dot) : parts[2]);
            long frac = dot >= 0 ? Math.round(Double.parseDouble("0" + parts[2].substring(dot)) * 1000) : 0;
            long seconds = Math.addExact(Math.multiplyExact(Long.parseLong(parts[0]), 3600), Long.parseLong(parts[1]) * 60 + secs);
            return Math.addExact(Math.multiplyExact(seconds, 1000), frac);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
        }
    }

    private String formatMs(long ms) {
        if (ms <= 0) return "00:00:00";
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
