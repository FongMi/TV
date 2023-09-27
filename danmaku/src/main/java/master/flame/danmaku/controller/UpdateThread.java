package master.flame.danmaku.controller;

public class UpdateThread extends Thread {

    volatile boolean mIsQuited;

    public UpdateThread(String name) {
        super(name);
    }

    public void quit() {
        mIsQuited = true;
    }

    public boolean isQuited() {
        return mIsQuited;
    }

    @Override
    public void run() {
        if (mIsQuited) return;
    }
}
