package com.fongmi.android.tv.api;

import android.text.TextUtils;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.AssrtResponse;
import com.fongmi.android.tv.bean.SubtitleSearchItem;
import com.fongmi.android.tv.bean.SubtitleSearchPage;
import com.fongmi.android.tv.impl.ApiCallback;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.SubtitleArchive;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Trans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;

public class SubtitleApi {

    public static final int SEARCH_COUNT = 15;

    private static final String TAG = SubtitleApi.class.getSimpleName();
    private static final String SEARCH_URL = "https://api.assrt.net/v1/sub/search";
    private static final String DETAIL_URL = "https://api.assrt.net/v1/sub/detail";
    private static final AtomicInteger REQUEST_ID = new AtomicInteger();

    private static Download download;

    public static boolean hasToken() {
        return !TextUtils.isEmpty(SubtitleSetting.getEffectiveToken());
    }

    public static void cancel() {
        REQUEST_ID.incrementAndGet();
        OkHttp.cancel(TAG);
        cancelDownload();
    }

    public static void search(String keyword, int pos, Consumer<SubtitleSearchPage> success, Consumer<Exception> error) {
        int requestId = begin();
        newSearchCall(keyword, pos).enqueue(new ApiCallback<>(requestId, REQUEST_ID, response -> parseSearch(read(response)), success, error));
    }

    public static void detail(int id, Consumer<List<SubtitleSearchItem>> success, Consumer<Exception> error) {
        int requestId = begin();
        newDetailCall(id).enqueue(new ApiCallback<>(requestId, REQUEST_ID, response -> parseDetail(read(response)), success, error));
    }

    public static void loadSubtitle(SubtitleSearchItem item, Consumer<List<SubtitleSearchItem>> success, Consumer<Exception> error) {
        int requestId = begin();
        Download task = SubtitleArchive.createDownload(item.getName(), item.getUrl()).tag(TAG);
        download = task;
        task.start(new SubtitleDownloadCallback(task, requestId, item, success, error));
    }

    private static void cancelDownload() {
        Download task = download;
        download = null;
        if (task != null) task.cancel();
    }

    private static int begin() {
        cancel();
        return REQUEST_ID.get();
    }

    private static Call newSearchCall(String keyword, int pos) {
        HttpUrl.Builder builder = urlBuilder(SEARCH_URL);
        builder.addQueryParameter("q", Trans.t2s(keyword));
        builder.addQueryParameter("cnt", String.valueOf(SEARCH_COUNT));
        builder.addQueryParameter("pos", String.valueOf(pos));
        builder.addQueryParameter("filelist", "1");
        return OkHttp.newCall(builder.build().toString(), TAG);
    }

    private static Call newDetailCall(int id) {
        HttpUrl.Builder builder = urlBuilder(DETAIL_URL);
        builder.addQueryParameter("id", String.valueOf(id));
        return OkHttp.newCall(builder.build().toString(), TAG);
    }

    private static HttpUrl.Builder urlBuilder(String url) {
        return Objects.requireNonNull(HttpUrl.parse(url)).newBuilder().addQueryParameter("token", SubtitleSetting.getEffectiveToken());
    }

    private static SubtitleSearchPage parseSearch(String text) throws IOException {
        List<AssrtResponse.Subtitle> subtitles = parse(text).getSubtitles();
        List<SubtitleSearchItem> items = subtitles.stream().map(SubtitleApi::mapSearchItem).filter(Objects::nonNull).toList();
        return SubtitleSearchPage.from(items, subtitles.size());
    }

    private static List<SubtitleSearchItem> parseDetail(String text) throws IOException {
        List<SubtitleSearchItem> items = parse(text).getSubtitles().stream().flatMap(subtitle -> mapDetailItems(subtitle).stream()).toList();
        if (items.isEmpty()) throw new IOException(ResUtil.getString(R.string.error_empty));
        return items;
    }

