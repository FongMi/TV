package com.fongmi.android.tv.api.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.fongmi.nodejs.NodeClient;
import com.fongmi.nodejs.NodeSpider;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RunWith(AndroidJUnit4.class)
public final class NodeFirstScreenTest {

    private static final String API = "node:/spider/test/3";
    private static final String INDEX = """
            module.exports = {
                start() {
                    let init = 0, home = 0, homeVod = 0;
                    global.catServerFactory((req, res) => {
                        let result;
                        if (req.url === '/config') result = {video: {sites: [{key: 'test', name: 'Test', api: '/spider/test/3'}]}};
                        else if (req.url.endsWith('/init')) result = {init: ++init};
                        else if (req.url.endsWith('/home')) result = {list: [{vod_id: '1', vod_name: 'home-' + ++home}]};
                        else if (req.url.endsWith('/homeVod')) result = {list: [{vod_id: '2', vod_name: 'video-' + ++homeVod}]};
                        else if (req.url.endsWith('/stats')) result = {init, home, homeVod};
                        else { res.statusCode = 404; result = {}; }
                        res.setHeader('Content-Type', 'application/json');
                        res.end(JSON.stringify(result));
                    }).listen(0);
                }
            };
            """;
    private static final String CONFIG = "module.exports = {};";

    @Test
    public void pendingBundleDoesNotProbeBeforeHomeRequest() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NodeClient client = new NodeClient(context, null);
        try {
            String source = writeBundle(context, "node-first-screen", false);
            assertTrue(client.loadConfig(source).json().contains(API));
            // The second load reuses the verified active bundle.
            assertTrue(client.loadConfig(source).json().contains(API));
            JsonObject before = stats(client);
            assertEquals(0, before.get("init").getAsInt());
            assertEquals(0, before.get("home").getAsInt());

            NodeSpider spider = new NodeSpider(client, API);
            spider.init(context, "");
            assertTrue(spider.homeContent(true).contains("home-1"));
            assertTrue(spider.homeVideoContent().contains("video-1"));
            JsonObject after = stats(client);
            assertEquals(1, after.get("init").getAsInt());
            assertEquals(1, after.get("home").getAsInt());
            assertEquals(1, after.get("homeVod").getAsInt());
        } finally {
            client.clear();
        }
    }

    @Test
    public void localBundleStillRequiresMatchingMd5() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NodeClient client = new NodeClient(context, null);
        try {
            String source = writeBundle(context, "node-first-screen-bad-md5", true);
            assertThrows(IOException.class, () -> client.load(source));
        } finally {
            client.clear();
        }
    }

    private static JsonObject stats(NodeClient client) throws Exception {
        return JsonParser.parseString(client.post(API + "/stats", new JsonObject())).getAsJsonObject();
    }

    private static String writeBundle(Context context, String name, boolean badMd5) throws Exception {
        File root = new File(context.getCacheDir(), name);
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Unable to create Node test bundle");
        write(new File(root, "index.js"), INDEX);
        write(new File(root, "index.config.js"), CONFIG);
        write(new File(root, "index.js.md5"), badMd5 ? "00000000000000000000000000000000" : md5(INDEX));
        write(new File(root, "index.config.js.md5"), md5(CONFIG));
        return "file://" + new File(root, "index.js.md5").getAbsolutePath();
    }

    private static void write(File file, String text) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String md5(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
