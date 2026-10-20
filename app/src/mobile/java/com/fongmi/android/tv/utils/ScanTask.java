package com.fongmi.android.tv.utils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.server.Server;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class ScanTask {

    private static final Dispatcher dispatcher = new Dispatcher();

    private final List<Call> calls;
    private final List<Device> devices;
    private final OkHttpClient client;
    private final Listener listener;
    private boolean replaceCache;
    private int generation;
    private int remaining;

    public ScanTask(Listener listener) {
        this.client = OkHttp.client(1000).newBuilder().dispatcher(dispatcher).build();
        this.calls = new ArrayList<>();
        this.devices = new ArrayList<>();
        this.listener = listener;
    }

    public void start() {
        start(getUrl(Server.get().getAddress()), true);
    }

    public void start(String url) {
        start(List.of(RemoteDeviceUrl.origin(url)), false);
    }

    private synchronized void start(List<String> urls, boolean replaceCache) {
        stop();
        this.replaceCache = replaceCache && !urls.isEmpty();
        int id = generation;
        remaining = urls.size();
        App.post(() -> notifyStart(id));
        if (urls.isEmpty()) {
            App.post(() -> notifyComplete(id));
            return;
        }
        String local = Server.get().getAddress().concat("/");
        for (String url : urls) {
            if (RemoteDeviceUrl.origin(url).equals(local)) finish(null, id);
            else findDevice(url, id);
        }
    }

    public synchronized void stop() {
        generation++;
        remaining = 0;
        calls.forEach(Call::cancel);
        calls.clear();
        devices.clear();
    }

    private List<String> getUrl(String local) {
        int index = local.lastIndexOf(".");
        if (index < 0) return List.of();
        String base = local.substring(0, index + 1);
        return IntStream.range(1, 256).mapToObj(i -> base + i + ":9978").toList();
    }

    private void findDevice(String url, int id) {
        Call call;
        synchronized (this) {
            if (generation != id) return;
            call = OkHttp.newCall(client, RemoteDeviceUrl.page(url, "/device"), "scan");
            calls.add(call);
        }
        call.enqueue(getCallback(id));
    }

    private Callback getCallback(int id) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response res = response) {
                    if (!res.isSuccessful() || res.body() == null) return;
                    Device device = Device.objectFrom(res.body().string());
                    if (device != null && device.isApp()) App.post(() -> publish(device, id));
                } catch (Exception ignored) {
                } finally {
                    finish(call, id);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                finish(call, id);
            }
        };
    }

    private synchronized void publish(Device device, int id) {
        if (generation != id) return;
        devices.removeIf(item -> item.isSameItem(device));
        devices.add(device);
        listener.onFind(replaceCache ? device : device.save());
    }

    private synchronized void notifyStart(int id) {
        if (generation == id) listener.onScanStarted();
    }

    private synchronized void finish(Call call, int id) {
        calls.remove(call);
        if (generation != id || --remaining != 0) return;
        App.post(() -> notifyComplete(id));
    }

    private synchronized void notifyComplete(int id) {
        if (generation != id) return;
        if (replaceCache) Device.replaceAppDevices(devices);
        listener.onScanFinished();
    }

    public interface Listener {

        void onFind(Device device);

        default void onScanStarted() {
        }

        default void onScanFinished() {
        }
    }
}
