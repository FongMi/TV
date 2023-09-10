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
    private List<D> d;

    public static Danmu fromXml(String str) {
        try {
            return new Persister().read(Danmu.class, str);
        } catch (Exception e) {
            return new Danmu();
        }
    }

    public List<D> getD() {
        return d == null ? Collections.emptyList() : d;
    }

    public static class D {

        @Attribute(name = "p", required = false)
        public String p;

        @Text
        public String t;

        public String getP() {
            return TextUtils.isEmpty(p) ? "" : p;
        }

        public String getT() {
            return TextUtils.isEmpty(t) ? "" : t;
        }
    }
}
