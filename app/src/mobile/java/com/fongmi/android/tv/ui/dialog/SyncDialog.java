package com.fongmi.android.tv.ui.dialog;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Task;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class SyncDialog extends DeviceDialog {

    private final OkHttpClient client;
    private final TypedArray mode;

    private String type;

    public SyncDialog() {
        client = OkHttp.client(Constant.TIMEOUT_SYNC);
        mode = ResUtil.getTypedArray(R.array.cast_mode);
    }

    public static SyncDialog create() {
        return new SyncDialog();
    }

    public SyncDialog history() {
        return type("history");
    }

    public SyncDialog keep() {
        return type("keep");
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof SyncDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    private SyncDialog type(String type) {
        Bundle args = new Bundle();
        args.putString("type", type);
        setArguments(args);
        return this;
    }

    @Override
    protected void initView() {
        type = requireArguments().getString("type");
        super.initView();
        binding.title.setText("history".equals(type) ? R.string.device_sync_history_title : R.string.device_sync_keep_title);
        binding.mode.setVisibility(View.VISIBLE);
        setMode();
    }

    @Override
    protected void initEvent() {
        super.initEvent();
        binding.mode.setOnClickListener(v -> onMode());
    }

    private void setMode() {
        int index = Setting.getSyncMode();
        binding.mode.setImageResource(mode.getResourceId(index, 0));
        binding.mode.setTag(String.valueOf(index));
        binding.mode.setContentDescription(getString(getModeText(index)));
    }

    private int getModeText(int index) {
        return switch (index) {
            case 1 -> R.string.dialog_action_sync_upload;
            case 2 -> R.string.dialog_action_sync_download;
            default -> R.string.dialog_action_sync_two;
        };
    }

    private void onMode() {
        int index = Setting.getSyncMode();
        Setting.putSyncMode(index == mode.length() - 1 ? 0 : index + 1);
        setMode();
    }

    @Override
    public void onItemClick(Device item) {
        send(item, binding.mode.getTag().toString(), false);
    }

    @Override
    public boolean onLongClick(Device item) {
        String mode = binding.mode.getTag().toString();
        if (mode.equals("0")) return false;
        send(item, mode, true);
        return true;
    }

    private void send(Device item, String mode, boolean force) {
        requestLocalNetwork(() -> sync(item, mode, force));
    }

    private void sync(Device item, String mode, boolean force) {
        if (!accepts(item)) return;
        int id = beginOperation(item, R.string.device_sync_pending);
        if (id == 0) return;
        String url = String.format(Locale.getDefault(), "%s/action?do=sync&mode=%s&type=%s%s", item.getIp(), mode, type, force ? "&force=true" : "");
        Runnable request = () -> OkHttp.newCall(client, url, buildBody()).enqueue(getCallback(id));
        if (type.equals("history")) Task.executeSerial(request);
        else request.run();
    }

    private FormBody buildBody() {
        FormBody.Builder body = new FormBody.Builder().add("device", Device.get().toString());
        if (type.equals("history")) {
            Config config = Config.vod();
            body.add("config", config.toString());
            body.add("targets", App.gson().toJson(History.get(config.getId())));
        } else {
            body.add("targets", App.gson().toJson(Keep.getVod()));
            body.add("configs", App.gson().toJson(Config.findUrls()));
        }
        return body.build();
    }

    private Callback getCallback(int id) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response res = response) {
                    boolean accepted = res.isSuccessful() && res.body() != null && res.body().string().equals("OK");
                    App.post(() -> onSyncResponse(id, accepted));
                } catch (IOException e) {
                    onFailure(call, e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                App.post(() -> onSyncFailure(id, e));
            }
        };
    }

    private void onSyncResponse(int id, boolean accepted) {
        if (!completeOperation(id)) return;
        if (accepted) dismiss();
        else Notify.show(R.string.device_offline);
    }

    private void onSyncFailure(int id, IOException error) {
        if (completeOperation(id)) Notify.show(error.getMessage());
    }
}
