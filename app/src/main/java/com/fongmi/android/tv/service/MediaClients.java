package com.fongmi.android.tv.service;

import androidx.annotation.NonNull;
import androidx.media3.session.MediaSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MediaClients {

    private final Map<String, Integer> controllers = new ConcurrentHashMap<>();
    private volatile boolean browserBound;

    void bind() {
        browserBound = true;
    }

    void unbind() {
        browserBound = false;
    }

    void connect(@NonNull MediaSession.ControllerInfo controller, @NonNull String packageName) {
        if (controller.getControllerVersion() != MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION && !isSelf(controller, packageName)) controllers.merge(controller.getPackageName(), 1, Integer::sum);
    }

    void disconnect(@NonNull MediaSession.ControllerInfo controller) {
        if (controller.getControllerVersion() == MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION) return;
        controllers.computeIfPresent(controller.getPackageName(), (key, count) -> count > 1 ? count - 1 : null);
    }

    boolean hasAny() {
        return browserBound || !controllers.isEmpty();
    }

    boolean isSelf(@NonNull MediaSession.ControllerInfo controller, @NonNull String packageName) {
        return packageName.equals(controller.getPackageName());
    }
}
