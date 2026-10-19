package com.fongmi.android.tv.dlna;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CastVideo;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.ResUtil;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.support.avtransport.callback.Play;
import org.jupnp.support.avtransport.callback.Seek;
import org.jupnp.support.avtransport.callback.SetAVTransportURI;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.DIDLObject;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.SeekMode;
import org.jupnp.support.model.item.VideoItem;

import java.util.Locale;

public record DLNACast(CastVideo video, Callback callback) {

    public void cast(Device item) {
        DLNACastManager mgr = DLNACastManager.get();
        ControlPoint control = mgr.getControlPoint();
        RemoteService service = mgr.findAVTransport(item);
        if (service != null && control != null) control.execute(uriAction(control, service));
        else App.post(() -> callback.error(ResUtil.getString(R.string.device_offline)));
    }

    private String buildMetaData() {
        try {
            DIDLContent content = new DIDLContent();
            VideoItem item = new VideoItem("0", "-1", video.name(), "", new Res(new ProtocolInfo("http-get:*:video/*:*"), 0L, video.url()));
            if (!video.headers().isEmpty()) item.addProperty(new DIDLObject.Property.DC.DESCRIPTION(App.gson().toJson(video.headers())));
            content.addItem(item);
            return new DIDLParser().generate(content);
        } catch (Exception e) {
            return "";
        }
    }

    private SetAVTransportURI uriAction(ControlPoint control, RemoteService service) {
        return new SetAVTransportURI(service, video.url(), buildMetaData()) {
            @Override
            public void success(ActionInvocation i) {
                control.execute(playAction(control, service));
            }

            @Override
            public void failure(ActionInvocation i, UpnpResponse r, String msg) {
                App.post(() -> callback.error(msg));
            }
        };
    }

    private Play playAction(ControlPoint control, RemoteService service) {
        return new Play(service) {
            @Override
            public void success(ActionInvocation i) {
                if (video.position() > 0) control.execute(seekAction(service));
                else App.post(callback::success);
            }

            @Override
            public void failure(ActionInvocation i, UpnpResponse r, String msg) {
                App.post(() -> callback.error(msg));
            }
        };
    }

    private Seek seekAction(RemoteService service) {
        return new Seek(service, SeekMode.REL_TIME, formatMs(video.position())) {
            @Override
            public void success(ActionInvocation i) {
                App.post(callback::success);
            }

            @Override
            public void failure(ActionInvocation i, UpnpResponse r, String m) {
                App.post(() -> callback.error(m));
            }
        };
    }

    private String formatMs(long ms) {
        if (ms <= 0) return "00:00:00";
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
