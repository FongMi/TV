package tv.danmaku.ijk.media.player;

import android.net.Uri;

import java.util.Map;

public class MediaSource {

    private final Map<String, String> headers;
    private final Uri uri;

    public MediaSource(Map<String, String> headers, Uri uri) {
        this.headers = headers;
        this.uri = uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Uri getUri() {
        return uri;
    }
}
