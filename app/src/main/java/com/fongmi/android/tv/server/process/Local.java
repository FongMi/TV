package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Local implements Process {

    private final SimpleDateFormat format;

    public Local() {
        this.format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String url) {
        return url.startsWith("/file") || url.startsWith("/upload") || url.startsWith("/newFolder") || url.startsWith("/delFolder") || url.startsWith("/delFile");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files) {
        if (url.startsWith("/file")) return getFile(session.getHeaders(), url);
        if (url.startsWith("/upload")) return upload(session.getParms(), files);
        if (url.startsWith("/newFolder")) return newFolder(session.getParms());
        if (url.startsWith("/delFolder") || url.startsWith("/delFile")) return delFolder(session.getParms());
        return null;
    }

    private NanoHTTPD.Response getFile(Map<String, String> headers, String path) {
        try {
            File file = Path.local(path.substring(5));
            if (file.isDirectory()) return getFolder(file);
            if (file.isFile()) return getFile(headers, file, NanoHTTPD.getMimeTypeForFile(path));
            throw new FileNotFoundException();
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private NanoHTTPD.Response upload(Map<String, String> params, Map<String, String> files) {
        String path = params.get("path");
        for (String k : files.keySet()) {
            String fn = params.get(k);
            File temp = new File(files.get(k));
            if (fn.toLowerCase().endsWith(".zip")) FileUtil.zipDecompress(temp, Path.root(path));
            else Path.copy(temp, Path.root(path, fn));
        }
        return Nano.ok();
    }

    private NanoHTTPD.Response newFolder(Map<String, String> params) {
        String path = params.get("path");
        String name = params.get("name");
        Path.root(path, name).mkdirs();
        return Nano.ok();
    }

    private NanoHTTPD.Response delFolder(Map<String, String> params) {
        String path = params.get("path");
        Path.clear(Path.root(path));
        return Nano.ok();
    }

    private NanoHTTPD.Response getFolder(File root) {
        List<File> list = Path.list(root);
        JsonObject info = new JsonObject();
        info.addProperty("parent", root.equals(Path.root()) ? "." : root.getParent().replace(Path.rootPath(), ""));
        if (list.isEmpty()) {
            info.add("files", new JsonArray());
            return Nano.ok(info.toString());
        }
        JsonArray files = new JsonArray();
        info.add("files", files);
        for (File file : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", file.getName());
            obj.addProperty("path", file.getAbsolutePath().replace(Path.rootPath(), ""));
            obj.addProperty("time", format.format(new Date(file.lastModified())));
            obj.addProperty("dir", file.isDirectory() ? 1 : 0);
            files.add(obj);
        }
        return Nano.ok(info.toString());
    }

    private NanoHTTPD.Response getFile(Map<String, String> headers, File file, String mime) throws IOException {
        long fileLen = file.length();
        long startFrom = 0, endAt = fileLen - 1;
        String range = headers.get("range");
        if (range != null && range.startsWith("bytes=")) {
            try {
                String[] parts = range.substring(6).split("-", 2);
                if (!parts[0].isEmpty()) startFrom = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isEmpty()) endAt = Long.parseLong(parts[1]);
                if (startFrom > endAt) startFrom = 0;
                if (endAt >= fileLen) endAt = fileLen - 1;
            } catch (NumberFormatException ignored) {
                startFrom = 0;
                endAt = fileLen - 1;
            }
        }
        long contentLength;
        NanoHTTPD.Response res;
        String ifRange = headers.get("if-range");
        String ifNoneMatch = headers.get("if-none-match");
        String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + fileLen).hashCode());
        boolean ifRangeMatch = ifRange == null || ifRange.equals(etag);
        boolean ifNoneMatchHit = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));
        if (ifRangeMatch && range != null && startFrom < fileLen) {
            if (ifNoneMatchHit) {
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mime, "");
                contentLength = 0;
            } else {
                long newLen = endAt - startFrom + 1;
                FileInputStream fis = new FileInputStream(file);
                long skipped = 0;
                while (skipped < startFrom) {
                    long s = fis.skip(startFrom - skipped);
                    if (s <= 0) break;
                    skipped += s;
                }
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mime, fis, newLen);
                res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                contentLength = newLen;
            }
        } else if (range != null && startFrom >= fileLen) {
            res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
            res.addHeader("Content-Range", "bytes */" + fileLen);
            contentLength = 0;
        } else if (ifNoneMatchHit) {
            res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mime, "");
            contentLength = 0;
        } else {
            FileInputStream fis = new FileInputStream(file);
            res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mime, fis, fileLen);
            contentLength = fileLen;
        }
        res.addHeader("Content-Length", String.valueOf(contentLength));
        res.addHeader("Accept-Ranges", "bytes");
        res.addHeader("ETag", etag);
        return res;
    }
}
