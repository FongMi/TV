package com.p2p;

import android.content.Context;

import java.io.File;

public class P2PClass {

    public String path;
    public int port;

    public P2PClass(Context context, String lib) {
        System.load(lib);
        String cache = context.getCacheDir().getAbsolutePath();
        File file = new File(path = cache + "/jpali");
        if (!file.exists()) file.mkdirs();
        port = doxstarthttpd("TEST3E63BAAECDAA79BEAA91853490A69F08".getBytes(), cache.getBytes());
    }

    public int P2Pdoxstart(byte[] bArr) {
        return doxstart(bArr);
    }

    public int P2Pdoxdownload(byte[] bArr) {
        return doxdownload(bArr);
    }

    public int P2Pdoxterminate() {
        return doxterminate();
    }

    public int P2Pdosetupload(int i) {
        return dosetupload(i);
    }

    public int P2Pdoxcheck(byte[] bArr) {
        return doxcheck(bArr);
    }

    public int P2Pdoxadd(byte[] bArr) {
        return doxadd(bArr);
    }

    public int P2Pdoxpause(byte[] bArr) {
        return doxpause(bArr);
    }

    public int P2Pdoxdel(byte[] bArr) {
        return doxdel(bArr);
    }

    public int P2PdoxdelAll() {
        return doxdelall();
    }

    public long P2Pgetspeed(int i) {
        return getspeed(i);
    }

    public long P2Pgetdownsize(int i) {
        return getdownsize(i);
    }

    public long P2Pgetfilesize(int i) {
        return getfilesize(i);
    }

    public int P2Pgetpercent() {
        return getpercent();
    }

    public long P2Pgetlocalfilesize(byte[] bArr) {
        return getlocalfilesize(bArr);
    }

    public long P2Pdosetduration(int i) {
        return doxsetduration(i);
    }

    public String getServiceAddress() {
        return doxgethostbynamehook("xx0.github.com");
    }

    public int P2Pdoxstarthttpd(byte[] bArr, byte[] bArr2) {
        return doxstarthttpd(bArr, bArr2);
    }

    public int P2Pdoxsave() {
        return doxsave();
    }

    public int P2Pdoxendhttpd() {
        return doxendhttpd();
    }

    public String getVersion() {
        return doxgetVersion();
    }

    public long xGFilmOpenFile(byte[] bArr) {
        return XGFilmOpenFile(bArr);
    }

    public void xGFilmCloseFile(long j) {
        XGFilmCloseFile(j);
    }

    public int xGFilmReadFile(long j, long j2, int i, byte[] bArr) {
        return XGFilmReadFile(j, j2, i, bArr);
    }

    public void setP2PPauseUpdate(int i) {
        doxSetP2PPauseUpdate(i);
    }

    public String getTouPingUrl() {
        return doxgetlocalAddress();
    }

    public String P2Pdoxgettaskstat(int i) {
        return doxgettaskstat(i);
    }

    private native void XGFilmCloseFile(long j);

    private native long XGFilmOpenFile(byte[] bArr);

    private native int XGFilmReadFile(long j, long j2, int i, byte[] bArr);

    private native int dosetupload(int i);

    private native void doxSetP2PPauseUpdate(int i);

    private native int doxadd(byte[] bArr);

    private native int doxcheck(byte[] bArr);

    private native int doxdel(byte[] bArr);

    private native int doxdelall();

    private native int doxdownload(byte[] bArr);

    private native int doxendhttpd();

    private native String doxgetVersion();

    private native String doxgethostbynamehook(String str);

    private native String doxgetlocalAddress();

    private native String doxgettaskstat(int i);

    private native int doxpause(byte[] bArr);

    private native int doxsave();

    private native int doxsetduration(int i);

    private native int doxstart(byte[] bArr);

    private native int doxstarthttpd(byte[] bArr, byte[] bArr2);

    private native int doxterminate();

    private native long getdownsize(int i);

    private native long getfilesize(int i);

    private native long getlocalfilesize(byte[] bArr);

    private native int getpercent();

    private native long getspeed(int i);
}
