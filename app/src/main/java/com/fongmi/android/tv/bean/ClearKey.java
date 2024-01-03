package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.github.catvod.utils.Util;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ClearKey {

    @SerializedName("keys")
    private List<Keys> keys;
    @SerializedName("type")
    private String type;

    public static ClearKey objectFrom(String str) throws Exception {
        ClearKey item = App.gson().fromJson(str, ClearKey.class);
        if (item.keys == null) throw new Exception();
        return item;
    }

    public static ClearKey get(String line) {
        ClearKey item = new ClearKey();
        item.keys = new ArrayList<>();
        item.type = "temporary";
        item.addKeys(line);
        return item;
    }

    private void addKeys(String line) {
        for (String s : line.split(",")) {
            String[] a = s.split(":");
            String kid = Util.base64(Util.hex2byte(a[0].trim())).replace("=", "");
            String k = Util.base64(Util.hex2byte(a[1].trim())).replace("=", "");
            keys.add(new Keys(kid, k));
        }
    }

    public static class Keys {

        @SerializedName("kty")
        private String kty;
        @SerializedName("k")
        private String k;
        @SerializedName("kid")
        private String kid;

        public Keys(String kid, String k) {
            this.kty = "oct";
            this.kid = kid;
            this.k = k;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }
}
