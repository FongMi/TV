package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Local implements Process {

    private final SimpleDateFormat format;

    public Local() {
        this.format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return path.startsWith("/file") || path.startsWith("/upload") || path.startsWith("/newFolder") || path.startsWith("/delFolder") || path.startsWith("/delFile");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path, Map<String, String> files) {
        if (path.startsWith("/file")) return getFile(session.getHeaders(), path);
        if (path.startsWith("/upload")) return upload(session.getParms(), files);
        if (path.startsWith("/newFolder")) return newFolder(session.getParms());
        if (path.startsWith("/delFolder") || path.startsWith("/delFile")) return delFolder(session.getParms());
        return null;
    }

    private NanoHTTPD.Response getFile(Map<String, String> headers, String path) {
        try {
            File file = Path.root(path.substring(5));
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
            if (fn.toLowerCase().endsWith(".zip")) FileUtil.unzip(temp, Path.root(path));
            else Path.copy(temp, Path.root(path, fn));
        }
        return Nano.success();
    }

    private NanoHTTPD.Response newFolder(Map<String, String> params) {
        String path = params.get("path");
        String name = params.get("name");
        Path.root(path, name).mkdirs();
        return Nano.success();
    }

    private NanoHTTPD.Response delFolder(Map<String, String> params) {
        String path = params.get("path");
        Path.clear(Path.root(path));
        return Nano.success();
    }

    private NanoHTTPD.Response getFolder(File root) {
        File[] list = root.listFiles();
        JsonObject info = new JsonObject();
        info.addProperty("parent", root.equals(Path.root()) ? "." : root.getParent().replace(Path.rootPath(), ""));
        if (list == null || list.length == 0) {
            info.add("files", new JsonArray());
            return Nano.success(info.toString());
        }
        Arrays.sort(list, (o1, o2) -> {
            if (o1.isDirectory() && o2.isFile()) return -1;
            return o1.isFile() && o2.isDirectory() ? 1 : o1.getName().compareTo(o2.getName());
        });
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
        return Nano.success(info.toString());
    }

    private NanoHTTPD.Response getFile(Map<String, String> header, File file, String mime) throws Exception {
        long startFrom = 0;
        long endAt = -1;
        String range = header.get("range");
        if (range != null) {
            if (range.startsWith("bytes=")) {
                range = range.substring("bytes=".length());
                int minus = range.indexOf('-');
                try {
                    if (minus > 0) {
                        startFrom = Long.parseLong(range.substring(0, minus));
                        endAt = Long.parseLong(range.substring(minus + 1));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        NanoHTTPD.Response res;
        long fileLen = file.length();
        String ifRange = header.get("if-range");
        String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());
        boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));
        String ifNoneMatch = header.get("if-none-match");
        boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && ("*".equals(ifNoneMatch) || ifNoneMatch.equals(etag));
        if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
            if (headerIfNoneMatchPresentAndMatching) {
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mime, "");
                res.addHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                res.addHeader(HttpHeaders.ETAG, etag);
            } else {
                if (endAt < 0) endAt = fileLen - 1;
                long newLen = endAt - startFrom + 1;
                if (newLen < 0) newLen = 0;
                FileInputStream fis = new FileInputStream(file);
                fis.skip(startFrom);
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mime, fis, newLen);
                res.addHeader(HttpHeaders.CONTENT_LENGTH, newLen + "");
                res.addHeader(HttpHeaders.CONTENT_RANGE, "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                res.addHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                res.addHeader(HttpHeaders.ETAG, etag);
            }
        } else {
            if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                res.addHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLen);
                res.addHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                res.addHeader(HttpHeaders.ETAG, etag);
            } else if (headerIfNoneMatchPresentAndMatching && (!headerIfRangeMissingOrMatching || range == null)) {
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_MODIFIED, mime, "");
                res.addHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                res.addHeader(HttpHeaders.ETAG, etag);
            } else {
                res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mime, new FileInputStream(file), (int) file.length());
                res.addHeader(HttpHeaders.CONTENT_LENGTH, fileLen + "");
                res.addHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                res.addHeader(HttpHeaders.ETAG, etag);
            }
        }
        return res;
    }
}
