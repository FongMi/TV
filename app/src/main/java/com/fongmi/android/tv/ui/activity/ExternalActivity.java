package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.fongmi.android.tv.playback.ExternalPlayback;
import com.fongmi.android.tv.utils.FileChooser;

public final class ExternalActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(getIntent()).setClass(this, HomeActivity.class);
        boolean forwardResult = getCallingActivity() != null;
        intent.putExtra(ExternalPlayback.FORWARD_RESULT, forwardResult);
        int grants = getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.setFlags((forwardResult ? Intent.FLAG_ACTIVITY_FORWARD_RESULT : Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP) | grants);
        Uri source = ExternalPlayback.getSourceUri(getIntent());
        Uri subtitle = ExternalPlayback.getSubtitleUri(getIntent());
        ClipData clip = addGrant(intent.getClipData(), source);
        clip = addGrant(clip, subtitle);
        if (clip != null) intent.setClipData(clip);
        startActivity(intent);
        if (!forwardResult && isTaskRoot()) finishAndRemoveTask();
        else finish();
    }

    private static ClipData addGrant(ClipData clip, Uri uri) {
        if (!FileChooser.isFileSource(uri)) return clip;
        if (clip == null) return ClipData.newRawUri("media", uri);
        for (int i = 0; i < clip.getItemCount(); i++) if (uri.equals(clip.getItemAt(i).getUri())) return clip;
        clip.addItem(new ClipData.Item(uri));
        return clip;
    }
}
