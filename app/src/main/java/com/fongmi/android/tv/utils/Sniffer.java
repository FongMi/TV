package com.fongmi.android.tv.utils;

import java.util.regex.Pattern;

public class Sniffer {

    public static final Pattern RULE = Pattern.compile(
                    "http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg)\\?.*|" +
                    "http((?!http).)*?cdn-tos[^?]*|" +
                    "http((?!http).)*?/obj/tos[^?]*|" +
                    "http((?!http).)*?/video/tos[^?]*"
    );
}
