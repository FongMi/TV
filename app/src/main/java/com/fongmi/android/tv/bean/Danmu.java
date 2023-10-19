package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.core.Persister;

import java.util.Collections;
import java.util.List;

@Root(name = "i", strict = false)
public class Danmu {

    @ElementList(entry = "d", required = false, inline = true)
    private List<Data> data;

    public static Danmu fromXml(String str) {
        try {
            return new Persister().read(Danmu.class, str);
        } catch (Exception e) {
            e.printStackTrace();
            return new Danmu();
        }
    }

    public List<Data> getData() {
        return data == null ? Collections.emptyList() : data;
    }

    public static class Data {

        @Attribute(name = "p", required = false)
        public String param;

        @Text(required = false)
        public String text;

        public String getParam() {
            return TextUtils.isEmpty(param) ? "" : param;
        }

        public String getText() {
            return TextUtils.isEmpty(text) ? "" : text;
        }
    }
}
