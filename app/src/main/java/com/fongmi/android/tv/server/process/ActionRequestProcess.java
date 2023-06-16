package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.net.OkHttp;

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
                onSearch(params);
                break;
            case "push":
                onPush(params);
                break;
            case "api":
                onApi(params);
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

    private void onSearch(Map<String, String> params) {
        String word = Objects.requireNonNullElse(params.get("word"), "");
        if (word.length() > 0) ServerEvent.search(word);
    }

    private void onPush(Map<String, String> params) {
        String url = Objects.requireNonNullElse(params.get("url"), "");
        if (url.length() > 0) ServerEvent.push(url);
    }

    private void onApi(Map<String, String> params) {
        String url = Objects.requireNonNullElse(params.get("url"), "");
        if (url.length() > 0) ServerEvent.api(url);
    }

    private void onCast(Map<String, String> params) {
        Config config = Config.find(params.get("url"), 0);
        Device device = Device.objectFrom(params.get("device"));
        History history = History.objectFrom(params.get("history"));
        CastEvent.post(config, device, history);
    }

    private void onSync(Map<String, String> params) {
        boolean sync = Objects.equals(params.get("mode"), "0");
        boolean keep = Objects.equals(params.get("type"), "keep");
        boolean history = Objects.equals(params.get("type"), "history");
        Device device = Device.objectFrom(params.get("device"));
        if (params.get("device") != null && sync) {
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
            OkHttp.newCall(OkHttp.client(Constant.TIMEOUT_SYNC), device.getIp().concat("/action?do=sync&mode=0&type=history"), body.build()).execute();
        } catch (Exception e) {
            App.post(() -> Notify.show(e.getMessage()));
        }
    }

    private void sendKeep(Device device) {
        try {
            FormBody.Builder body = new FormBody.Builder();
            body.add("targets", App.gson().toJson(Keep.getVod()));
            body.add("configs", App.gson().toJson(Config.findUrls()));
            OkHttp.newCall(OkHttp.client(Constant.TIMEOUT_SYNC), device.getIp().concat("/action?do=sync&mode=0&type=keep"), body.build()).execute();
        } catch (Exception e) {
            App.post(() -> Notify.show(e.getMessage()));
        }
    }

    public void syncHistory(Map<String, String> params) {
        String url = params.get("url");
        if (TextUtils.isEmpty(url)) return;
        Config config = Config.find(url, 0);
        boolean replace = Objects.equals(params.get("mode"), "1");
        List<History> targets = History.arrayFrom(params.get("targets"));
        if (ApiConfig.get().getConfig().equals(config)) {
            if (replace) History.delete(config.getId());
            History.sync(targets);
        } else {
            ApiConfig.load(config, getCallback(targets));
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

    private void syncKeep(Map<String, String> params) {
        List<Config> configs = Config.arrayFrom(params.get("configs"));
        List<Keep> targets = Keep.arrayFrom(params.get("targets"));
        boolean replace = Objects.equals(params.get("mode"), "1");
        if (ApiConfig.getUrl().isEmpty() && configs.size() > 0) {
            ApiConfig.load(Config.find(configs.get(0), 0), getCallback(configs, targets));
        } else {
            if (replace) Keep.deleteAll();
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
