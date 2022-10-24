package com.forcetech;

import android.net.Uri;

public class Port {

    public static int FORCE = 9001;
    public static int MTV = 9002;

    public static int get(String url) {
        switch (Uri.parse(url).getScheme()) {
            case "P2p":
                return FORCE;
            case "mitv":
                return MTV;
            default:
                return -1;
        }
    }
}
