package com.fongmi.android.tv.playback.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.EpgData;

import java.util.Objects;

public final class LivePlayRequest {

    private final Channel channel;
    private final EpgData data;
    private final String group;
    private final long position;
    private final int line;

    private LivePlayRequest(@NonNull Channel channel, @Nullable EpgData data, long position) {
        this.channel = channel;
        this.data = data;
        this.group = channel.getGroup() == null ? "" : channel.getGroup().getName();
        this.position = position;
        this.line = channel.getIndex();
    }

    public static LivePlayRequest live(@NonNull Channel channel, long position) {
        return new LivePlayRequest(channel, null, position);
    }

    public static LivePlayRequest catchup(@NonNull Channel channel, @NonNull EpgData data, long position) {
        return new LivePlayRequest(channel, data, position);
    }

    public Channel getChannel() {
        return channel;
    }

    @NonNull
    public EpgData getCatchupData() {
        if (data == null) throw new IllegalStateException("Not a catchup request");
        return data;
    }

    public long getPosition() {
        return position;
    }

    public boolean isCatchup() {
        return data != null;
    }

    public boolean matches(@Nullable Channel current) {
        String currentGroup = current == null || current.getGroup() == null ? "" : current.getGroup().getName();
        return channel.equals(current) && group.equals(currentGroup) && line == current.getIndex();
    }

    public boolean matches(@Nullable LivePlayRequest request) {
        return request != null && channel.equals(request.channel) && group.equals(request.group) && line == request.line && position == request.position && Objects.equals(data, request.data);
    }
}
