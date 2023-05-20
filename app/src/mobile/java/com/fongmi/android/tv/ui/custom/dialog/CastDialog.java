package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.android.cast.dlna.dmc.DLNACastManager;
import com.android.cast.dlna.dmc.OnDeviceRegistryListener;
import com.android.cast.dlna.dmc.control.ICastInterface;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.cast.CastDevice;
import com.fongmi.android.tv.cast.CastVideo;
import com.fongmi.android.tv.cast.ScanEvent;
import com.fongmi.android.tv.cast.ScanTask;
import com.fongmi.android.tv.databinding.DialogDeviceBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.activity.ScanActivity;
import com.fongmi.android.tv.ui.adapter.DeviceAdapter;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class CastDialog extends BaseDialog implements DeviceAdapter.OnClickListener, ScanTask.Listener, OnDeviceRegistryListener, ICastInterface.CastEventListener {

    private final FormBody.Builder body;
    private final OkHttpClient client;
    private DialogDeviceBinding binding;
    private DeviceAdapter adapter;
    private Listener listener;
    private CastVideo video;

    public static CastDialog create() {
        return new CastDialog();
    }

    public CastDialog() {
        client = OkHttp.client(1000);
        body = new FormBody.Builder();
        body.add("url", ApiConfig.getUrl());
        body.add("device", Device.get().toString());
    }

    public CastDialog history(History history) {
        String id = history.getVodId();
        String fd = id.startsWith("file") ? Server.get().getAddress() + "/" + id.replace(FileUtil.getRootPath(), "") : id;
        body.add("history", history.toString().replace(id, fd));
        return this;
    }

    public CastDialog video(CastVideo video) {
        this.video = video;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
        this.listener = (Listener) activity;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDeviceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setRecyclerView();
        getDevice();
        initDLNA();
    }

    @Override
    protected void initEvent() {
        binding.scan.setOnClickListener(v -> onScan());
        binding.refresh.setOnClickListener(v -> onRefresh());
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter = new DeviceAdapter(this));
    }

    private void getDevice() {
        adapter.addAll(Device.getAll());
        adapter.addAll(CastDevice.get().getAll());
        if (CastDevice.get().isEmpty()) App.post(() -> onRefresh(false), 1000);
    }

    private void initDLNA() {
        DLNACastManager.getInstance().bindCastService(App.get());
        DLNACastManager.getInstance().registerDeviceListener(this);
        DLNACastManager.getInstance().registerActionCallbacks(this);
    }

    private void onRefresh() {
        onRefresh(true);
    }

    private void onRefresh(boolean clear) {
        DLNACastManager.getInstance().search(null, 15);
        ScanTask.create(this).start(adapter.getIps());
        if (clear) adapter.clear();
    }

    private void onScan() {
        ScanActivity.start(getActivity());
    }

    private void onSuccess(Device item) {
        if (!item.isMobile()) listener.onCastTo();
        dismiss();
    }

    private void onError() {
        Notify.show(R.string.device_offline);
    }

    @Override
    public void onSuccess(String result) {
        listener.onCastTo();
        dismiss();
    }

    @Override
    public void onFailed(String errMsg) {
        Notify.show(errMsg);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanEvent(ScanEvent event) {
        ScanTask.create(this).start(event.getAddress());
    }

    @Override
    public void onFind(List<Device> devices) {
        if (devices.size() > 0) adapter.addAll(devices);
    }

    @Override
    public void onDeviceAdded(org.fourthline.cling.model.meta.Device<?, ?, ?> device) {
        adapter.addAll(CastDevice.get().add(device));
    }

    @Override
    public void onDeviceRemoved(org.fourthline.cling.model.meta.Device<?, ?, ?> device) {
        adapter.remove(CastDevice.get().remove(device));
    }

    @Override
    public void onDeviceUpdated(org.fourthline.cling.model.meta.Device<?, ?, ?> device) {
    }

    @Override
    public void onItemClick(Device item) {
        if (item.isDLNA()) DLNACastManager.getInstance().cast(CastDevice.get().find(item), video);
        else OkHttp.newCall(client, item.getIp().concat("/action?do=cast"), body.build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean ok = response.body().string().equals("OK");
                if (ok) App.post(() -> onSuccess(item));
                else App.post(() -> onError());
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                App.post(() -> onError());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        DLNACastManager.getInstance().unregisterListener(this);
        DLNACastManager.getInstance().unregisterActionCallbacks();
        DLNACastManager.getInstance().unbindCastService(App.get());
    }

    public interface Listener {

        void onCastTo();
    }
}
