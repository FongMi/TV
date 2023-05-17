package com.fongmi.android.tv.server;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.server.process.ActionRequestProcess;
import com.fongmi.android.tv.server.process.RawRequestProcess;
import com.fongmi.android.tv.server.process.RequestProcess;
import com.fongmi.android.tv.utils.FileUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class Nano extends NanoHTTPD {

    private List<RequestProcess> processes;
    private final SimpleDateFormat format;

    public Nano(int port) {
        super(port);
        addRequestProcess();
        format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
    }

    private void addRequestProcess() {
        processes = new ArrayList<>();
        processes.add(new ActionRequestProcess());
        processes.add(new RawRequestProcess("/", R.raw.index, MIME_HTML));
        processes.add(new RawRequestProcess("/index.html", R.raw.index, MIME_HTML));
        processes.add(new RawRequestProcess("/ui.css", R.raw.ui, "text/css"));
        processes.add(new RawRequestProcess("/style.css", R.raw.style, "text/css"));
        processes.add(new RawRequestProcess("/script.js", R.raw.script, "application/x-javascript"));
        processes.add(new RawRequestProcess("/favicon.ico", R.mipmap.ic_launcher, "image/x-icon"));
    }

    public static Response createSuccessResponse() {
        return createSuccessResponse("OK");
    }

    public static Response createSuccessResponse(String text) {
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, text);
    }

    public static Response createErrorResponse(String text) {
        return createErrorResponse(Response.Status.INTERNAL_ERROR, text);
    }

    public static Response createErrorResponse(Response.IStatus status, String text) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, text);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri().trim();
        Map<String, String> files = new HashMap<>();
        if (url.contains("?")) url = url.substring(0, url.indexOf('?'));
        if (session.getMethod() == Method.POST) parseBody(session, files);
        for (RequestProcess process : processes) {
            if (process.isRequest(session, url)) {
                return process.doResponse(session, url);
            }
        }
        switch (session.getMethod()) {
            case GET:
                if (url.startsWith("/file")) return doFile(url);
                else if (url.startsWith("/proxy")) return doProxy(session.getParms());
                else if (url.startsWith("/device")) return createSuccessResponse(Device.get().toString());
                break;
            case POST:
                if (url.startsWith("/upload")) return doUpload(session.getParms(), files);
                else if (url.startsWith("/newFolder")) return doNewFolder(session.getParms());
                else if (url.startsWith("/delFolder") || url.startsWith("/delFile")) return doDelFolder(session.getParms());
                break;
        }
        return createErrorResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Not Found");
    }

    private void parseBody(IHTTPSession session, Map<String, String> files) {
        String ct = session.getHeaders().get("content-type");
        if (ct != null && ct.toLowerCase().contains("multipart/form-data") && !ct.toLowerCase().contains("charset=")) {
            Matcher matcher = Pattern.compile("[ |\t]*(boundary[ |\t]*=[ |\t]*['|\"]?[^\"^'^;^,]*['|\"]?)", Pattern.CASE_INSENSITIVE).matcher(ct);
            String boundary = matcher.find() ? matcher.group(1) : null;
            if (boundary != null) session.getHeaders().put("content-type", "multipart/form-data; charset=utf-8; " + boundary);
        }
        try {
            session.parseBody(files);
        } catch (Exception ignored) {
        }
    }

    private Response doFile(String url) {
        try {
            String path = url.substring(6);
            File file = FileUtil.getRootFile(path);
            if (file.isFile()) return newChunkedResponse(Response.Status.OK, "application/octet-stream", new FileInputStream(file));
            else return createSuccessResponse(listFiles(file));
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    private Response doProxy(Map<String, String> params) {
        try {
            Object[] rs = ApiConfig.get().proxyLocal(params);
            return newChunkedResponse(Response.Status.lookup((Integer) rs[0]), (String) rs[1], (InputStream) rs[2]);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    private Response doUpload(Map<String, String> params, Map<String, String> files) {
        String path = params.get("path");
        for (String k : files.keySet()) {
            String fn = params.get(k);
            File temp = new File(files.get(k));
            if (fn.toLowerCase().endsWith(".zip")) FileUtil.unzip(temp, FileUtil.getRootPath() + File.separator + path);
            else FileUtil.copy(temp, FileUtil.getRootFile(path + File.separator + fn));
        }
        return createSuccessResponse();
    }

    private Response doNewFolder(Map<String, String> params) {
        String path = params.get("path");
        String name = params.get("name");
        FileUtil.getRootFile(path + File.separator + name).mkdirs();
        return createSuccessResponse();
    }

    private Response doDelFolder(Map<String, String> params) {
        String path = params.get("path");
        FileUtil.clearDir(FileUtil.getRootFile(path));
        return createSuccessResponse();
    }

    private String getParent(File root) {
        if (root.getAbsolutePath().equals(FileUtil.getRootPath())) return ".";
        return root.getParentFile().getAbsolutePath().replace(FileUtil.getRootPath() + File.separator, "").replace(FileUtil.getRootPath(), "");
    }

    private String listFiles(File root) {
        File[] list = root.listFiles();
        String parent = getParent(root);
        JsonObject info = new JsonObject();
        info.addProperty("parent", parent);
        if (list == null || list.length == 0) {
            info.add("files", new JsonArray());
            return info.toString();
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
            obj.addProperty("path", file.getAbsolutePath().replace(FileUtil.getRootPath() + File.separator, ""));
            obj.addProperty("time", format.format(new Date(file.lastModified())));
            obj.addProperty("dir", file.isDirectory() ? 1 : 0);
            files.add(obj);
        }
        return info.toString();
    }
}
