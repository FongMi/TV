import static org.junit.Assert.*;
import com.fongmi.android.tv.api.LiveApi;
import com.fongmi.android.tv.api.parser.EpgParser;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.utils.*;
import com.github.catvod.net.*;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=35)
public class GuideChecks {
    final LocalDate day=LocalDate.of(2026,11,1);
    final ZoneId zone=ZoneId.of("Asia/Shanghai");
    final long start=day.atStartOfDay(zone).toInstant().toEpochMilli();
    EpgData item(long from,long to,String title) {
        EpgData item=new EpgData();item.setStartTime(from);item.setEndTime(to);item.setTitle(title);return item;
    }
    Epg real() { Epg e=Epg.create("fixture",day.toString());e.setList(List.of(item(start+3600000,start+7200000,"News")));return e; }
    @Before public void clearResponses() { OkHttp.responses.clear(); }
    @Test public void localSelectionNeverMutatesSourceAndLateDataReplacesPlaceholders() {
        Channel channel=new Channel();EpgTimeline.Cache cache=new EpgTimeline.Cache();
        Epg empty=cache.get(channel,day,zone,"暂无节目信息");assertEquals(24,empty.getList().size());
        assertSame(empty,cache.get(channel,day,zone,"暂无节目信息"));
        Epg source=real();channel.setData(source);Epg view=cache.get(channel,day,zone,"暂无节目信息");assertNotSame(empty,view);
        EpgTimeline.select(view,null,false,start+5400000);assertTrue(view.getList().stream().anyMatch(EpgData::isSelected));
        assertFalse(source.getList().get(0).isSelected());assertEquals(1,source.getList().size());
        assertTrue(EpgTimeline.hasRealProgrammes(view));
    }
    @Test public void partialActualRequestContinuesFromItsEndInsideTheSameWindow() {
        Channel channel=new Channel();EpgData slot=new EpgTimeline.Cache().get(channel,day,zone,"暂无节目信息").getList().get(0);
        EpgProgramme.forRequest(slot,start+123456);EpgData next=EpgTimeline.next(channel,slot,zone,start+1800000,"暂无节目信息");
        assertEquals(start+123000,next.getStartTime());assertEquals(start+3600000,next.getEndTime());
        assertEquals(start+3600000,slot.getEndTime());
    }
    @Test public void continuationWorksForRealProgrammesAndAcrossMidnight() {
        Channel channel=new Channel();channel.setData(real());
        EpgData real=new EpgTimeline.Cache().get(channel,day,zone,"暂无节目信息").getList().get(1);
        EpgProgramme.forRequest(real,start+4000000);EpgData rest=EpgTimeline.next(channel,real,zone,start+5000000,"暂无节目信息");
        assertEquals(start+4000000,rest.getStartTime());assertEquals("News",rest.getTitle());
        EpgData last=new EpgTimeline.Cache().get(channel,day,zone,"暂无节目信息").getList().get(23);
        EpgData next=EpgTimeline.next(channel,last,zone,start+90000000,"暂无节目信息");
        assertEquals(start+86400000,next.getStartTime());
    }
    @Test public void liveEdgeDoesNotCreateFutureOrZeroLengthReplay() {
        Channel channel=new Channel();EpgData slot=new EpgTimeline.Cache().get(channel,day,zone,"暂无节目信息").getList().get(0);
        EpgProgramme.forRequest(slot,start+123456);
        assertNull(EpgTimeline.next(channel,slot,zone,start+123500,"暂无节目信息"));
    }
    @Test public void resultScopeUsesSourceChannelAndZoneIdentity() {
        Live source=new Live();Channel channel=new Channel();LiveViewModel.GuideResult result=new LiveViewModel.GuideResult(source,channel,zone,day,real(),1,true);
        assertTrue(result.matches(source,channel,zone));assertFalse(result.matches(new Live(),channel,zone));
        assertFalse(result.matches(source,new Channel(),zone));assertFalse(result.matches(source,channel,ZoneId.of("UTC")));
    }
    String json(String date,String title) {return "{\"date\":\""+date+"\",\"epg_data\":[{\"start\":\"01:00\",\"end\":\"02:00\",\"title\":\""+title+"\"}]}";}
    @Test public void dateApiRejectsWrongDateAndEmptyRefreshAndAcceptsLaterCorrection() throws Exception {
        Live live=new Live();Channel channel=new Channel();channel.epg="http://fixture/{date}";Epg original=real();channel.setData(original);
        OkHttp.responses.put("http://fixture/"+day,json(day.minusDays(1).toString(),"Wrong"));
        assertFalse(LiveApi.refreshEpg(live,channel,zone,day));assertSame(original,channel.getData(zone,day));
        OkHttp.responses.put("http://fixture/"+day,"{\"date\":\""+day+"\",\"epg_data\":[]}");
        assertFalse(LiveApi.refreshEpg(live,channel,zone,day));assertSame(original,channel.getData(zone,day));
        OkHttp.responses.put("http://fixture/"+day,json(day.toString(),"Corrected"));
        assertTrue(LiveApi.refreshEpg(live,channel,zone,day));assertEquals("Corrected",channel.getData(zone,day).getList().get(0).getTitle());
    }
    @Test public void failedDateApiLeavesGoodDataAvailable() {
        Channel channel=new Channel();channel.epg="http://fixture/{date}";Epg original=real();channel.setData(original);
        try {LiveApi.refreshEpg(new Live(),channel,zone,day);fail();}catch(Exception expected){}
        assertSame(original,channel.getData(zone,day));
    }
    String xml(String title) {return "<tv><channel id=\"fixture\"><display-name>Channel</display-name></channel><programme channel=\"fixture\" start=\"20261101010000 +0800\" stop=\"20261101020000 +0800\"><title>"+title+"</title></programme></tv>";}
    @Test public void forcedXmlRefreshBypassesFreshCacheAndFailureRetainsTheLastGoodFile() throws Exception {
        Path cache=Files.createTempDirectory("epg-check-");System.setProperty("epg.fixture.cache",cache.toString());
        try {
            Live live=new Live();Channel channel=new Channel();Group group=new Group();group.channels.add(channel);live.groups.add(group);
            String url="http://fixture/epg.xml";OkHttp.responses.put(url,xml("Original"));EpgParser.start(live,url);
            OkHttp.responses.put(url,xml("Corrected"));EpgParser.start(live,url);assertEquals("Original",channel.getData(zone,day).getList().get(0).getTitle());
            EpgParser.start(live,url,true);assertEquals("Corrected",channel.getData(zone,day).getList().get(0).getTitle());
            OkHttp.responses.put(url,"<tv/>");try{EpgParser.start(live,url,true);fail();}catch(Exception expected){}
            EpgParser.start(live,url);assertEquals("Corrected",channel.getData(zone,day).getList().get(0).getTitle());
        } finally {System.clearProperty("epg.fixture.cache");try(var files=Files.walk(cache)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
    }
}
