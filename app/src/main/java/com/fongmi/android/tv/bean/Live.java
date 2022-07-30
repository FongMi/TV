package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Live {

    @SerializedName("group")
    private String group;
    @SerializedName("channels")
    private List<Channels> channels;

    public String getGroup() {
        return group;
    }

    public List<Channels> getChannels() {
        return channels;
    }

    public static class Channels {

        @SerializedName("name")
        private String name;
        @SerializedName("urls")
        private List<String> urls;

        public String getName() {
            return name;
        }

        public List<String> getUrls() {
            return urls;
        }
    }
}
