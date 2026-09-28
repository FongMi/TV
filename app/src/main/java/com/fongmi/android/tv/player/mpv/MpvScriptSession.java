package com.fongmi.android.tv.player.mpv;

/** Script status representation. Running scripts needs private Media3 APIs. */
public final class MpvScriptSession {

    private MpvScriptSession() {
    }

    public enum State { RUNNING, DONE, ERROR, TIMEOUT, LOADING, LOADED }

    public record Status(State state, String error) {
    }
}
