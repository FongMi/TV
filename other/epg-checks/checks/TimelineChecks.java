import static org.junit.Assert.*;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.utils.*;
import java.time.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.junit.Test;
import java.util.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=35)
public class TimelineChecks {
    static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    static final String DATE="2026-10-31";
    static final long DAY=LocalDate.parse(DATE).atStartOfDay(ZONE).toInstant().toEpochMilli(),END=LocalDate.parse("2026-11-01").atStartOfDay(ZONE).toInstant().toEpochMilli();
    static EpgTimeline.Entry real(long from,long to,String title) {
        return new EpgTimeline.Entry(new EpgData(),DAY+from*60000,DAY+to*60000,title);
    }
    static EpgProgramme slot(EpgTimeline.Entry e) {
        return new EpgProgramme(e.source,e.start,e.end,e.title,LocalDate.parse(DATE),ZONE);
    }
    static List<EpgTimeline.Entry> plan(EpgTimeline.Entry... entries) {
        return EpgTimeline.plan(Arrays.asList(entries),DAY,END,ZONE,"暂无节目信息");
    }
    static void covered(List<EpgTimeline.Entry> rows,long start,long end) {
        long cursor=start;
        for(EpgTimeline.Entry row:rows) {
            assertEquals(cursor,Math.max(start,row.start));
            assertTrue(row.end>row.start);
            if(row.fallback())assertTrue(row.end-row.start<=EpgTimeline.SLOT_MILLIS);
            cursor=Math.min(end,row.end);
        }
        assertEquals(end,cursor);
    }
    @Test public void entireMissingDayHasTwentyFourOneHourChoices() {
        List<EpgTimeline.Entry> rows=plan();assertEquals(24,rows.size());covered(rows,DAY,END);
        assertTrue(rows.stream().allMatch(EpgTimeline.Entry::fallback));
        assertTrue(rows.stream().allMatch(e->e.title.equals("暂无节目信息")));
        assertEquals("00:00 ~ 01:00",slot(rows.get(0)).getTime());
    }
    @Test public void beginningMiddleAndEndGapsAreAllCovered() {
        EpgTimeline.Entry first=real(70,100,"节目甲"),second=real(145,185,"节目乙");
        List<EpgTimeline.Entry> rows=plan(first,second);covered(rows,DAY,END);
        assertTrue(rows.contains(first));assertTrue(rows.contains(second));
        assertTrue(rows.stream().anyMatch(e->e.fallback()&&e.start==DAY+100*60000&&e.end==DAY+120*60000));
    }
    @Test public void missingMidnightPieceDoesNotBorrowThePreviousTitle() {
        List<EpgTimeline.Entry> rows=plan(real(-11,0,"纪录片"),real(106,132,"南岭物语"));
        covered(rows,DAY,END);assertEquals(2,rows.stream().filter(e->e.start<DAY+106*60000).count());
        assertTrue(rows.get(0).fallback());assertEquals(DAY+60*60000,rows.get(1).start);
        assertEquals(DAY+106*60000,rows.get(1).end);
    }
    @Test public void genuineCarryOverKeepsItsFullRange() {
        EpgTimeline.Entry carry=real(-35,7,"一代匠师（24）");
        List<EpgTimeline.Entry> rows=plan(carry,real(7,48,"一代匠师（25）"));
        covered(rows,DAY,END);assertSame(carry,rows.get(0));assertEquals(DAY-35*60000,rows.get(0).start);
    }
    @Test public void splitMidnightEpisodeIsJoinedWithoutMutatingInput() {
        EpgTimeline.Entry last=real(-35,0,"一代匠师（24）");
        last=new EpgTimeline.Entry(last.source,last.start,DAY-1000,last.title);
        EpgTimeline.Entry first=real(0,7,"一代匠师（24）");
        List<EpgTimeline.Entry> rows=plan(last,first,real(7,48,"一代匠师（25）"));
        covered(rows,DAY,END);assertEquals(DAY-35*60000,rows.get(0).start);assertEquals(DAY+7*60000,rows.get(0).end);
        assertEquals(DAY-1000,last.end);assertEquals(DAY,first.start);
    }
    @Test public void identicalGenericNamesDoNotProveSameEpisode() {
        List<EpgTimeline.Entry> rows=plan(real(-60,0,"活力影院"),real(0,60,"活力影院"));
        covered(rows,DAY,END);assertEquals(DAY,rows.get(0).start);
    }
    @Test public void overlappingRecordsAreClippedInTheViewOnly() {
        EpgTimeline.Entry first=real(120,240,"节目甲"),second=real(180,300,"节目乙");
        List<EpgTimeline.Entry> rows=plan(first,second);covered(rows,DAY,END);
        EpgTimeline.Entry actual=rows.stream().filter(e->e.source==first.source).findFirst().get();
        assertEquals(DAY+180*60000,actual.end);assertEquals(DAY+240*60000,first.end);
    }
    @Test public void duplicateStartsDoNotProduceOverlappingRows() {
        List<EpgTimeline.Entry> rows=plan(real(60,120,"短记录"),real(60,180,"旧长记录"));
        covered(rows,DAY,END);assertEquals(1,rows.stream().filter(e->e.start==DAY+60*60000).count());
    }
    @Test public void invalidRecordsAndLegacyServerSlotsBecomeLocalOneHourWindows() {
        List<EpgTimeline.Entry> rows=plan(real(0,1440,"暂无节目信息"),real(0,300,"节目单缺失（时段回看）"),
                real(180,120,"倒序"),new EpgTimeline.Entry(null,0,1,"invalid"));
        covered(rows,DAY,END);assertEquals(24,rows.size());assertTrue(rows.stream().allMatch(EpgTimeline.Entry::fallback));
    }
    @Test public void genericAllDayPlaceholderIsAlsoSlicedButNamedRealProgrammesRemain() {
        assertEquals(24,plan(real(0,1440,"精彩节目")).size());
        List<EpgTimeline.Entry> rows=plan(real(0,1440,"马拉松赛事全天特别直播"));
        assertEquals(1,rows.size());assertFalse(rows.get(0).fallback());
    }
    @Test public void thirdPartyTwoHourGenericPlaceholdersUseLocalOneHourWindows() {
        EpgTimeline.Entry source=real(0,120,"精彩节目");
        List<EpgTimeline.Entry> rows=plan(source,real(120,240,"精彩節目"),real(240,1440,"暂无节目信息"));
        covered(rows,DAY,END);assertEquals(24,rows.size());
        assertTrue(rows.stream().allMatch(e->e.fallback()&&e.title.equals("暂无节目信息")));
        assertEquals(DAY+120*60000,source.end);
    }
    @Test public void daylightSavingDaysUseActualDayLength() {
        ZoneId zone=ZoneId.of("America/New_York");
        for(String date:Arrays.asList("2026-03-08","2026-11-01")) {
            long from=LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),to=LocalDate.parse(date).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
            List<EpgTimeline.Entry> rows=EpgTimeline.plan(Collections.emptyList(),from,to,zone,"暂无节目信息");
            assertEquals(date.contains("03-")?23:25,rows.size());covered(rows,from,to);
        }
    }
    @Test public void tinyGapsKeepTheirExactSecondBounds() {
        EpgTimeline.Entry first=real(0,1,"第一档"),next=new EpgTimeline.Entry(new EpgData(),DAY+61000,DAY+120000,"第二档");
        List<EpgTimeline.Entry> rows=plan(first,next);covered(rows,DAY,END);
        EpgTimeline.Entry gap=rows.get(1);assertEquals(1000,gap.end-gap.start);
        assertTrue(slot(gap).getTime().contains("00:01:01"));
    }
    @Test public void currentSlotRequestsStopAtNowAndKeepTheDisplayWindowStable() {
        EpgProgramme slot=new EpgProgramme(null,DAY,DAY+1800000,"暂无节目信息",LocalDate.parse(DATE),ZONE);
        EpgData request=EpgProgramme.forRequest(slot,DAY+123456);
        assertEquals(DAY+123000,request.getEndTime());assertEquals(DAY+1800000,slot.getEndTime());
        assertEquals(DAY+123000,EpgProgramme.playbackEnd(slot));
    }
    @Test public void futureSlotCannotGenerateAReplayRequest() {
        EpgProgramme slot=new EpgProgramme(null,DAY,DAY+1800000,"暂无节目信息",LocalDate.parse(DATE),ZONE);
        try { EpgProgramme.forRequest(slot,DAY-1);fail("Future requests must be rejected"); }
        catch(IllegalArgumentException expected) { }
    }
    @Test public void sameClockOnDifferentDatesIsNotTheSameSlot() {
        EpgProgramme first=new EpgProgramme(null,DAY,DAY+1800000,"暂无节目信息",LocalDate.parse(DATE),ZONE);
        EpgProgramme next=new EpgProgramme(null,END,END+1800000,"暂无节目信息",LocalDate.parse(DATE).plusDays(1),ZONE);
        assertNotEquals(first,next);assertEquals("23:00 ~ 24:00",slot(plan().get(23)).getTime());
    }
    @Test public void randomizedPartialGuidesAlwaysCoverTheSelectedDay() {
        Random random=new Random(769);
        for(int pass=0;pass<150;pass++) {
            List<EpgTimeline.Entry> input=new ArrayList<>();
            for(int i=0;i<20;i++){int start=random.nextInt(1700)-100;input.add(real(start,start+1+random.nextInt(180),"节目"+i));}
            covered(EpgTimeline.plan(input,DAY,END,ZONE,"暂无节目信息"),DAY,END);
        }
    }
}
