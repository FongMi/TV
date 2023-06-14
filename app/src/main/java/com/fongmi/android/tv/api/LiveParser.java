package com.fongmi.android.tv.api;

import android.util.Base64;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.net.OkHttp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveParser {

    private static final Pattern GROUP = Pattern.compile(".*group-title=\"(.?|.+?)\".*");
    private static final Pattern LOGO = Pattern.compile(".*tvg-logo=\"(.?|.+?)\".*");
    private static final Pattern NAME = Pattern.compile(".*,(.+?)$");

    private static String extract(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line.trim());
        if (matcher.matches()) return matcher.group(1);
        return "";
    }

    public static void start(Live live) {
        if (live.getGroups().size() > 0) return;
        if (live.getType() == 0) text(live, getText(live.getUrl()));
        if (live.getType() == 1) json(live, getText(live.getUrl()));
        if (live.getType() == 2) proxy(live, getText(Utils.checkProxy(live.getUrl())));
    }

    public static void text(Live live, String text) {
        int number = 0;
        if (live.getGroups().size() > 0) return;
        if (text.trim().startsWith("#EXTM3U")) m3u(live, text);
        else txt(live, text);
        for (Group group : live.getGroups()) {
            for (Channel channel : group.getChannel()) {
                channel.setNumber(++number);
                channel.live(live);
            }
        }
    }

    private static void json(Live live, String text) {
        live.getGroups().addAll(Group.arrayFrom(text));
        for (Group group : live.getGroups()) {
            for (Channel channel : group.live(live).getChannel()) {
                channel.live(live);
            }
        }
    }

    private static void m3u(Live live, String text) {
        Channel channel = Channel.create("");
        for (String line : text.split("\n")) {
            if (Thread.interrupted()) break;
            if (line.startsWith("#EXTINF:")) {
                Group group = live.find(Group.create(extract(line, GROUP)));
                channel = group.find(Channel.create(extract(line, NAME)));
                channel.setLogo(extract(line, LOGO));
            } else if (line.contains("://")) {
                channel.getUrls().add(line);
            }
        }
    }

    private static void txt(Live live, String text) {
        for (String line : text.split("\n")) {
            String[] split = line.split(",");
            if (split.length < 2) continue;
            if (Thread.interrupted()) break;
            if (line.contains("#genre#")) live.getGroups().add(Group.create(split[0], live.isPass()));
            if (live.getGroups().isEmpty()) live.getGroups().add(Group.create(R.string.live_group));
            if (split[1].contains("://")) {
                Group group = live.getGroups().get(live.getGroups().size() - 1);
                group.find(Channel.create(split[0])).addUrls(split[1].split("#"));
            }
        }
    }

    private static void proxy(Live live, String text) {
        int number = 0;
        for (Live item : Live.arrayFrom(text)) {
            Group group = live.find(Group.create(item.getGroup()));
            for (Channel channel : item.getChannels()) {
                channel.setNumber(++number);
                channel.live(live);
                group.add(channel);
            }
        }
    }

    private static String getText(String url) {
        try {
            if (url.startsWith("file")) return FileUtil.read(url);
            if (url.startsWith("http")) return OkHttp.newCall(url).execute().body().string();
            if (url.endsWith(".txt") || url.endsWith(".m3u")) return getText(Utils.convert(LiveConfig.getUrl(), url));
            if (url.length() > 0 && url.length() % 4 == 0) return getText(new String(Base64.decode(url, Base64.DEFAULT)));
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
