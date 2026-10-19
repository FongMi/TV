package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;

import com.fongmi.android.tv.BuildConfig;

public class RestartActivity extends Activity {

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, RestartActivity.class).putExtra("pid", Process.myPid()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pid = getIntent().getIntExtra("pid", -1);
        if (pid <= 0 || pid == Process.myPid()) {
            finish();
            return;
        }
        Process.killProcess(pid);
        killNodeProcess();
        startActivity(Intent.makeRestartActivityTask(new ComponentName(this, HomeActivity.class)));
        finish();
        Process.killProcess(Process.myPid());
    }

    private void killNodeProcess() {
        var processes = getSystemService(ActivityManager.class).getRunningAppProcesses();
        if (processes == null) return;
        for (var process : processes) if (process.uid == Process.myUid() && (BuildConfig.APPLICATION_ID + ":nodejs").equals(process.processName)) Process.killProcess(process.pid);
    }
}
