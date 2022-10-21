package com.fongmi.android.tv.api;

import android.util.Base64;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Prefers;

import java.util.ArrayList;
import java.util.List;

public class LiveConfig {

    private List<Live> lives;
    private Live home;

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

    public Live getHome() {
        return home;
    }

    public void setHome() {
        if (home == null) setHome(lives.isEmpty() ? new Live() : lives.get(0));
    }

    public void setHome(Live home) {
        this.home = home;
        this.home.setActivated(true);
        Prefers.putLive(home.getName());
        for (Live item : lives) item.setActivated(home);
    }

    private boolean isProxy(Live live) {
        return live.getGroup().equals("redirect") && live.getChannels().size() > 0 && live.getChannels().get(0).getUrls().size() > 0 && live.getChannels().get(0).getUrls().get(0).startsWith("proxy");
    }

    public void parse(Live live) {
        try {
            if (lives == null) init();
            if (isProxy(live)) live = new Live(live.getChannels().get(0).getName(), live.getChannels().get(0).getUrl().split("ext=")[1]);
            if (live.getType() == 0) parse(live, getTxt(live.getUrl()));
            if (live.getGroups().size() > 0) getLives().add(live);
            if (live.getName().equals(Prefers.getLive())) setHome(live);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTxt(String url) throws Exception {
        if (url.startsWith("file")) return FileUtil.read(url);
        else if (url.startsWith("http")) return OKHttp.newCall(url).execute().body().string();
        else if (url.length() % 4 == 0) return getTxt(new String(Base64.decode(url, Base64.DEFAULT)));
        else return "";
    }

    private void parse(Live live, String txt) {
        for (String line : txt.split("\n")) {
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
