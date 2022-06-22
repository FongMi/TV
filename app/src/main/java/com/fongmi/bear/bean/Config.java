package com.fongmi.bear.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Config {

    @SerializedName("sites")
    private List<Sites> sites;
    @SerializedName("lives")
    private List<Lives> lives;
    @SerializedName("parses")
    private List<Parses> parses;
    @SerializedName("flags")
    private List<String> flags;
    @SerializedName("spider")
    private String spider;

    public static Config objectFrom(String str) {
        return new Gson().fromJson(str, Config.class);
    }

    public List<Sites> getSites() {
        return sites;
    }

    public List<Lives> getLives() {
        return lives;
    }

    public List<Parses> getParses() {
        return parses;
    }

    public List<String> getFlags() {
        return flags;
    }

    public String getSpider() {
        return spider;
    }

    public static class Sites {

        @SerializedName("key")
        private String key;
        @SerializedName("name")
        private String name;
        @SerializedName("type")
        private Integer type;
        @SerializedName("api")
        private String api;
        @SerializedName("searchable")
        private Integer searchable;
        @SerializedName("quickSearch")
        private Integer quickSearch;
        @SerializedName("filterable")
        private Integer filterable;
        @SerializedName("ext")
        private String ext;

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public Integer getType() {
            return type;
        }

        public String getApi() {
            return api;
        }

        public Integer getSearchable() {
            return searchable;
        }

        public Integer getQuickSearch() {
            return quickSearch;
        }

        public Integer getFilterable() {
            return filterable;
        }

        public String getExt() {
            return ext;
        }
    }

    public static class Lives {

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

    public static class Parses {

        @SerializedName("name")
        private String name;
        @SerializedName("type")
        private Integer type;
        @SerializedName("url")
        private String url;

        public String getName() {
            return name;
        }

        public Integer getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }
    }
}
