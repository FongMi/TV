package com.fongmi.android.tv.api;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveParser {

    private static final Pattern GROUP = Pattern.compile(".*group-title=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOGO = Pattern.compile(".*tvg-logo=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile(".*,(.+?)$", Pattern.CASE_INSENSITIVE);

    private static String extract(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) return matcher.group(1);
        return "";
    }

    public static void start(Live live, String text) {
        int number = 0;
        if (text.startsWith("#EXTM3U")) m3u(live, text); else txt(live, text);
        for (Group group : live.getGroups()) {
            for (Channel channel : group.getChannel()) {
                channel.setNumber(++number);
            }
        }
    }

    private static void m3u(Live live, String text) {
        Channel channel = Channel.create("");
        for (String line : text.split("\n")) {
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
            if (line.contains("#genre#")) {
                live.getGroups().add(Group.create(split[0]));
            }
            if (split[1].contains("://")) {
                Group group = live.getGroups().get(live.getGroups().size() - 1);
                group.find(Channel.create(split[0])).addUrls(split[1].split("#"));
            }
        }
    }
}
