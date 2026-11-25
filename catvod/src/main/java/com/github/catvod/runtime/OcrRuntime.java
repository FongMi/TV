package com.github.catvod.runtime;

import android.graphics.Bitmap;

import java.io.Closeable;
import java.io.IOException;

public interface OcrRuntime extends RuntimePlugin {

    enum Model {
        LATIN, CHINESE, JAPANESE
    }

    interface Session extends Closeable {

        String recognize(Bitmap bitmap) throws IOException;
    }

    Session open(Model model);
}
