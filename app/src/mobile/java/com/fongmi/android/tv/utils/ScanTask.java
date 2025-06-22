package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.server.Server;
import com.github.catvod.net.OkHttp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class ScanTask {

    private final List<Device> devices;
    private final OkHttpClient client;

    private ExecutorService executor;
    private Listener listener;

    public ScanTask(Listener listener) {
        this.devices = Collections.synchronizedList(new ArrayList<>());
        this.client = OkHttp.client(1000);
        this.listener = listener;
    }

    public void start(List<String> ips) {
        App.execute(() -> run(getUrl(ips)));
    }

    public void start(String url) {
        App.execute(() -> run(List.of(url)));
    }

    public void stop() {
        if (executor != null) executor.shutdownNow();
        executor = null;
        listener = null;
    }

    private void init() {
        if (executor != null) executor.shutdownNow();
        executor = Executors.newCachedThreadPool();
        devices.clear();
    }

    private void run(List<String> items) {
        try {
            init();
            getDevice(items);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            App.post(() -> {
                if (listener != null) listener.onFind(devices);
            });
        }
    }

    private void getDevice(List<String> urls) throws Exception {
        CountDownLatch cd = new CountDownLatch(urls.size() - 1);
        for (String url : urls) executor.execute(() -> findDevice(cd, url));
        cd.await();
    }

    private List<String> getUrl(List<String> ips) {
        Set<String> urls = new HashSet<>(ips);
        String local = Server.get().getAddress();
        String base = local.substring(0, local.lastIndexOf(".") + 1);
        for (int i = 1; i < 256; i++) urls.add(base + i + ":9978");
        return new ArrayList<>(urls);
    }

    private void findDevice(CountDownLatch cd, String url) {
        if (url.contains(Server.get().getAddress())) return;
        try (Response res = OkHttp.newCall(client, url.concat("/device")).execute()) {
            Device device = Device.objectFrom(res.body().string());
            if (device != null) devices.add(device.save());
        } catch (Exception ignored) {
        } finally {
            cd.countDown();
        }
    }

    public interface Listener {

        void onFind(List<Device> devices);
    }
}
