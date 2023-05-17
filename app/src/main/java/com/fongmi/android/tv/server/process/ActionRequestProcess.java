package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.utils.Notify;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.FormBody;

public class ActionRequestProcess implements RequestProcess {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return session.getMethod() == NanoHTTPD.Method.POST && path.equals("/action");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path) {
        Map<String, String> params = session.getParms();
        switch (Objects.requireNonNullElse(params.get("do"), "")) {
            case "search":
                onSearch(params.get("word").trim());
                break;
            case "push":
                onPush(params.get("url").trim());
                break;
            case "api":
                onApi(params.get("url").trim());
                break;
            case "cast":
                onCast(params);
                break;
            case "sync":
                onSync(params);
                break;
        }
        return Nano.createSuccessResponse();
    }

    public void onSearch(String word) {
        if (word.length() > 0) ServerEvent.search(word);
    }

    public void onPush(String url) {
        if (url.length() > 0) ServerEvent.push(url);
    }

    public void onApi(String url) {
        if (url.length() > 0) ServerEvent.api(url);
    }

    public void onCast(Map<String, String> params) {
        Config config = Config.find(params.get("url"), 0);
        Device device = Device.objectFrom(params.get("device"));
        History history = History.objectFrom(params.get("history"));
        CastEvent.post(config, device, history);
    }

    public void onSync(Map<String, String> params) {
        boolean keep = params.get("type").equals("keep");
        boolean history = params.get("type").equals("history");
        Device device = Device.objectFrom(params.get("device"));
        if (params.get("device") != null) {
            if (history) sendHistory(device, params);
            else if (keep) sendKeep(device);
        }
        if (history) {
            syncHistory(params);
        } else if (keep) {
            syncKeep(params);
        }
    }

    private void sendHistory(Device device, Map<String, String> params) {
        try {
            String url = Objects.requireNonNullElse(params.get("url"), ApiConfig.getUrl());
            FormBody.Builder body = new FormBody.Builder();
            body.add("url", url);
            body.add("targets", App.gson().toJson(History.get(Config.find(url, 0).getId())));
            OkHttp.newCall(OkHttp.client(1000), device.getIp().concat("/action?do=sync&type=history"), body.build()).execute();
        } catch (Exception e) {
            App.post(() -> Notify.show(e.getMessage()));
        }
    }

    private void sendKeep(Device device) {
        try {
            FormBody.Builder body = new FormBody.Builder();
            body.add("targets", App.gson().toJson(Keep.getVod()));
            body.add("configs", App.gson().toJson(Config.findUrls()));
            OkHttp.newCall(OkHttp.client(1000), device.getIp().concat("/action?do=sync&type=keep"), body.build()).execute();
        } catch (Exception e) {
            App.post(() -> Notify.show(e.getMessage()));
        }
    }

    public void syncHistory(Map<String, String> params) {
        String url = params.get("url");
        if (TextUtils.isEmpty(url)) return;
        Config config = Config.find(url, 0);
        List<History> targets = History.arrayFrom(params.get("targets"));
        if (ApiConfig.get().getConfig().equals(config)) {
            History.sync(targets);
        } else {
            ApiConfig.get().clear().config(config).load(getCallback(targets));
        }
    }

    private Callback getCallback(List<History> targets) {
        return new Callback() {
            @Override
            public void success() {
                RefreshEvent.config();
                RefreshEvent.video();
                History.sync(targets);
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
            }
        };
    }

    public void syncKeep(Map<String, String> params) {
        List<Config> configs = Config.arrayFrom(params.get("configs"));
        List<Keep> targets = Keep.arrayFrom(params.get("targets"));
        if (ApiConfig.getUrl() == null && configs.size() > 0) {
            ApiConfig.get().clear().config(Config.find(configs.get(0), 0)).load(getCallback(configs, targets));
        } else {
            Keep.sync(configs, targets);
        }
    }

    private Callback getCallback(List<Config> configs, List<Keep> targets) {
        return new Callback() {
            @Override
            public void success() {
                RefreshEvent.history();
                RefreshEvent.config();
                RefreshEvent.video();
                Keep.sync(configs, targets);
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
            }
        };
    }
}
