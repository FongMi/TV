import static org.junit.Assert.*;
import com.fongmi.android.tv.utils.EpgRefreshPolicy;
import java.time.*;
import org.junit.Test;

public class RefreshChecks {
    final Object source = new Object(), channel = new Object();
    final LocalDate day = LocalDate.of(2026,11,1);
    final ZoneId zone = ZoneId.of("Asia/Shanghai");
    final EpgRefreshPolicy policy = new EpgRefreshPolicy();
    long begin(long now, boolean missing) { return policy.begin(source,channel,day,day,zone,"fixture",false,missing,now); }
    @Test public void onlyOneRequestMayBeInFlight() {
        long token=begin(1000,true); assertTrue(token>0); assertEquals(0,begin(100000,true));
        policy.complete(token,false,1000); assertTrue(begin(100000,true)>0);
    }
    @Test public void missingGuideBacksOffAndCapsAtFifteenMinutes() {
        long now=1000;
        for(long delay:new long[]{60000,120000,240000,480000,900000,900000}) {
            long token=begin(now,true); assertTrue(token>0); policy.complete(token,false,now);
            assertEquals(0,begin(now+delay-1,true)); now+=delay;
        }
    }
    @Test public void successfulNormalGuideRefreshesHourly() {
        long token=begin(1000,false); policy.complete(token,true,1000);
        assertEquals(0,begin(3600999,false)); assertTrue(begin(3601000,false)>0);
    }
    @Test public void failedNormalGuideMayRetryAfterOneMinute() {
        long token=begin(1000,false); policy.complete(token,false,1000);
        assertEquals(0,begin(60999,false)); assertTrue(begin(61000,false)>0);
    }
    @Test public void resetDoesNotLetAnOldCompletionReleaseTheNewRequest() {
        long old=begin(1000,true); policy.reset(); long current=begin(1001,true);
        assertNotEquals(old,current); policy.complete(old,true,1002); assertEquals(0,begin(1000000,true));
        policy.complete(current,false,1002); assertTrue(begin(1000000,true)>0);
    }
    @Test public void xmlRefreshIsSharedAcrossChannelsAndDates() {
        long token=policy.begin(source,channel,day,day,zone,"xml",true,true,1000);policy.complete(token,true,1000);
        assertEquals(0,policy.begin(source,new Object(),day.minusDays(1),day,zone,"xml",true,true,2000));
        assertTrue(policy.begin(source,new Object(),day.minusDays(1),day,zone,"xml",true,true,61000)>0);
    }
    @Test public void sourceAndCalendarGenerationHaveIndependentSchedules() {
        long token=begin(1000,false);policy.complete(token,true,1000);
        token=policy.begin(new Object(),channel,day,day,zone,"fixture",false,false,1001);assertTrue(token>0);policy.complete(token,true,1001);
        assertTrue(policy.begin(source,channel,day,day.plusDays(1),zone,"fixture",false,false,1002)>0);
    }
    @Test public void dateZoneAndChannelCannotReuseAnUnrelatedThrottle() {
        long token=begin(1000,false);policy.complete(token,true,1000);
        token=policy.begin(source,new Object(),day,day,zone,"fixture",false,false,1001);assertTrue(token>0);policy.complete(token,true,1001);
        token=policy.begin(source,channel,day.minusDays(1),day,zone,"fixture",false,false,1002);assertTrue(token>0);policy.complete(token,true,1002);
        assertTrue(policy.begin(source,channel,day,day,ZoneId.of("UTC"),"fixture",false,false,1003)>0);
    }
}
