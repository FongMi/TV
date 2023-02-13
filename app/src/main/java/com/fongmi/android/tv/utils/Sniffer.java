package com.fongmi.android.tv.utils;

import java.util.regex.Pattern;

public class Sniffer {

    public static final Pattern RULE = Pattern.compile(
            "http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)\\?.*|" +
                    "http((?!http).){12,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)|" +
                    "http((?!http).)*?video/tos*"
    );

    public static final Pattern CUSTOM = Pattern.compile(
            "http((?!http).)*?xg.php\\?id=|" +
                    "http((?!http).)*?/m3/(.*)\\.css|" +
                    "http((?!http).)*?_playback/\\?video_id=|" +
                    "http((?!http).)*huoshan.com(.*)?item_id=|" +
                    "http((?!http).)*qianpailive.com(.*)&vid=|" +
                    "http((?!http).)*douyin.com(.*)&is_play_url="
    );
}
