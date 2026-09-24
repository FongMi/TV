package com.fongmi.android.tv.player.engine;

import androidx.annotation.Nullable;
import androidx.media3.exoplayer.iso.IsoNavigationSession;
import java.util.function.Consumer;

/** Player-independent controls for an authored disc menu. */
public interface DiscMenuController {

    enum OpenResult { OPENED, UNAVAILABLE, TIMED_OUT, CANCELLED }

    boolean hasMenu();

    boolean isActive();

    default int getMenuDomain() {
        return IsoNavigationSession.MENU_DOMAIN_UNKNOWN;
    }

    boolean isNavigationPlayback();

    default boolean acceptsNavigationKeys() {
        return isActive();
    }

    boolean sendAction(String action);

    void observeOpen(String action, Consumer<OpenResult> callback);

    default boolean hasExternalGraphics() {
        return false;
    }

    default boolean supportsPointer() {
        return false;
    }

    default boolean sendPointer(float x, float y, boolean activate) {
        return false;
    }

    @Nullable
    default IsoNavigationSession.MenuOverlay getHdmvOverlay(int previousVersion) {
        return null;
    }

    @Nullable
    default IsoNavigationSession.MenuHighlight getDvdHighlight() {
        return null;
    }
}
