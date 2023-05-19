package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.cast.ScanEvent;
import com.fongmi.android.tv.cast.ScanTask;
import com.fongmi.android.tv.databinding.DialogDeviceBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.ui.activity.ScanActivity;
import com.fongmi.android.tv.ui.adapter.DeviceAdapter;
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

public class SyncDialog extends BaseDialog implements DeviceAdapter.OnClickListener, ScanTask.Listener {

    private final FormBody.Builder body;
    private final OkHttpClient client;
    private DialogDeviceBinding binding;
    private DeviceAdapter adapter;
    private String type;

    public static SyncDialog create() {
        return new SyncDialog();
    }

    public SyncDialog() {
        client = OkHttp.client(1000);
        body = new FormBody.Builder();
    }

    public SyncDialog history() {
        type = "history";
        body.add("device", Device.get().toString());
        body.add("targets", App.gson().toJson(History.get()));
        if (ApiConfig.getUrl() != null) body.add("url", ApiConfig.getUrl());
        return this;
    }

    public SyncDialog keep() {
        type = "keep";
        body.add("device", Device.get().toString());
        body.add("targets", App.gson().toJson(Keep.getVod()));
        body.add("configs", App.gson().toJson(Config.findUrls()));
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
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
        if (adapter.getItemCount() == 0) App.post(this::onRefresh, 1000);
    }

    private void onRefresh() {
        ScanTask.create(this).start(adapter.getIps());
        adapter.clear();
    }

    private void onScan() {
        ScanActivity.start(getActivity());
    }

    private void onSuccess() {
        dismiss();
    }

    private void onError() {
        Notify.show(R.string.device_offline);
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
    public void onItemClick(Device item) {
        OkHttp.newCall(client, item.getIp().concat("/action?do=sync&type=").concat(type), body.build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                App.post(() -> onSuccess());
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
    }
}
