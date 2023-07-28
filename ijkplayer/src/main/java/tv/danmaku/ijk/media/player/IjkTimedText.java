package tv.danmaku.ijk.media.player;

public final class IjkTimedText {

    private final String text;

    public static IjkTimedText create(String text) {
        return new IjkTimedText(text);
    }

    public IjkTimedText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
