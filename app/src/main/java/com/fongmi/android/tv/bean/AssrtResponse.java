package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.gson.AssrtListAdapter;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public final class AssrtResponse {

    @SerializedName("status")
    private int status;
    @SerializedName(value = "errmsg", alternate = "message")
    private String error;
    @SerializedName("sub")
    private Payload payload;

    public static AssrtResponse from(String text) {
        AssrtResponse response = App.gson().fromJson(text, AssrtResponse.class);
        return response == null ? new AssrtResponse() : response;
    }

    public String getError() {
        if (status == 0) return "";
        return TextUtils.isEmpty(error) ? String.valueOf(status) : error.trim();
    }

    public List<Subtitle> getSubtitles() {
        return payload == null ? Collections.emptyList() : payload.getSubtitles();
    }

    private static final class Payload {

        @SerializedName("subs")
        @JsonAdapter(AssrtListAdapter.class)
        private List<Subtitle> subtitles;

        private List<Subtitle> getSubtitles() {
            return subtitles == null ? Collections.emptyList() : subtitles;
        }
    }

    public static final class Subtitle {

        @SerializedName("id")
        private int id;
        @SerializedName("native_name")
        private String nativeName;
        @SerializedName("title")
        private String title;
        @SerializedName("filename")
        private String fileName;
        @SerializedName("videoname")
        private String videoName;
        @SerializedName("url")
        private String url;
        @SerializedName("lang")
        private JsonElement language;
        @SerializedName("filelist")
        @JsonAdapter(AssrtListAdapter.class)
        private List<SubtitleFile> files;

        public int getId() {
            return id;
        }

        public String getNativeName() {
            return clean(nativeName);
        }

        public String getTitle() {
            return clean(title);
        }

        public String getFileName() {
            return clean(fileName);
        }

        public String getVideoName() {
            return clean(videoName);
        }

        public String getUrl() {
            return clean(url);
        }

        public String getLanguage() {
            try {
                if (language == null || language.isJsonNull()) return "";
                if (language.isJsonPrimitive()) return clean(language.getAsString());
                JsonElement desc = language.getAsJsonObject().get("desc");
                return desc == null || desc.isJsonNull() ? "" : clean(desc.getAsString());
            } catch (Exception e) {
                return "";
            }
        }

        public boolean hasFileList() {
            return files != null;
        }

        public List<SubtitleFile> getFiles() {
            return files == null ? Collections.emptyList() : files;
        }
    }

    public static final class SubtitleFile {

        @SerializedName(value = "f", alternate = {"filename", "name"})
        private String name;
        @SerializedName("url")
        private String url;

        public String getName() {
            return clean(name);
        }

        public String getUrl() {
            return clean(url);
        }
    }

    private static String clean(String text) {
        return TextUtils.isEmpty(text) ? "" : text.trim();
    }
}
