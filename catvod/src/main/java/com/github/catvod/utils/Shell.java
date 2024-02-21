package com.github.catvod.utils;

import com.orhanobut.logger.Logger;

public class Shell {

    public static void exec(String command) {
        try {
            int code = Runtime.getRuntime().exec(command).waitFor();
            if (code != 0) Logger.e("Shell command failed with exit code " + code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}