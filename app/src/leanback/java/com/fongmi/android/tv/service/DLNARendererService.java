package com.fongmi.android.tv.service;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.dlna.CastAction;
import com.fongmi.android.tv.dlna.DLNAAvTransportImpl;
import com.fongmi.android.tv.dlna.DLNARenderingControlImpl;
import com.fongmi.android.tv.dlna.DLNAServiceConfiguration;
import com.fongmi.android.tv.dlna.RenderState;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Prefers;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.android.AndroidUpnpServiceImpl;
import org.jupnp.binding.LocalServiceBindingException;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.model.ProtocolInfos;
import org.jupnp.support.lastchange.LastChangeAwareServiceManager;
import org.jupnp.support.lastchange.LastChangeDelegator;
import org.jupnp.support.lastchange.LastChangeParser;
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.jupnp.support.renderingcontrol.RenderingControlErrorCode;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DLNARendererService extends AndroidUpnpServiceImpl implements ServiceConnection {

    private final IBinder binder = new LocalBinder();

    private volatile PlayerManager player;
    private volatile boolean isDlnaActive;

    private DLNARenderingControlImpl renderingControlImpl;
    private DLNAAvTransportImpl avTransportImpl;
    private EventManager<DLNAAvTransportImpl> avTransportEvents;
    private EventManager<DLNARenderingControlImpl> renderingControlEvents;
    private PlaybackService playbackService;
    private Player currentListenerPlayer;
    private Object dlnaOwner;
    private boolean bound;

    public static void start(Context context) {
        context.startService(new Intent(context, DLNARendererService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, DLNARendererService.class));
    }

    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new DLNAServiceConfiguration();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new NotificationCompat.Builder(this, Notify.DEFAULT).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string.app_name)).setSilent(true).build();
        startForeground(Notify.ID + 1, notification);
        upnpService.startup();
        registerLocalDevice();
    }

    private void registerLocalDevice() {
        LocalService<DLNAAvTransportImpl> avTransport = createAvTransport();
        LocalService<ConnectionManagerService> connManager = createConnectionManager();
        LocalService<DLNARenderingControlImpl> renderControl = createRenderingControl();
        String udn = Prefers.getString("dlna_udn");
        if (udn.isEmpty()) Prefers.put("dlna_udn", udn = UUID.randomUUID().toString());
        DeviceIdentity identity = new DeviceIdentity(new UDN(udn));
        UDADeviceType type = new UDADeviceType("MediaRenderer", 1);
        DeviceDetails details = new DeviceDetails(Util.getDeviceName(), new ManufacturerDetails(Build.MANUFACTURER), new ModelDetails(Build.MODEL, "DLNA Renderer", "1.0"));
        try {
            LocalDevice device = new LocalDevice(identity, type, details, new LocalService[]{avTransport, connManager, renderControl});
            upnpService.getRegistry().addDevice(device);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private LocalService<DLNAAvTransportImpl> createAvTransport() {
        avTransportImpl = new DLNAAvTransportImpl(this, () -> avTransportEvents.schedule());
        LocalService<DLNAAvTransportImpl> service = readService(DLNAAvTransportImpl.class, AVTransportErrorCode.INVALID_INSTANCE_ID.getCode());
        service.setManager(avTransportEvents = new EventManager<>(service, new AVTransportLastChangeParser()) {
            @Override
            protected DLNAAvTransportImpl createServiceInstance() {
                return avTransportImpl;
            }
        });
        return service;
    }

    @SuppressWarnings("unchecked")
    private LocalService<ConnectionManagerService> createConnectionManager() {
        LocalService<ConnectionManagerService> service = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        service.setManager(new DefaultServiceManager<>(service) {
            @Override
            protected ConnectionManagerService createServiceInstance() {
                return new ConnectionManagerService(new ProtocolInfos(), new ProtocolInfos("http-get:*:video/*:*,http-get:*:audio/*:*"));
            }
        });
        return service;
    }

    @SuppressWarnings("unchecked")
    private LocalService<DLNARenderingControlImpl> createRenderingControl() {
        renderingControlImpl = new DLNARenderingControlImpl(this, () -> renderingControlEvents.schedule());
        LocalService<DLNARenderingControlImpl> service = readService(DLNARenderingControlImpl.class, RenderingControlErrorCode.INVALID_INSTANCE_ID.getCode());
        service.setManager(renderingControlEvents = new EventManager<>(service, new RenderingControlLastChangeParser()) {
            @Override
            protected DLNARenderingControlImpl createServiceInstance() {
                return renderingControlImpl;
            }
        });
        return service;
    }

    private <T> LocalService<T> readService(Class<T> type, int instanceError) {
        return new AnnotationLocalServiceBinder() {
            @Override
            protected Map<Action, ActionExecutor> readActions(Class<?> type, Map<StateVariable, StateVariableAccessor> variables, Set<Class> stringTypes) throws LocalServiceBindingException {
                Map<Action, ActionExecutor> actions = super.readActions(type, variables, stringTypes);
                actions.replaceAll((action, executor) -> invocation -> {
                    ActionArgumentValue<?> instance = invocation.getInput("InstanceID");
                    if (instance != null && !DLNAAvTransportImpl.getDefaultInstanceID().equals(instance.getValue())) invocation.setFailure(new ActionException(instanceError, "Invalid InstanceID"));
                    else executor.execute(invocation);
                });
                return actions;
            }
        }.read(type);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        avTransportEvents.close();
        renderingControlEvents.close();
        unbindPlaybackService();
        super.onDestroy();
    }

    private void bindPlaybackService() {
        if (bound) return;
        bound = bindService(new Intent(this, PlaybackService.class).setAction(PlaybackService.LOCAL_BIND_ACTION), this, BIND_AUTO_CREATE);
    }

    private void cleanupPlaybackRefs() {
        App.removeCallbacks(positionUpdater);
        if (currentListenerPlayer != null) {
            currentListenerPlayer.removeListener(listener);
            currentListenerPlayer = null;
        }
        if (playbackService != null) {
            playbackService.removePlayerCallback(playerCallback);
            playbackService = null;
        }
        player = null;
        if (avTransportImpl != null) avTransportImpl.setPlayerManager(null);
    }

    private void unbindPlaybackService() {
        if (!bound) return;
        bound = false;
        cleanupPlaybackRefs();
        unbindService(this);
    }

    public void activateDlna(Object owner, String uri) {
        if (avTransportImpl == null || !avTransportImpl.isCurrentURI(uri)) return;
        dlnaOwner = owner;
        setDlnaActive(true);
        bindPlaybackService();
    }

    public void deactivateDlna(Object owner, String uri) {
        if (dlnaOwner != owner || avTransportImpl == null || !avTransportImpl.isCurrentURI(uri)) return;
        releaseDlna();
    }

    private void releaseDlna() {
        dlnaOwner = null;
        setDlnaActive(false);
        avTransportImpl.reset();
        unbindPlaybackService();
    }

    private void setDlnaActive(boolean active) {
        isDlnaActive = active;
        if (avTransportImpl != null) avTransportImpl.setDlnaActive(active);
    }

    public void onPrepared(String uri) {
        if (avTransportImpl != null) avTransportImpl.onPrepared(uri);
    }

    public CastAction consumeNext() {
        return avTransportImpl != null ? avTransportImpl.popNext() : null;
    }

    public void notifyError() {
        if (avTransportImpl != null) avTransportImpl.fireStateChange(RenderState.STOPPED);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        if (!bound || !isDlnaActive) {
            unbindPlaybackService();
            return;
        }
        playbackService = ((PlaybackService.LocalBinder) binder).getService();
        playbackService.addPlayerCallback(playerCallback);
        player = playbackService.player();
        avTransportImpl.setPlayerManager(player);
        currentListenerPlayer = player.getPlayer();
        currentListenerPlayer.addListener(listener);
        App.post(positionUpdater, 1000);
        avTransportImpl.applyPendingCommands();
        notifyState();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        cleanupPlaybackRefs();
    }

    private void notifyState() {
        if (avTransportImpl == null || player == null || !isDlnaActive || !avTransportImpl.ownsPlayer()) return;
        int state = player.getPlaybackState();
        RenderState renderState = switch (state) {
            case Player.STATE_IDLE -> RenderState.STOPPED;
            case Player.STATE_BUFFERING -> RenderState.PREPARING;
            case Player.STATE_READY -> player.isPlaying() ? RenderState.PLAYING : RenderState.PAUSED;
            case Player.STATE_ENDED -> avTransportImpl.hasNext() ? RenderState.PREPARING : RenderState.STOPPED;
            default -> null;
        };
        avTransportImpl.updateState(player.getKey(), player.getPosition(), getDuration(), player.getSpeed(), renderState);
    }

    private final Runnable positionUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null && avTransportImpl != null && avTransportImpl.ownsPlayer() && player.isPlaying()) avTransportImpl.updatePositionCache(player.getKey(), player.getPosition(), getDuration());
            if (player != null) App.post(this, 1000);
        }
    };

    private long getDuration() {
        long duration = player.getDuration();
        return duration == C.TIME_UNSET || duration <= 0 ? -1 : duration;
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_READY) avTransportImpl.applyPendingCommands();
            notifyState();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            notifyState();
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            notifyState();
        }
    };

    private final PlaybackService.PlayerCallback playerCallback = new PlaybackService.PlayerCallback() {
        @Override
        public void onPrepare() {
            if (player != null && avTransportImpl != null && avTransportImpl.isDlnaActive() && !avTransportImpl.isCurrentURI(player.getKey())) {
                releaseDlna();
                return;
            }
            if (player != null) onPrepared(player.getKey());
            notifyState();
        }

        @Override
        public void onPlayerRebuild(Player newPlayer) {
            if (currentListenerPlayer != null) currentListenerPlayer.removeListener(listener);
            currentListenerPlayer = newPlayer;
            newPlayer.addListener(listener);
            notifyState();
        }
    };

    private abstract static class EventManager<T extends LastChangeDelegator> extends LastChangeAwareServiceManager<T> implements Runnable {

        private volatile boolean closed;
        private boolean scheduled;
        private long generation;
        private long publishedGeneration = -1;

        EventManager(LocalService<T> service, LastChangeParser parser) {
            super(service, parser);
        }

        synchronized void schedule() {
            if (closed) return;
            generation++;
            if (scheduled) return;
            scheduled = true;
            App.post(this, 300);
        }

        synchronized void close() {
            closed = true;
            scheduled = false;
            App.removeCallbacks(this);
        }

        @Override
        public void run() {
            long expectedGeneration;
            synchronized (this) {
                if (closed) return;
                expectedGeneration = generation;
            }
            try {
                getPropertyChangeSupport().firePropertyChange(ServiceManager.EVENTED_STATE_VARIABLES, null, getCurrentState());
                getImplementation().getLastChange().reset();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to publish DLNA state", e);
            }
            synchronized (this) {
                if (closed) return;
                scheduled = publishedGeneration != generation;
                publishedGeneration = expectedGeneration;
                if (scheduled) App.post(this, 300);
            }
        }
    }

    public class LocalBinder extends Binder {

        public DLNARendererService getService() {
            return DLNARendererService.this;
        }
    }
}
