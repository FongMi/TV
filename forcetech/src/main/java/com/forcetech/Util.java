package com.forcetech;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.forcetech.service.P2PService;
import com.forcetech.service.P3PService;
import com.forcetech.service.P4PService;
import com.forcetech.service.P5PService;
import com.forcetech.service.P6PService;
import com.forcetech.service.P7PService;
import com.forcetech.service.P8PService;
import com.forcetech.service.P9PService;
import com.gsoft.mitv.MainActivity;

public class Util {

    public static int MTV = 9002;
    public static int P2P = 9906;
    public static int P3P = 9907;
    public static int P4P = 9908;
    public static int P5P = 9909;
    public static int P6P = 9910;
    public static int P7P = 9911;
    public static int P8P = 9912;
    public static int P9P = 9913;

    public static String scheme(String url) {
        String scheme = Uri.parse(url).getScheme();
        if ("P2p".equals(scheme)) scheme = "mitv";
        return scheme.toLowerCase();
    }

    public static String trans(ComponentName o) {
        String name = o.getClassName();
        name = name.substring(name.lastIndexOf(".") + 1);
        name = name.replace("Service", "");
        name = name.replace("MainActivity", "mitv");
        return name.toLowerCase();
    }

    public static Intent intent(Context context, String scheme) {
        Intent intent = new Intent(context, clz(scheme));
        intent.putExtra("scheme", scheme);
        return intent;
    }

    private static Class<?> clz(String scheme) {
        switch (scheme) {
            case "p2p":
                return P2PService.class;
            case "p3p":
                return P3PService.class;
            case "p4p":
                return P4PService.class;
            case "p5p":
                return P5PService.class;
            case "p6p":
                return P6PService.class;
            case "p7p":
                return P7PService.class;
            case "p8p":
                return P8PService.class;
            case "p9p":
                return P9PService.class;
            default:
                return MainActivity.class;
        }
    }

    public static int port(String scheme) {
        switch (scheme) {
            case "p2p":
                return P2P;
            case "p3p":
                return P3P;
            case "p4p":
                return P4P;
            case "p5p":
                return P5P;
            case "p6p":
                return P6P;
            case "p7p":
                return P7P;
            case "p8p":
                return P8P;
            case "p9p":
                return P9P;
            default:
                return MTV;
        }
    }
}
