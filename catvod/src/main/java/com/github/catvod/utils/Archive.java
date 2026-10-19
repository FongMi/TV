package com.github.catvod.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Archive {

    private static final int COPY_BUFFER_SIZE = 64 * 1024;

    private static File createTempFile(File target) throws IOException {
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent == null) throw new IOException("Target has no parent directory");
        if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Unable to create target directory");
        return File.createTempFile("copy-", ".tmp", parent);
    }

    private static void transfer(InputStream input, OutputStream output, long maxBytes) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        long totalBytes = 0L;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Canceled");
            totalBytes += count;
            if (totalBytes > maxBytes) throw new IOException("File is too large");
            output.write(buffer, 0, count);
        }
    }

    public static boolean gzipCompress(byte[] data, File target) {
        File temp = null;
        try {
            temp = createTempFile(target);
            writeGzip(data, temp);
            Path.move(temp, target);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            Path.clear(temp);
        }
    }

    private static void writeGzip(byte[] data, File target) throws IOException {
        try (FileOutputStream fileOutput = new FileOutputStream(target); GZIPOutputStream output = new GZIPOutputStream(fileOutput)) {
            output.write(data);
            output.finish();
            fileOutput.getFD().sync();
        }
    }

    public static boolean gzipDecompress(File source, File target) {
        File temp = null;
        try {
            temp = createTempFile(target);
            expandGzip(source, temp);
            Path.move(temp, target);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            Path.clear(temp);
        }
    }

    private static void expandGzip(File source, File target) throws IOException {
        try (GZIPInputStream input = new GZIPInputStream(new BufferedInputStream(new FileInputStream(source)));
             FileOutputStream fileOutput = new FileOutputStream(target);
             BufferedOutputStream output = new BufferedOutputStream(fileOutput)) {
            transfer(input, output, Long.MAX_VALUE);
            output.flush();
            fileOutput.getFD().sync();
        }
    }

    public static String readGzip(File file) {
        try (GZIPInputStream is = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return Path.read(is);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean zipDecompress(File target, File path) {
        return zipDecompress(target, path, Integer.MAX_VALUE, Long.MAX_VALUE);
    }

    public static boolean zipDecompress(File target, File path, int maxEntries, long maxBytes) {
        try (ZipFile zip = new ZipFile(target)) {
            requireArchiveSpace(zip, path, maxBytes);
            extractEntries(zip, path, maxEntries, maxBytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void requireArchiveSpace(ZipFile zip, File path, long maxBytes) throws IOException {
        long required = 0;
        Enumeration<? extends ZipEntry> sizes = zip.entries();
        while (sizes.hasMoreElements()) {
            long size = sizes.nextElement().getSize();
            if (size > 0) required = Math.addExact(required, size);
        }
        if (required > maxBytes) throw new IOException("Archive size limit exceeded");
        Path.requireSpace(path, required);
    }

    private static void extractEntries(ZipFile zip, File path, int maxEntries, long maxBytes) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        String root = path.getCanonicalPath() + File.separator;
        byte[] buffer = new byte[16384];
        long totalBytes = 0;
        int totalEntries = 0;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (++totalEntries > maxEntries) throw new IOException("Archive entry limit exceeded");
            File out = new File(path, entry.getName());
            if (!out.getCanonicalPath().startsWith(root)) continue;
            if (entry.isDirectory()) out.mkdirs();
            else totalBytes = extractFile(zip, entry, out, buffer, totalBytes, maxBytes);
        }
    }

    private static long extractFile(ZipFile zip, ZipEntry entry, File out, byte[] buffer, long totalBytes, long maxBytes) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
             BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(Path.create(out)))) {
            int read;
            while ((read = is.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > maxBytes) throw new IOException("Archive size limit exceeded");
                os.write(buffer, 0, read);
            }
        }
        return totalBytes;
    }
}
