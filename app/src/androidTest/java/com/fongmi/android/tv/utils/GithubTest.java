package com.fongmi.android.tv.utils;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class GithubTest {

    @Test
    public void apkUrlComesFromMatchingReleaseAsset() throws Exception {
        JSONObject release = new JSONObject("""
                {"assets":[
                  {"name":"leanback-arm64_v8a.apk","state":"uploaded","browser_download_url":"https://example.com/release/leanback.apk"},
                  {"name":"mobile-arm64_v8a.apk","state":"uploaded","browser_download_url":"https://example.com/release/mobile.apk"}
                ]}
                """);

        assertEquals("https://example.com/release/mobile.apk", Github.getApk(release, "mobile-arm64_v8a.apk"));
        assertEquals("", Github.getApk(release, "mobile-armeabi_v7a.apk"));
    }
}
