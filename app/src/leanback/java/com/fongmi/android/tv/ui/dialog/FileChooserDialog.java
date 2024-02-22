package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;

import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.Players;
import com.github.catvod.utils.Path;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;

public class FileChooserDialog {

    private ChooserDialog dialog;
    private TrackDialog trackDialog;
    private Players player;

    public static FileChooserDialog create() {
        return new FileChooserDialog();
    }

    public FileChooserDialog player(Players player) {
        this.player = player;
        return this;
    }

    public FileChooserDialog trackDialog(TrackDialog dialog) {
        this.trackDialog = dialog;
        return this;
    }

    public void show(Activity activity) {
        dialog = new ChooserDialog(activity);
        dialog.withFilter(false, false, "srt", "ass", "scc", "stl", "ttml");
        dialog.withStartFile(Path.downloadPath());
        dialog.withChosenListener(this::onChoosePath);
        dialog.build().show();
    }


    private void onChoosePath(String path, File pathFile) {
        player.setSub(Sub.from(pathFile.getAbsolutePath()));
        if (dialog != null) dialog.dismiss();
        if (trackDialog != null) trackDialog.dismiss();
    }

}