    private static void unzip(int requestId, SubtitleSearchItem item, File file, Consumer<List<SubtitleSearchItem>> success) {
        List<File> files = SubtitleArchive.unzip(file, SubtitleArchive.getDir(item.getUrl()));
        List<SubtitleSearchItem> items = SubtitleSearchItem.fromFiles(files, item.getLang());
        ApiCallback.post(requestId, REQUEST_ID, () -> success.accept(items));
    }

    private static void completeDownload(int requestId, SubtitleSearchItem item, File file, Consumer<List<SubtitleSearchItem>> success) {
        SubtitleSearchItem local = SubtitleSearchItem.from(item.getId(), item.getName(), file.getAbsolutePath(), item.getLang());
        ApiCallback.post(requestId, REQUEST_ID, () -> success.accept(List.of(local)));
    }

    private static String read(Response response) throws IOException {
        String text = response.body().string();
        if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " " + response.message());
        if (TextUtils.isEmpty(text)) throw new IOException(ResUtil.getString(R.string.error_empty));
        return text;
    }

    private static AssrtResponse parse(String text) throws IOException {
        AssrtResponse response = AssrtResponse.from(text);
        String error = response.getError();
        if (!TextUtils.isEmpty(error)) throw new IOException(error);
        return response;
    }

    private static SubtitleSearchItem mapSearchItem(AssrtResponse.Subtitle subtitle) {
        int id = subtitle.getId();
        if (id <= 0 || !hasSearchFiles(subtitle)) return null;
        String name = first(subtitle.getNativeName(), subtitle.getTitle(), subtitle.getFileName(), subtitle.getVideoName());
        return SubtitleSearchItem.from(id, name, "", subtitle.getLanguage());
    }

    private static List<SubtitleSearchItem> mapDetailItems(AssrtResponse.Subtitle subtitle) {
        List<SubtitleSearchItem> items = new ArrayList<>();
        String name = first(subtitle.getFileName(), subtitle.getNativeName(), subtitle.getTitle(), subtitle.getVideoName());
        for (AssrtResponse.SubtitleFile file : subtitle.getFiles()) addDetailFile(items, subtitle, file, name);
        if (items.isEmpty()) addDetailItem(items, subtitle, name, subtitle.getUrl());
        return items;
    }

    private static void addDetailFile(List<SubtitleSearchItem> items, AssrtResponse.Subtitle subtitle, AssrtResponse.SubtitleFile file, String fallbackName) {
        if (TextUtils.isEmpty(file.getUrl())) return;
        addDetailItem(items, subtitle, first(file.getName(), fallbackName), file.getUrl());
    }

    private static void addDetailItem(List<SubtitleSearchItem> items, AssrtResponse.Subtitle subtitle, String name, String url) {
        if (!SubtitleArchive.isSupported(name, url)) return;
        items.add(SubtitleSearchItem.from(subtitle.getId(), name, url, subtitle.getLanguage()));
    }

    private static boolean hasSearchFiles(AssrtResponse.Subtitle subtitle) {
        return !subtitle.hasFileList() || subtitle.getFiles().stream().anyMatch(file -> SubtitleArchive.isSupported(file.getName(), ""));
    }

    private static String first(String... values) {
        for (String value : values) if (!TextUtils.isEmpty(value)) return value;
        return "";
    }

    private record SubtitleDownloadCallback(Download task, int requestId, SubtitleSearchItem item, Consumer<List<SubtitleSearchItem>> success, Consumer<Exception> error) implements Download.Callback {

        @Override
        public void error(String msg) {
            if (download != task) return;
            download = null;
            ApiCallback.post(requestId, REQUEST_ID, () -> error.accept(new IOException(msg)));
        }

        @Override
        public void success(File file) {
            if (download != task) return;
            download = null;
            if (item.isZip()) Task.execute(() -> unzip(requestId, item, file, success));
            else completeDownload(requestId, item, file, success);
        }
    }
}
