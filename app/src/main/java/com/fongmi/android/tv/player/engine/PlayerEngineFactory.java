package com.fongmi.android.tv.player.engine;

import static com.fongmi.android.tv.player.engine.PlayerEngine.Type.EXO;
import static com.fongmi.android.tv.player.engine.PlayerEngine.Type.MPV;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.hls.CmgConfiguration;
import androidx.media3.mpvplayer.media.MpvDrmSupport;

import com.fongmi.android.tv.player.exo.ExoPlayerEngine;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.player.mpv.MpvPlayerEngine;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.UrlUtil;

public final class PlayerEngineFactory {

    public static PlayerEngine create(Player.Listener listener) {
        return create(resolve(), listener);
    }

    public static PlayerEngine create(PlaySpec spec, Player.Listener listener) {
        return create(resolve(spec), listener);
    }

    public static PlayerEngine create(PlayerEngine.Type type, Player.Listener listener) {
        return switch (type) {
            case EXO -> new ExoPlayerEngine(listener);
            case MPV -> new MpvPlayerEngine(listener);
        };
    }

    public static boolean matches(PlayerEngine engine, PlaySpec spec) {
        return engine != null && engine.getType() == resolve(spec) && !engine.needsRebuild();
    }

    public static boolean canPreload(PlaySpec spec) {
        return !isCmg(spec);
    }

    private static PlayerEngine.Type resolve(PlaySpec spec) {
        return requiresExo(spec) ? EXO : resolve();
    }

    private static PlayerEngine.Type resolve() {
        return isMpvReady() ? MPV : EXO;
    }

    static boolean requiresExo(PlaySpec spec) {
        return isCmg(spec) || "smb".equals(UrlUtil.scheme(spec.getUrl()))
                || (spec.getDrm() != null && !MpvDrmSupport.supports(MediaItemFactory.from(spec)));
    }

    private static boolean isCmg(PlaySpec spec) {
        return CmgConfiguration.fromMediaUri(spec.getUri()) != null;
    }

    private static boolean isMpvReady() {
        return PlayerSetting.isMpv() && MpvPlayerEngine.isAvailable();
    }
}
