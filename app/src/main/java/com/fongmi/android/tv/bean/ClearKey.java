package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
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

    public static ClearKey get(String kid, String k) {
        ClearKey key = new ClearKey();
        key.keys = Arrays.asList(new Keys(kid, k));
        key.type = "temporary";
        return key;
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
