package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Root(name = "tv", strict = false)
public class Tv {

    @Attribute(name = "date", required = false)
    private String date;

    @ElementList(entry = "channel", required = false, inline = true)
    private List<Channel> channel;

    @ElementList(entry = "programme", required = false, inline = true)
    private List<Programme> programme;

    public String getDate() {
        return TextUtils.isEmpty(date) ? "" : date;
    }

    public List<Channel> getChannel() {
        return channel == null ? Collections.emptyList() : channel;
    }

    public List<Programme> getProgramme() {
        return programme == null ? Collections.emptyList() : programme;
    }

    @Root(name = "channel")
    public static class Channel {

        @Attribute(name = "id", required = false)
        private String id;

        @Element(name = "icon", required = false)
        private Icon icon;

        @ElementList(entry = "display-name", required = false, inline = true)
        private List<DisplayName> displayName;

        public String getId() {
            return TextUtils.isEmpty(id) ? "" : id;
        }

        private Icon getIcon() {
            return icon == null ? new Icon() : icon;
        }

        public List<DisplayName> getDisplayName() {
            return displayName == null ? new ArrayList<>() : displayName;
        }

        public String getSrc() {
            return getIcon().getSrc();
        }

        public boolean hasSrc() {
            return !getIcon().getSrc().isEmpty();
        }
    }

    @Root(name = "programme")
    public static class Programme {

        @Attribute(name = "start", required = false)
        private String start;

        @Attribute(name = "stop", required = false)
        private String stop;

        @Attribute(name = "channel", required = false)
        private String channel;

        @Element(name = "title", required = false)
        private String title;

        public String getStart() {
            return TextUtils.isEmpty(start) ? "" : start;
        }

        public String getStop() {
            return TextUtils.isEmpty(stop) ? "" : stop;
        }

        public String getChannel() {
            return TextUtils.isEmpty(channel) ? "" : channel;
        }

        public String getTitle() {
            return TextUtils.isEmpty(title) ? "" : title;
        }
    }

    @Root(name = "icon")
    public static class Icon {

        @Attribute(name = "src")
        private String src;

        public String getSrc() {
            return TextUtils.isEmpty(src) ? "" : src;
        }
    }

    @Root(name = "display-name")
    public static class DisplayName {

        @Text
        private String text;

        public String getText() {
            return TextUtils.isEmpty(text) ? "" : text;
        }
    }
}
