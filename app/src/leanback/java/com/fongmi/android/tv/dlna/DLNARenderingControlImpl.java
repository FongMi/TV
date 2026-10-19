package com.fongmi.android.tv.dlna;

import android.content.Context;
import android.media.AudioManager;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.support.model.Channel;
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl;
import org.jupnp.support.renderingcontrol.lastchange.ChannelMute;
import org.jupnp.support.renderingcontrol.lastchange.ChannelVolume;
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlVariable;

public class DLNARenderingControlImpl extends AbstractAudioRenderingControl {

    private final Context context;
    private final Runnable onChange;

    public DLNARenderingControlImpl(Context context, Runnable onChange) {
        this.context = context;
        this.onChange = onChange;
    }

    private AudioManager getAudioManager() {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
        return new UnsignedIntegerFourBytes[]{getDefaultInstanceID()};
    }

    @Override
    public boolean getMute(UnsignedIntegerFourBytes instanceId, String channelName) {
        return getAudioManager().isStreamMute(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void setMute(UnsignedIntegerFourBytes instanceId, String channelName, boolean desiredMute) {
        getAudioManager().adjustStreamVolume(AudioManager.STREAM_MUSIC, desiredMute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
        getLastChange().setEventedValue(instanceId, new RenderingControlVariable.Mute(new ChannelMute(Channel.Master, getMute(instanceId, channelName))));
        onChange.run();
    }

    @Override
    public UnsignedIntegerTwoBytes getVolume(UnsignedIntegerFourBytes instanceId, String channelName) {
        AudioManager am = getAudioManager();
        int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return new UnsignedIntegerTwoBytes(max > 0 ? Math.round(current * 100.0 / max) : 100);
    }

    @Override
    public void setVolume(UnsignedIntegerFourBytes instanceId, String channelName, UnsignedIntegerTwoBytes desiredVolume) {
        AudioManager manager = getAudioManager();
        int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = (int) Math.round(desiredVolume.getValue() * max / 100.0);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        getLastChange().setEventedValue(instanceId, new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, getVolume(instanceId, channelName).getValue().intValue())));
        onChange.run();
    }

    @Override
    protected Channel[] getCurrentChannels() {
        return new Channel[]{Channel.Master};
    }
}
