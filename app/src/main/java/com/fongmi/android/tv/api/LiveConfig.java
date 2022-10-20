package com.fongmi.android.tv.api;

import android.util.Base64;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.net.OKHttp;

import java.util.ArrayList;
import java.util.List;

public class LiveConfig {

    private List<Live> lives;

    private static class Loader {
        static volatile LiveConfig INSTANCE = new LiveConfig();
    }

    public static LiveConfig get() {
        return Loader.INSTANCE;
    }

    public LiveConfig init() {
        this.lives = new ArrayList<>();
        return this;
    }

    public List<Live> getLives() {
        return lives;
    }

    public void parse(Channel item) {
        if (lives == null) init();
        if (item.getUrls().isEmpty()) return;
        if (!item.getUrls().get(0).startsWith("proxy://")) return;
        try {
            String base64 = item.getUrls().get(0).split("ext=")[1];
            String url = new String(Base64.decode(base64, Base64.DEFAULT));
            Live live = new Live(item.getName(), new ArrayList<>());
            parse(OKHttp.newCall(url).execute().body().string(), live);
            if (live.getGroups().size() > 0) getLives().add(live);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parse(String result, Live live) {
        for (String line : result.split("\n")) {
            String[] split = line.split(",");
            if (split.length < 2) continue;
            if (line.contains("#genre#")) {
                live.getGroups().add(new Group(split[0]));
            }
            if (split[1].contains("://")) {
                Group group = live.getGroups().get(live.getGroups().size() - 1);
                Channel channel = new Channel(group.getChannel().size() + 1, split[0], split[1].split("#"));
                int index = group.getChannel().indexOf(channel);
                if (index != -1) group.getChannel().get(index).getUrls().addAll(channel.getUrls());
                else group.getChannel().add(channel);
            }
        }
    }
}
