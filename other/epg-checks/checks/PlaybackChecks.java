package com.fongmi.android.tv.playback.live;

import static org.junit.Assert.*;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.utils.EpgTimeline;
import java.lang.reflect.Proxy;
import java.time.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=35)
public class PlaybackChecks {
    final ZoneId zone=ZoneId.of("Asia/Shanghai");
    final Channel channel=new Channel();
    final LivePlaybackState state=new LivePlaybackState();
    final List<LivePlayRequest> requests=new ArrayList<>();
    MediaMetadata metadata;
    final LivePlaybackHost host=(LivePlaybackHost)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{LivePlaybackHost.class},(proxy,method,args)->{
        switch(method.getName()) {
            case "getZoneId": return zone;
            case "isPlaybackServiceReady": case "hasPlaybackSession": return true;
            case "isPlayerLive": return false;
            case "getPlayerPosition": return 0L;
            case "getGroupCount": case "getGroupPosition": return 0;
            case "renderPlaybackMetadata": metadata=(MediaMetadata)args[0];return null;
            default: return null;
        }
    });
    final LiveDataSource source=new LiveDataSource(){public void getUrl(LivePlayRequest request){requests.add(request);}public void cancelUrl(){}};
    final LivePlaybackController controller=new LivePlaybackController(host,source,state);
    EpgProgramme programme(long start,long end){return new EpgProgramme(null,start,end,"暂无节目信息",Instant.ofEpochMilli(start).atZone(zone).toLocalDate(),zone);}
    void playing(EpgData data){state.setChannel(channel);state.setPlayingRequest(LivePlayRequest.catchup(channel,data,C.TIME_UNSET),"fixture");}
    @Test public void finiteReplayEndRequestsNextHourEvenWhenPlayerReportsVod() {
        LocalDate yesterday=LocalDate.now(zone).minusDays(1);long start=yesterday.atStartOfDay(zone).toInstant().toEpochMilli();
        playing(programme(start,start+3600000));controller.playbackEnded();assertEquals(1,requests.size());
        assertTrue(requests.get(0).isCatchup());assertEquals(start+3600000,requests.get(0).getCatchupData().getStartTime());
    }
    @Test public void continuationStartsAtActuallyRequestedEndAndDoesNotSkipTheRemainder() {
        long start=LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        EpgProgramme slot=programme(start,start+3600000);EpgProgramme.forRequest(slot,start+125000);playing(slot);controller.playbackEnded();
        assertEquals(start+125000,requests.get(0).getCatchupData().getStartTime());assertEquals(start+3600000,requests.get(0).getCatchupData().getEndTime());
    }
    @Test public void midnightContinuationStaysOnTheSameChannel() {
        long midnight=LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli();
        playing(programme(midnight-3600000,midnight));controller.playbackEnded();
        assertSame(channel,requests.get(0).getChannel());assertEquals(midnight,requests.get(0).getCatchupData().getStartTime());
    }
    @Test public void reachingTheLiveEdgeReturnsToLiveInsteadOfChangingChannel() {
        long now=System.currentTimeMillis(),end=now/1000*1000;
        playing(programme(now-3600000,end));controller.playbackEnded();
        assertEquals(1,requests.size());assertFalse(requests.get(0).isCatchup());assertSame(channel,requests.get(0).getChannel());
    }
    @Test public void duplicateEndEventsCannotIssueTwoRequests() {
        long end=System.currentTimeMillis()-86400000;playing(programme(end-3600000,end));
        controller.playbackEnded();controller.playbackEnded();assertEquals(1,requests.size());
    }
    @Test public void placeholdersDoNotOfferReplayOnChannelsWithoutDvrEvenIfSelected() {
        channel.replay=false;state.setChannel(channel);long now=System.currentTimeMillis();EpgProgramme slot=programme(now-3600000,now-1000);slot.setSelected(true);
        assertFalse(controller.selectEpg(slot));assertTrue(requests.isEmpty());
    }
    @Test public void futureRowsNeverCreatePlaybackRequests() {
        state.setChannel(channel);long now=System.currentTimeMillis();assertFalse(controller.selectEpg(programme(now+60000,now+3600000)));assertTrue(requests.isEmpty());
    }
    @Test public void oldChannelReplayCannotOverrideNewChannelMetadata() {
        long now=System.currentTimeMillis();playing(programme(now-86400000,now-86300000));
        Channel next=new Channel();next.name="Next channel";state.setChannel(next);
        assertFalse(controller.isCatchup());assertNull(controller.getPlayingEpgData());
        EpgData current=new EpgData();current.setTitle("Current news");controller.onEpgChanged(current);
        assertEquals("Next channel",metadata.title.toString());assertTrue(metadata.subtitle.toString().contains("Current news"));
    }
    @Test public void refreshReusesTheCurrentReplayAndPosition() {
        long now=System.currentTimeMillis();EpgProgramme item=programme(now-86400000,now-86300000);playing(item);controller.refresh();
        assertSame(item,requests.get(0).getCatchupData());assertEquals(0,requests.get(0).getPosition());
    }
}
