package com.p2p;

import com.github.catvod.utils.Path;

public class P2PClass {

    public int port;

    public P2PClass() {
        System.loadLibrary("jpa");
        this.port = P2Pdoxstarthttpd("TEST3E63BAAECDAA79BEAA91853490A69F08".getBytes(), Path.jpa().getAbsolutePath().getBytes());
    }

    public int P2Pdoxstarthttpd(byte[] bArr, byte[] bArr2) {
        return doxstarthttpd(bArr, bArr2);
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

    private native int doxstarthttpd(byte[] bArr, byte[] bArr2);

    private native int doxstart(byte[] bArr);

    private native int doxadd(byte[] bArr);

    private native int doxpause(byte[] bArr);

    private native int doxdel(byte[] bArr);
}
