package com.fongmi.android.tv.server.process;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.github.catvod.runtime.OcrRuntime;
import com.github.catvod.runtime.OcrRuntime.Model;
import com.github.catvod.utils.RuntimeModule;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public final class Ocr {

    private static final String STATUS_PATH = "/ocr/status";
    private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;
    private static final long MAX_BITMAP_PIXELS = 8_294_400;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/bmp");
    private static final String AUTO = "auto";
    private static final Map<String, Model> MODELS = Map.of("latin", Model.LATIN, "chinese", Model.CHINESE, "japanese", Model.JAPANESE);
    private static final Pattern LOCAL_HOST = Pattern.compile("(?:127\\.0\\.0\\.1|localhost|\\[::1\\])(?::[0-9]{1,5})?", Pattern.CASE_INSENSITIVE);
    private static final Semaphore RECOGNITION_SLOT = new Semaphore(1);

    public static boolean isRequest(String url) {
        return url.equals("/ocr") || url.equals(STATUS_PATH);
    }

    public Response serve(IHTTPSession session, String url) {
        Map<String, String> headers = session.getHeaders();
        if (!isLoopback(session, headers.get("host"))) return error(Status.FORBIDDEN, "loopback_required");
        if (url.equals(STATUS_PATH)) return getStatus(session.getMethod());
        return postImage(session, headers);
    }

    private boolean isLoopback(IHTTPSession session, String host) {
        return "127.0.0.1".equals(session.getRemoteIpAddress()) && host != null && LOCAL_HOST.matcher(host).matches();
    }

    private Response getStatus(Method method) {
        if (method != Method.GET) return methodNotAllowed(Method.GET);
        JsonObject body = new JsonObject();
        body.addProperty("installed", RuntimeModule.OCR.isInstalled());
        return response(Status.OK, body);
    }

    private Response postImage(IHTTPSession session, Map<String, String> headers) {
        if (session.getMethod() != Method.POST) return methodNotAllowed(Method.POST);
        String mimeType = headers.getOrDefault("content-type", "").split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(mimeType)) return error(Status.UNSUPPORTED_MEDIA_TYPE, "unsupported_image_type");
        String model = getModel(session.getParameters().get("model"));
        if (model == null) return error(Status.BAD_REQUEST, "invalid_model");
        if (headers.containsKey("expect")) return error(Status.EXPECTATION_FAILED, "expectation_unsupported");
        String contentLength = headers.get("content-length");
        if (headers.containsKey("transfer-encoding") || contentLength == null) return error(Status.LENGTH_REQUIRED, "content_length_required");
        long length;
        try {
            length = Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            return error(Status.BAD_REQUEST, "invalid_content_length");
        }
        if (length <= 0) return error(Status.BAD_REQUEST, "invalid_content_length");
        if (length > MAX_IMAGE_BYTES) return error(Status.PAYLOAD_TOO_LARGE, "image_too_large");
        return recognizeUpload(session, (int) length, mimeType, model);
    }

    private String getModel(List<String> values) {
        if (values == null) return "latin";
        if (values.size() != 1) return null;
        String model = values.get(0);
        return model.equals(AUTO) || MODELS.containsKey(model) ? model : null;
    }

    private Response recognizeUpload(IHTTPSession session, int length, String mimeType, String model) {
        if (!RECOGNITION_SLOT.tryAcquire()) return error(Status.TOO_MANY_REQUESTS, "ocr_busy");
        try {
            byte[] image = new byte[length];
            new DataInputStream(session.getInputStream()).readFully(image);
            return recognizeImage(image, mimeType, model);
        } catch (SocketTimeoutException e) {
            return error(Status.REQUEST_TIMEOUT, "image_read_timeout");
        } catch (IOException e) {
            return error(Status.BAD_REQUEST, "image_read_failed");
        } finally {
            RECOGNITION_SLOT.release();
        }
    }

    private Response recognizeImage(byte[] image, String mimeType, String model) {
        Bitmap bitmap = decodeImage(image, mimeType);
        if (bitmap == null) return error(Status.BAD_REQUEST, "invalid_image");
        try {
            return recognizeText(bitmap, model);
        } finally {
            bitmap.recycle();
        }
    }

    private Bitmap decodeImage(byte[] image, String mimeType) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(image, 0, image.length, options);
        if (!mimeType.equals(options.outMimeType) || options.outWidth <= 0 || options.outHeight <= 0) return null;
        options.inJustDecodeBounds = false;
        options.inSampleSize = getSampleSize(options.outWidth, options.outHeight);
        return BitmapFactory.decodeByteArray(image, 0, image.length, options);
    }

    private int getSampleSize(int width, int height) {
        int sample = 1;
        while ((long) width * height > MAX_BITMAP_PIXELS) {
            width = (int) ((width + 1L) / 2);
            height = (int) ((height + 1L) / 2);
            sample *= 2;
        }
        return sample;
    }

    private Response recognizeText(Bitmap bitmap, String model) {
        RuntimeModule module = RuntimeModule.OCR;
        OcrRuntime runtime;
        try {
            runtime = module.entry(OcrRuntime.class, module.requireRevision());
        } catch (RuntimeModule.MissingException e) {
            return error(Status.SERVICE_UNAVAILABLE, "ocr_module_missing");
        } catch (RuntimeModule.RestartRequiredException e) {
            return error(Status.SERVICE_UNAVAILABLE, "ocr_restart_required");
        } catch (IOException | IllegalStateException | LinkageError e) {
            Log.e("Ocr", "Unable to load OCR module", e);
            return error(Status.SERVICE_UNAVAILABLE, "ocr_module_unavailable");
        }
        try {
            Recognition result = model.equals(AUTO) ? recognizeAutomatically(runtime, bitmap) : recognizeModel(runtime, bitmap, MODELS.get(model));
            return textResponse(result);
        } catch (IOException | RuntimeException | LinkageError e) {
            Log.e("Ocr", "Text recognition failed", e);
            return error(Status.INTERNAL_ERROR, "ocr_failed");
        }
    }

    private Recognition recognizeAutomatically(OcrRuntime runtime, Bitmap bitmap) throws IOException {
        Recognition japanese = recognizeModel(runtime, bitmap, Model.JAPANESE);
        if (japanese.text().codePoints().anyMatch(this::isKana)) return japanese;
        Recognition chinese = recognizeModel(runtime, bitmap, Model.CHINESE);
        return chinese.text().isEmpty() ? japanese : chinese;
    }

    private boolean isKana(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return Character.isLetter(codePoint) && (script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA);
    }

    private Recognition recognizeModel(OcrRuntime runtime, Bitmap bitmap, Model model) throws IOException {
        try (OcrRuntime.Session recognizer = runtime.open(model)) {
            return new Recognition(model, recognizer.recognize(bitmap));
        }
    }

    private Response textResponse(Recognition result) {
        JsonObject body = new JsonObject();
        body.addProperty("model", result.model().name().toLowerCase(Locale.ROOT));
        body.addProperty("text", result.text());
        return response(Status.OK, body);
    }

    private Response methodNotAllowed(Method method) {
        Response response = error(Status.METHOD_NOT_ALLOWED, method.name().toLowerCase(Locale.ROOT) + "_required");
        response.addHeader("Allow", method.name());
        return response;
    }

    private Response error(Status status, String code) {
        JsonObject body = new JsonObject();
        body.addProperty("error", code);
        Response response = response(status, body);
        response.closeConnection(true);
        return response;
    }

    private Response response(Status status, JsonObject body) {
        Response response = newFixedLengthResponse(status, "application/json; charset=utf-8", body.toString());
        response.addHeader("Cache-Control", "no-store");
        return response;
    }

    private record Recognition(Model model, String text) {}
}
