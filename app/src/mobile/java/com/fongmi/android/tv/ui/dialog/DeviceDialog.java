package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.databinding.DialogDeviceBinding;
import com.fongmi.android.tv.dlna.DLNACastManager;
import com.fongmi.android.tv.ui.activity.ScanActivity;
import com.fongmi.android.tv.ui.adapter.DeviceAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.RemoteDeviceUrl;
import com.fongmi.android.tv.utils.ScanTask;

import java.util.ArrayList;
import java.util.List;

public abstract class DeviceDialog extends BaseBottomSheetDialog implements DeviceAdapter.OnClickListener, ScanTask.Listener, DLNACastManager.DeviceListener {

    private final List<Device> devices = new ArrayList<>();
    protected DialogDeviceBinding binding;
    protected DeviceAdapter adapter;
    private ScanTask scanTask;
    private int operation;
    private boolean pending;
    private boolean networkDenied;
    private boolean scanning;

    protected boolean includeAppDevices() {
        return true;
    }

    protected boolean includeDlnaDevices() {
        return false;
    }

    protected boolean accepts(Device device) {
        return device.isDLNA() ? includeDlnaDevices() : device.isApp() && includeAppDevices();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDeviceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        scanTask = new ScanTask(this);
        binding.scan.setVisibility(includeAppDevices() ? View.VISIBLE : View.GONE);
        binding.recycler.setHasFixedSize(false);
        binding.recycler.setAdapter(adapter = new DeviceAdapter(this));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        if (includeDlnaDevices()) DLNACastManager.get().setDeviceListener(this);
        requestLocalNetwork(this::loadDevices);
    }

    @Override
    protected void initEvent() {
        binding.scan.setOnClickListener(this::onScan);
        binding.refresh.setOnClickListener(this::onRefresh);
    }

    private void onScan(View view) {
        launcher.launch(new Intent(requireActivity(), ScanActivity.class));
    }

    private void onRefresh(View view) {
        requestLocalNetwork(() -> {
            scanTask.stop();
            devices.clear();
            discoverDevices();
        });
    }

    protected void requestLocalNetwork(Runnable action) {
        View view = getView();
        PermissionUtil.requestLocalNetwork(this, allGranted -> onNetworkPermissionResult(view, allGranted, action));
    }

    private void onNetworkPermissionResult(View view, boolean allGranted, Runnable action) {
        if (view == null || getView() != view) return;
        networkDenied = !allGranted;
        if (networkDenied) {
            scanTask.stop();
            scanning = false;
        }
        showState();
        if (allGranted) action.run();
    }

    protected int beginOperation(Device device, int message) {
        Dialog dialog = getDialog();
        if (getView() == null || dialog == null || !dialog.isShowing() || pending) return 0;
        pending = true;
        binding.pendingText.setText(getString(message, device.getDisplayName()));
        showState();
        return ++operation;
    }

    protected boolean completeOperation(int id) {
        if (getView() == null || !pending || operation != id) return false;
        pending = false;
        showState();
        return true;
    }

    private void loadDevices() {
        devices.clear();
        devices.addAll(Device.getAll().stream().filter(this::accepts).toList());
        if (includeDlnaDevices() || devices.isEmpty()) discoverDevices();
        else showDevices();
    }

    private void discoverDevices() {
        scanning = includeAppDevices();
        showDevices();
        if (includeAppDevices()) scanTask.start();
        if (includeDlnaDevices()) {
            DLNACastManager.get().init(requireActivity());
            for (Device device : DLNACastManager.get().getRegistered()) onFind(device);
        }
    }

    private void showDevices() {
        adapter.setItems(devices.stream().sorted().toList());
        showState();
    }

    private void showState() {
        boolean searching = scanning && devices.isEmpty();
        binding.recycler.setVisibility(pending || devices.isEmpty() ? View.GONE : View.VISIBLE);
        binding.empty.setVisibility(!pending && !scanning && (devices.isEmpty() || networkDenied) ? View.VISIBLE : View.GONE);
        binding.empty.setText(networkDenied ? R.string.device_network_denied : includeDlnaDevices() ? R.string.device_dlna_searching : R.string.device_empty);
        binding.pendingLayout.setVisibility(pending || searching ? View.VISIBLE : View.GONE);
        if (!pending && searching) binding.pendingText.setText(R.string.device_searching);
        binding.mode.setEnabled(!pending);
        binding.scan.setEnabled(!pending);
        binding.refresh.setEnabled(!pending);
    }

    @Override
    public void onScanStarted() {
        if (getView() == null) return;
        scanning = true;
        showState();
    }

    @Override
    public void onScanFinished() {
        if (getView() == null) return;
        scanning = false;
        showState();
    }

    protected void onScanned(String address) {
        if (RemoteDeviceUrl.isLogPage(address)) openBrowser(address, null);
        else requestLocalNetwork(() -> scanTask.start(address));
    }

    protected void openBrowser(String address, String path) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(path == null ? address : RemoteDeviceUrl.page(address, path))));
        } catch (ActivityNotFoundException e) {
            Notify.show(R.string.device_browser_missing);
        } catch (IllegalArgumentException e) {
            Notify.show(R.string.device_address_invalid);
        }
    }

    @Override
    public void onFind(Device device) {
        if (getView() == null || !accepts(device)) return;
        devices.removeIf(item -> item.isSameItem(device));
        devices.add(device);
        showDevices();
    }

    @Override
    public void onDeviceAdded(Device device) {
        onFind(device);
    }

    @Override
    public void onDeviceRemoved(Device device) {
        if (getView() == null) return;
        devices.removeIf(item -> item.isSameItem(device));
        showDevices();
    }

    @Override
    public boolean onLongClick(Device item) {
        return false;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        operation++;
        pending = false;
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        operation++;
        pending = false;
        networkDenied = false;
        scanning = false;
        scanTask.stop();
        if (includeDlnaDevices()) DLNACastManager.get().release(requireActivity(), this);
        super.onDestroyView();
        devices.clear();
        binding = null;
        adapter = null;
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
        try {
            onScanned(result.getData().getStringExtra("address"));
        } catch (IllegalArgumentException e) {
            Notify.show(R.string.device_address_invalid);
        }
    });
}
