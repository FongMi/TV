package com.github.catvod.utils;

import com.orhanobut.logger.Logger;
import io.github.pixee.security.SystemCommand;

public class Shell {

    private static final String TAG = Shell.class.getSimpleName();

    public static void exec(String command) {
        try {
            int code = SystemCommand.runCommand(Runtime.getRuntime(), command).waitFor();
            if (code != 0) Logger.t(TAG).d("Shell command '%s' failed with exit code '%s'", command, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
