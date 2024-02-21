package com.github.catvod.utils;

public class Shell {

    public static void exec(String command) {
        try {
            int code = Runtime.getRuntime().exec(command).waitFor();
            if (code != 0) throw new RuntimeException("Shell command failed with exit code " + code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}