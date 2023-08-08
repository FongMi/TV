package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.Utils;
import com.google.gson.annotations.SerializedName;

public class Core {

    @SerializedName("auth")
    private String auth;
    @SerializedName("name")
    private String name;
    @SerializedName("pass")
    private String pass;
    @SerializedName("broker")
    private String broker;
    @SerializedName("resp")
    private String resp;
    @SerializedName("sign")
    private String sign;
    @SerializedName("so")
    private String so;

    public String getAuth() {
        return TextUtils.isEmpty(auth) ? "" : Utils.convert(auth);
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getPass() {
        return TextUtils.isEmpty(pass) ? "" : pass;
    }

    public String getBroker() {
        return TextUtils.isEmpty(broker) ? "" : broker;
    }

    public String getResp() {
        return TextUtils.isEmpty(resp) ? "" : resp;
    }

    public String getSign() {
        return !TextUtils.isEmpty(sign) ? sign : "3082034f30820237a003020102020447424f83300d06092a864886f70d01010b05003057310b300906035504061302434e310b300906035504081302484b310b300906035504071302484b3110300e060355040a130779737874656368310f300d060355040b1306686b2d797378310b300906035504031302686b3020170d3139303530363032303932395a180f32303734303230363032303932395a3057310b300906035504061302434e310b300906035504081302484b310b300906035504071302484b3110300e060355040a130779737874656368310f300d060355040b1306686b2d797378310b300906035504031302686b30820122300d06092a864886f70d01010105000382010f003082010a0282010100a0a51a8c28632cae9fea86712c054b3aa2a4d1051375dfe371d39db3df880b66c4f689c8cf47c511e53ae85fb20b3f8d2ce384ddf293a752ab2835b54c7d0edc24b6726f1224bd0d75cc5c63e48a8cb0272f64f92e50ca5526f8cadf2299508e3f029eb4dabacf2888837bf4d23352bb781990999b9f6c09a5762db6cf636203e7643fe06fdebd0650d8eca53af9942946e708edfd519fe940d3b83d8a1e8b11a168b1bd9b29c9d06a0cb7756933b5f8e7ebefc7c107904861b1499dd45d2d308ae7f0529accbfc6b4f0c6f7cfbf9f94a7cb95c4821fd32e1094d974d76f1edd37ce059e93474e5436fedca42c9754bfc8ade524f0fefd588e22c6888b7cfb530203010001a321301f301d0603551d0e041604142947601345e322f12e3d52611f2f55f6b50bd75d300d06092a864886f70d01010b05000382010100844fe39a6e017b6968817e0e94d34068ecb9d00c11b4585a9581c2b9dfc559a2d8ca1a998c49dab292a7c99e655fe64d66a6a879823c7bcb77275228db4fc1c2d685ce6e9fcfc5cbb4b2253866e4b4191cf03527d8ecc3f58d4ca463dc23b0ec88a6e2054a1b898a6a5a5425b03e59b1663b43a2f61690d5c1aa92a5d7e8f87cd6eddaa76c709075b12c5d460b8750cecde7e9bee10954103f437b182f5f242fe0d5337e52957d2502c31a65631fdd3863f0b5c4f348718b33dfd00b90c743c3fea97c715a3a05212921bba50daebf71ce7997aa599eab69570294b38571b3543e5c2af85a402fa3274659dec133bb4b593ff55c14fd8234039cbcc2394dd0a7";
    }

    public String getSo() {
        return !TextUtils.isEmpty(so) ? so : "tvcore";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Core)) return false;
        Core it = (Core) obj;
        return getSo().equals(it.getSo());
    }
}
