package com.fongmi.android.tv.ui.dialog;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CastVideo;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.dlna.DLNACast;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class CastDialog extends DeviceDialog {

    private final OkHttpClient client;

    private CastVideo video;
    private boolean fm;

    public CastDialog() {
        client = OkHttp.client(Constant.TIMEOUT_SYNC);
    }

    public static CastDialog create(PlayerManager player) {
        return createInternal(player, player.isVod());
    }

    public static CastDialog createLive(PlayerManager player) {
        return createInternal(player, false);
    }

    private static CastDialog createInternal(PlayerManager player, boolean app) {
        CastDialog dialog = new CastDialog();
        dialog.fm = app;
        dialog.video = CastVideo.create(player, dialog.fm ? player.getPosition() : C.TIME_UNSET);
        Bundle args = new Bundle();
        args.putBoolean("app", dialog.fm);
        args.putString("video", App.gson().toJson(dialog.video));
        args.putString("config", Config.vod().toString());
        dialog.setArguments(args);
        return dialog;
    }

    public CastDialog history(History history) {
        String id = history.getVodId();
        String fd = history.getVodId();
        if (fd.startsWith("/")) fd = Server.get().getAddress() + "/file" + fd.replace(Path.rootPath(), "");
        if (fd.startsWith("file")) fd = Server.get().getAddress() + "/" + fd.replace(Path.rootPath(), "").replace("://", "");
        if (fd.contains("127.0.0.1")) fd = fd.replace("127.0.0.1", Util.getIp());
        requireArguments().putString("history", history.toString().replace(id, fd));
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof CastDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected boolean includeAppDevices() {
        return fm;
    }

    @Override
    protected void initView() {
        Bundle args = requireArguments();
        fm = args.getBoolean("app");
        video = App.gson().fromJson(args.getString("video"), CastVideo.class);
        super.initView();
        binding.title.setText(R.string.device_cast_title);
    }

    @Override
    protected boolean includeDlnaDevices() {
        return true;
    }

    @Override
    public void onItemClick(Device item) {
        requestLocalNetwork(() -> cast(item));
    }

    private void cast(Device item) {
        if (!accepts(item)) return;
        int id = beginOperation(item, R.string.device_cast_pending);
        if (id == 0) return;
        Callback callback = getCallback(id);
        if (item.isDLNA()) new DLNACast(video, callback).cast(item);
        else OkHttp.newCall(client, item.getIp().concat("/action?do=cast"), buildBody()).enqueue(callback);
    }

    private Callback getCallback(int id) {
        PlayerManager player = Server.get().getService() == null ? null : Server.get().getService().player();
        MediaItem source = player != null && !player.isEmpty() && video.equals(CastVideo.create(player, video.position())) ? player.getCurrentMediaItem() : null;
        return new Callback() {
            @Override
            public void success() {
                if (!completeOperation(id)) return;
                if (source != null && !player.isReleased() && source == player.getCurrentMediaItem()) ((CastDialog.Listener) requireActivity()).onCasted();
                dismiss();
            }

            @Override
            public void error(String msg) {
                if (completeOperation(id)) Notify.show(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response res = response) {
                    boolean accepted = res.isSuccessful() && res.body() != null && res.body().string().equals("OK");
                    App.post(() -> {
                        if (accepted) success();
                        else error(ResUtil.getString(R.string.device_offline));
                    });
                } catch (IOException e) {
                    onFailure(call, e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                App.post(() -> error(e.getMessage()));
            }
        };
    }

    private FormBody buildBody() {
        Bundle args = requireArguments();
        return new FormBody.Builder().add("device", Device.get().toString()).add("config", args.getString("config")).add("history", args.getString("history")).build();
    }

    public interface Listener {

        void onCasted();
    }
}
