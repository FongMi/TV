package com.github.catvod.utils;

import java.io.DataOutputStream;

public class Shell {

    private static final String COMMAND_SH = "sh";
    private static final String COMMAND_EXIT = "exit\n";
    private static final String COMMAND_LINE_END = "\n";

    public static void exec(String command) {
        try {
            Process p = Runtime.getRuntime().exec(COMMAND_SH);
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            dos.write(command.getBytes());
            dos.writeBytes(COMMAND_LINE_END);
            dos.writeBytes(COMMAND_EXIT);
            dos.flush();
            dos.close();
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
