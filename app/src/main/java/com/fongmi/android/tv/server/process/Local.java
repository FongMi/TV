package com.fongmi.android.tv.server.process;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.getMimeTypeForFile;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Formatters;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Local implements Process {

    private static final String FILE = "/file";

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return isFile(url) || url.equals("/upload") || url.equals("/newFolder") || url.equals("/delFolder") || url.equals("/delFile");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        try {
            if (isFile(url)) return getFile(session, url);
            Map<String, String> params = session.getParms();
            if (url.equals("/upload")) upload(params, files);
            else if (url.equals("/newFolder")) newFolder(params);
            else if (url.equals("/delFolder") || url.equals("/delFile")) delete(params);
            else return null;
            return Nano.ok();
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }

    private static boolean isFile(String url) {
        return url.equals(FILE) || url.startsWith(FILE + "/");
    }

    private Response getFile(IHTTPSession session, String url) throws IOException {
        String path = url.substring(FILE.length());
        File file = resolveFile(session, path);
        if (file.isDirectory()) return getFolder(file);
        if (!file.isFile()) throw new FileNotFoundException("File not found");
        return getFile(session.getHeaders(), file, getMimeTypeForFile(path));
    }

    private void upload(Map<String, String> params, Map<String, String> files) throws IOException {
        if (files.isEmpty()) throw new IllegalArgumentException("Missing upload file");
        File directory = resolvePath(requirePath(params));
        if (!directory.isDirectory()) throw new FileNotFoundException("Upload directory not found");
        for (Map.Entry<String, String> entry : files.entrySet()) {
            File source = new File(entry.getValue());
            String name = requireName(params.get(entry.getKey()));
            if (!source.isFile() || !source.canRead()) throw new FileNotFoundException("Upload file not found");
            storeUpload(source, directory, name);
        }
    }

    private void storeUpload(File source, File directory, String name) throws IOException {
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            if (!FileUtil.zipDecompress(source, directory)) throw new IOException("Unable to extract archive");
        } else {
            FileUtil.copyAtomically(source, resolveChild(directory, name));
        }
    }

    private void newFolder(Map<String, String> params) throws IOException {
        File directory = resolvePath(requirePath(params));
        if (!directory.isDirectory()) throw new FileNotFoundException("Parent directory not found");
        File folder = resolveChild(directory, requireName(params.get("name")));
        if (!folder.mkdirs() && !folder.isDirectory()) throw new IOException("Unable to create directory");
    }

    private void delete(Map<String, String> params) throws IOException {
        File target = resolveDeletePath(requirePath(params));
        if (!target.exists()) throw new FileNotFoundException("File not found");
        deleteRecursively(target);
    }

    private Response getFolder(File directory) {
        File root = Path.root();
        String rootPath = root.getAbsolutePath();
        JsonArray files = new JsonArray();
        Path.list(directory).forEach(file -> files.add(buildFileInfo(file, rootPath)));
        JsonObject info = new JsonObject();
        info.addProperty("parent", parentOf(directory, root, rootPath));
        info.add("files", files);
        return Nano.ok(info.toString());
    }

    private JsonObject buildFileInfo(File file, String rootPath) {
        JsonObject info = new JsonObject();
        info.addProperty("name", file.getName());
        info.addProperty("path", relativeTo(file, rootPath));
        info.addProperty("time", Formatters.LOCAL_DATETIME.format(Instant.ofEpochMilli(file.lastModified()).atZone(ZoneId.systemDefault())));
        info.addProperty("dir", file.isDirectory() ? 1 : 0);
        return info;
    }

    private Response getFile(Map<String, String> headers, File file, String mime) throws IOException {
        long fileLen = file.length();
        String etag = etag(file, fileLen);
        String ifNoneMatch = headers.get("if-none-match");
        if (ifNoneMatch != null && (ifNoneMatch.equals("*") || ifNoneMatch.equals(etag))) return newFixedLengthResponse(Status.NOT_MODIFIED, mime, "");
        HttpRange range = HttpRange.from(fileLen, headers, etag);
        if (!range.valid()) return createRangeNotSatisfiableResponse(fileLen);
        return createFileResponse(file, mime, fileLen, etag, range);
    }

    private Response createFileResponse(File file, String mime, long fileLen, String etag, HttpRange range) throws IOException {
        FileInputStream input = new FileInputStream(file);
        input.getChannel().position(range.start);
        Status status = range.requested() ? Status.PARTIAL_CONTENT : Status.OK;
        Response response = newFixedLengthResponse(status, mime, input, range.length);
        if (range.requested()) response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileLen);
        response.addHeader("Content-Length", String.valueOf(range.length));
        response.addHeader("Accept-Ranges", "bytes");
        response.addHeader("ETag", etag);
        return response;
    }

    private String etag(File file, long fileLen) {
        CRC32 crc = new CRC32();
        crc.update((file.getAbsolutePath() + file.lastModified() + fileLen).getBytes());
        return Long.toHexString(crc.getValue());
    }

    private Response createRangeNotSatisfiableResponse(long fileLen) {
        Response response = newFixedLengthResponse(Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
        response.addHeader("Content-Range", "bytes */" + fileLen);
        return response;
    }

    private static String requirePath(Map<String, String> params) {
        String path = params.get("path");
        if (path == null) throw new IllegalArgumentException("Missing path");
        return path;
    }

    private static String requireName(String name) {
        if (name == null || name.isEmpty() || name.equals(".") || name.equals("..") || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid file name");
        return name;
    }

    private static File resolvePath(String path) throws IOException {
        return resolve(new File(Path.root(), relativePath(path)));
    }

    private static File resolveFile(IHTTPSession session, String path) throws IOException {
        File file = Path.local(path).getCanonicalFile();
        if (isLoopback(session) || isWithin(file, Path.root())) return file;
        throw new SecurityException("Path outside shared storage");
    }

    private static File resolveChild(File directory, String name) throws IOException {
        return resolve(new File(directory, name));
    }

    private static File resolveDeletePath(String path) throws IOException {
        File root = Path.root().getCanonicalFile();
        File target = new File(root, relativePath(path)).getAbsoluteFile();
        if (resolve(target).equals(root)) throw new SecurityException("Storage root cannot be deleted");
        return target;
    }

    private static String relativePath(String path) {
        while (path.startsWith(File.separator)) path = path.substring(1);
        return path;
    }

    private static File resolve(File file) throws IOException {
        File target = file.getCanonicalFile();
        if (!isWithin(target, Path.root())) throw new SecurityException("Path outside storage root");
        return target;
    }

    private static boolean isWithin(File file, File directory) throws IOException {
        return file.toPath().startsWith(directory.getCanonicalFile().toPath());
    }

    private static void deleteRecursively(File file) throws IOException {
        boolean symbolicLink = !file.getCanonicalFile().equals(file.getAbsoluteFile());
        if (file.isDirectory() && !symbolicLink) {
            File[] children = file.listFiles();
            if (children == null) throw new IOException("Unable to list directory");
            for (File child : children) deleteRecursively(child);
        }
        if (!file.delete() && file.exists()) throw new IOException("Unable to delete file");
    }

    private static boolean isLoopback(IHTTPSession session) {
        String address = session.getRemoteIpAddress();
        return address != null && (address.startsWith("127.") || address.equals("::1") || address.equals("0:0:0:0:0:0:0:1"));
    }

    private static String relativeTo(File file, String rootPath) {
        String path = file.getAbsolutePath();
        return path.startsWith(rootPath) ? path.substring(rootPath.length()) : path;
    }

    private static String parentOf(File dir, File rootDir, String rootPath) {
        if (dir.equals(rootDir)) return ".";
        File parent = dir.getParentFile();
        if (parent == null || parent.equals(rootDir)) return "";
        return relativeTo(parent, rootPath);
    }

    private record HttpRange(long start, long end, long length, boolean valid, boolean requested) {

        public static HttpRange invalid() {
            return new HttpRange(0, 0, 0, false, false);
        }

        public static HttpRange from(long fileLen, Map<String, String> headers, String etag) {
            String rangeHeader = headers.get("range");
            String ifRange = headers.get("if-range");
            if (ifRange != null && !ifRange.equals(etag)) rangeHeader = null;
            if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) return new HttpRange(0, fileLen - 1, fileLen, true, false);
            String range = rangeHeader.substring(6).trim();
            if (range.contains(",")) return invalid();
            String[] bounds = range.split("-", -1);
            if (bounds.length != 2) return invalid();
            String first = bounds[0].trim();
            String last = bounds[1].trim();
            try {
                if (first.isEmpty()) {
                    long suffix = Long.parseLong(last);
                    if (fileLen <= 0 || suffix <= 0) return invalid();
                    long length = Math.min(suffix, fileLen);
                    return new HttpRange(fileLen - length, fileLen - 1, length, true, true);
                }
                long start = Long.parseLong(first);
                long end = last.isEmpty() ? fileLen - 1 : Long.parseLong(last);
                if (start < 0 || start >= fileLen || end < start) return invalid();
                end = Math.min(end, fileLen - 1);
                return new HttpRange(start, end, end - start + 1, true, true);
            } catch (NumberFormatException e) {
                return invalid();
            }
        }
    }
}
