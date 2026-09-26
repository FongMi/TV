package com.fongmi.android.tv.utils;

import static org.junit.Assert.assertEquals;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.catvod.Init;
import com.github.catvod.utils.Path;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public final class PathTest {

    @Test
    public void replacesFilesAtomically() throws Exception {
        File file = new File(Init.context().getCacheDir(), "path-atomic-test");
        File source = new File(Init.context().getCacheDir(), "path-atomic-source");
        try {
            Path.writeAtomically(file, "old".getBytes(StandardCharsets.UTF_8));
            Path.write(source, "file".getBytes(StandardCharsets.UTF_8));
            Path.writeAtomically(file, source);
            assertEquals("file", Path.read(file));

            Path.write(source, "uri".getBytes(StandardCharsets.UTF_8));
            Path.writeAtomically(file, Uri.fromFile(source));
            assertEquals("uri", Path.read(file));

            Path.writeAtomically(file, "bytes".getBytes(StandardCharsets.UTF_8));
            assertEquals("bytes", Path.read(file));
        } finally {
            Path.clear(file);
            Path.clear(source);
        }
    }
}
