package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
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
        if (path.startsWith("/file")) return getFile(path);
        if (path.startsWith("/upload")) return upload(session.getParms(), files);
        if (path.startsWith("/newFolder")) return newFolder(session.getParms());
        if (path.startsWith("/delFolder") || path.startsWith("/delFile")) return delFolder(session.getParms());
        return null;
    }

    private NanoHTTPD.Response getFile(String url) {
        try {
            File file = Path.root(url.substring(6));
            if (file.isFile()) return Nano.newChunkedResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", new FileInputStream(file));
            else return Nano.success(listFiles(file));
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private NanoHTTPD.Response upload(Map<String, String> params, Map<String, String> files) {
        String path = params.get("path");
        for (String k : files.keySet()) {
            String fn = params.get(k);
            File temp = new File(files.get(k));
            if (fn.toLowerCase().endsWith(".zip")) Path.unzip(temp, Path.root(path));
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

    private String getParent(File root) {
        return root.equals(Path.root()) ? "." : root.getParent().replace(Path.rootPath(), "");
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
            obj.addProperty("path", file.getAbsolutePath().replace(Path.rootPath(), ""));
            obj.addProperty("time", format.format(new Date(file.lastModified())));
            obj.addProperty("dir", file.isDirectory() ? 1 : 0);
            files.add(obj);
        }
        return info.toString();
    }
}
