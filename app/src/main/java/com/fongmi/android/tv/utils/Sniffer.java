package com.fongmi.android.tv.utils;

import java.util.regex.Pattern;

public class Sniffer {

    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36";

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
