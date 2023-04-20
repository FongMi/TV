package com.fongmi.android.tv.cast;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.server.Server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.OkHttpClient;

public class ScanTask {

    private final Listener listener;
    private final OkHttpClient client;
    private final List<Device> devices;

    public static ScanTask create(Listener listener) {
        return new ScanTask(listener);
    }

    public ScanTask(Listener listener) {
        this.listener = listener;
        this.client = OkHttp.client(1000);
        this.devices = new ArrayList<>();
    }

    public void start(List<String> ips) {
        App.execute(() -> run(getUrl(ips)));
    }

    public void start(String url) {
        App.execute(() -> run(List.of(url)));
    }

    private void run(List<String> items) {
        try {
            getDevice(items);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            App.post(() -> listener.onFind(devices));
        }
    }

    private void getDevice(List<String> urls) throws Exception {
        CountDownLatch cd = new CountDownLatch(urls.size());
        for (String url : urls) new Thread(() -> findDevice(cd, url)).start();
        cd.await();
    }

    private List<String> getUrl(List<String> ips) {
        LinkedHashSet<String> urls = new LinkedHashSet<>(ips);
        String local = Server.get().getAddress();
        String base = local.substring(0, local.lastIndexOf(".") + 1);
        for (int i = 1; i < 256; i++) urls.add(base + i + ":9978");
        return new ArrayList<>(urls);
    }

    private void findDevice(CountDownLatch cd, String url) {
        try {
            if (url.contains(Server.get().getAddress())) return;
            String result = OkHttp.newCall(client, url.concat("/device")).execute().body().string();
            Device device = Device.objectFrom(result);
            if (device == null) return;
            devices.add(device.save());
        } catch (Exception ignored) {
        } finally {
            cd.countDown();
        }
    }

    public interface Listener {

        void onFind(List<Device> devices);
    }
}
