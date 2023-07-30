package tv.danmaku.ijk.media.player;

public final class IjkTimedText {

    private String text;
    private int[] bitmap;

    public static IjkTimedText create(String text) {
        return new IjkTimedText(text);
    }

    public static IjkTimedText create(int[] bitmap) {
        return new IjkTimedText(bitmap);
    }

    public IjkTimedText(String text) {
        this.text = text;
    }

    public IjkTimedText(int[] bitmap) {
        this.bitmap = bitmap;
    }

    public String getText() {
        return text;
    }

    public int[] getBitmap() {
        return bitmap;
    }
}
