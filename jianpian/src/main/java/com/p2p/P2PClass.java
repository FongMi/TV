package com.p2p;

import android.content.Context;

import java.io.File;

public class P2PClass {

    public int port;

    public P2PClass(Context context, String lib) {
        System.load(lib);
        init(context);
    }

    private void init(Context context) {
        try {
            String path = context.getCacheDir().getAbsolutePath();
            File file = new File(path + "/jpali");
            if (!file.exists()) file.mkdirs();
            port = P2Pdoxstarthttpd("TEST3E63BAAECDAA79BEAA91853490A69F08".getBytes(), path.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void P2Pdoxstart(byte[] bArr) {
        doxstart(bArr);
    }

    public void P2Pdoxadd(byte[] bArr) {
        doxadd(bArr);
    }

    public void P2Pdoxpause(byte[] bArr) {
        doxpause(bArr);
    }

    public void P2Pdoxdel(byte[] bArr) {
        doxdel(bArr);
    }

    public int P2Pdoxstarthttpd(byte[] bArr, byte[] bArr2) {
        return doxstarthttpd(bArr, bArr2);
    }

    public int P2Pdoxendhttpd() {
        return doxendhttpd();
    }

    private native int doxadd(byte[] bArr);

    private native int doxdel(byte[] bArr);

    private native int doxendhttpd();

    private native int doxpause(byte[] bArr);

    private native int doxstart(byte[] bArr);

    private native int doxstarthttpd(byte[] bArr, byte[] bArr2);
}
