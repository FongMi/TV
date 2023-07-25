package com.xunlei.downloadlib;

import java.io.IOException;

public class LinuxFileCommand {

    public final Runtime shell;

    public LinuxFileCommand(Runtime runtime) {
        shell = runtime;
    }

    public final Process deleteFile(String file) throws IOException {
        String[] cmds = {"rm", file};
        return shell.exec(cmds);
    }

    public final Process delete(String file) throws IOException {
        String[] cmds = {"rm", "-r", file};
        return shell.exec(cmds);
    }

    public final Process deleteMult(String[] cmds) throws IOException {
        return shell.exec(cmds);
    }

    public final Process deleteDirectory(String dire) throws IOException {
        String[] cmds = {"rm", "-r", dire};
        return shell.exec(cmds);
    }

    public final Process createFile(String file) throws IOException {
        String[] cmds = {"touch", file};
        return shell.exec(cmds);
    }

    public final Process createDirectory(String dire) throws IOException {
        String[] cmds = {"mkdir", dire};
        return shell.exec(cmds);
    }

    public final Process moveFile(String src, String dir) throws IOException {
        String[] cmds = {"mv", src, dir};
        return shell.exec(cmds);
    }

    public final Process copyFile(String src, String dir) throws IOException {
        String[] cmds = {"cp", "-r", src, dir};
        return shell.exec(cmds);
    }

    public final Process linkFile(String src, String dir) throws IOException {
        String[] cmds = {"ln", "-l", src, dir};
        return shell.exec(cmds);
    }

    public final Process du_s(String file) throws IOException {
        String[] cmds = {"du", "-s", file};
        return shell.exec(cmds);
    }

    public final Process ls_lhd(String file) throws IOException {
        String[] cmds = {"ls", "-l", file};
        return shell.exec(cmds);
    }

    public final Process ls_Directory(String directory) throws IOException {
        if (directory.equals("/")) directory = "";
        String[] cmds = {"ls", "-a", directory};
        return shell.exec(cmds);
    }
}
