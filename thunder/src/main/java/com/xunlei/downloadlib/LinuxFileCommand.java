package com.xunlei.downloadlib;

import java.io.IOException;

public class LinuxFileCommand {

    public final Runtime shell;

    public LinuxFileCommand(Runtime runtime) {
        shell = runtime;
    }

    public final void deleteDirectory(String dire) throws IOException {
        shell.exec(new String[]{"rm", "-r", dire});
    }
}
