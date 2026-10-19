package com.github.catvod.utils;

import com.github.catvod.Runtime;

import java.io.InputStream;

public class Asset {

    public static InputStream open(String fileName) {
        try {
            return Runtime.context().getAssets().open(fileName.replace("assets://", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public static String read(String fileName) {
        try {
            return Path.read(open(fileName));
        } catch (Exception e) {
            return "";
        }
    }
}
