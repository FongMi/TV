package master.flame.danmaku.danmaku.model;

public interface IDrawingCache<T> {

    void build(int w, int h, int density, boolean checkSizeEquals, int bitsPerPixel);

    void erase();

    T get();

    void destroy();

    int size();

    int width();

    int height();

    boolean hasReferences();

    void increaseReference();

    void decreaseReference();
}
