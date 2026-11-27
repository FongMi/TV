import static org.junit.Assert.*;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.ui.custom.LiveProgrammeInfo;
import com.fongmi.android.tv.utils.EpgHistory;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=35)
public class ProgrammeInfoChecks {
 static EpgData item(String title,long start,long end){EpgData d=new EpgData();d.setTitle(title);d.setStart("08:00");d.setEnd("09:00");d.setStartTime(start);d.setEndTime(end);return d;}
 static class Fixture {
  Activity activity=Robolectric.buildActivity(Activity.class).setup().visible().get();
  ActivityLiveBinding b=new ActivityLiveBinding();AtomicReference<EpgData> playing=new AtomicReference<>();
  AtomicBoolean replay=new AtomicBoolean();AtomicInteger requests=new AtomicInteger(),metadata=new AtomicInteger();
  AtomicReference<Channel> owner=new AtomicReference<>(new Channel());
  boolean failProvider,failRefresh,failMetadata;
  LiveProgrammeInfo helper;
  Fixture(){
   b.root=new FrameLayout(activity);b.root.setBackgroundColor(0xffffe8a0);
   FrameLayout video=new FrameLayout(activity);b.root.addView(video,new FrameLayout.LayoutParams(-1,-1));
   b.widget.info=new LinearLayout(activity);b.widget.info.setGravity(Gravity.CENTER_VERTICAL);b.widget.info.setBackgroundColor(0xdd000000);
   b.widget.info.setVisibility(View.GONE);b.widget.play=new TextView(activity);b.widget.play.setTextColor(0xffffffff);
   LinearLayout column=new LinearLayout(activity);column.setOrientation(LinearLayout.VERTICAL);column.addView(b.widget.play);
   b.widget.programmeProgress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);b.widget.programmeProgress.setMax(1000);column.addView(b.widget.programmeProgress,new LinearLayout.LayoutParams(-1,4));
   b.widget.info.addView(column,new LinearLayout.LayoutParams(-1,-2));video.addView(b.widget.info,new FrameLayout.LayoutParams(-1,56,Gravity.BOTTOM));
   b.widget.infoPip=new View(activity);b.widget.infoPip.setVisibility(View.GONE);video.addView(b.widget.infoPip);
   b.recycler=new LinearLayout(activity);b.recycler.setBackgroundColor(0xee202020);b.recycler.setVisibility(View.GONE);b.root.addView(b.recycler,new FrameLayout.LayoutParams(200,-1));
   b.control.root=new FrameLayout(activity);b.control.root.setVisibility(View.GONE);b.root.addView(b.control.root,new FrameLayout.LayoutParams(-1,-1));
   b.control.bottom=new LinearLayout(activity);b.control.root.addView(b.control.bottom,new FrameLayout.LayoutParams(-1,96,Gravity.BOTTOM));
   b.control.top=new View(activity);b.control.root.addView(b.control.top,new FrameLayout.LayoutParams(-1,56,Gravity.TOP));
   b.control.center=new View(activity);b.control.root.addView(b.control.center,new FrameLayout.LayoutParams(160,48,Gravity.CENTER));
   b.control.right.root=new View(activity);b.control.root.addView(b.control.right.root,new FrameLayout.LayoutParams(48,96,Gravity.RIGHT|Gravity.CENTER_VERTICAL));
   b.control.subtitle=new TextView(activity);b.control.root.addView(b.control.subtitle);
   activity.setContentView(b.root);
   helper=new LiveProgrammeInfo(b,()->owner.get(),()->{if(failProvider)throw new IllegalStateException("fixture provider");return playing.get();},replay::get,()->{requests.incrementAndGet();if(failRefresh)throw new IllegalStateException("fixture refresh");},()->{metadata.incrementAndGet();if(failMetadata)throw new IllegalStateException("fixture metadata");});
  }
  void layout(int width,int height){for(int i=0;i<4;i++){b.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));b.root.layout(0,0,width,height);helper.onPreDraw();}}
 }
 @Test public void datesHaveSevenHistoryDaysAndTwoFutureDays(){LocalDate day=LocalDate.of(2026,10,30);List<LocalDate> dates=EpgHistory.dates(day);assertEquals(10,dates.size());assertEquals(day.minusDays(7),dates.get(0));assertEquals(day,dates.get(7));assertEquals(day.plusDays(2),dates.get(9));}
 @Test public void datesHandleLeapAndYearBoundaries(){assertTrue(EpgHistory.dates(LocalDate.of(2024,3,1)).contains(LocalDate.of(2024,2,29)));assertEquals(LocalDate.of(2025,12,26),EpgHistory.dates(LocalDate.of(2026,1,2)).get(0));}
 @Test public void clampKeepsAvailableFutureAndHistory(){LocalDate day=LocalDate.of(2026,10,30);assertEquals(day,EpgHistory.clamp(null,day));assertEquals(day.plusDays(2),EpgHistory.clamp(day.plusDays(2),day));assertEquals(day,EpgHistory.clamp(day.plusDays(3),day));assertEquals(day.minusDays(7),EpgHistory.clamp(day.minusDays(8),day));}
 @Test public void naturalDatesRemainStableAcrossDst(){ZoneId zone=ZoneId.of("America/New_York");LocalDate today=LocalDate.of(2026,11,2);List<LocalDate> dates=EpgHistory.dates(today);assertEquals(LocalDate.of(2026,10,26),dates.get(0));assertEquals(25,Duration.between(today.minusDays(1).atStartOfDay(zone),today.atStartOfDay(zone)).toHours());}
 @Test public void cachedProgrammeAppearsWithoutRefreshingPlayback(){Fixture f=new Fixture();long now=System.currentTimeMillis();f.playing.set(item("Live",now-1200000,now+2400000));f.b.recycler.setVisibility(View.VISIBLE);f.layout(640,320);assertTrue(f.b.widget.play.getText().toString().contains("Live"));assertEquals(333,f.b.widget.programmeProgress.getProgress());assertTrue(f.requests.get()>0);assertEquals(1,f.metadata.get());f.layout(640,320);assertEquals(1,f.metadata.get());assertTrue(f.requests.get()>0);}
 @Test public void lateXmlAndProgrammeChangesUpdateTheSameBar(){Fixture f=new Fixture();f.b.recycler.setVisibility(View.VISIBLE);f.layout(640,320);assertEquals("No programme information",f.b.widget.play.getText().toString());assertEquals(View.GONE,f.b.widget.programmeProgress.getVisibility());long now=System.currentTimeMillis();f.playing.set(item("Late XML",now-10000,now+10000));f.layout(640,320);assertTrue(f.b.widget.play.getText().toString().contains("Late XML"));f.playing.set(item("Next",now,now+10000));f.layout(640,320);assertTrue(f.b.widget.play.getText().toString().contains("Next"));}
 @Test public void catchupNeverDisplaysTheLiveProgrammeProgress(){Fixture f=new Fixture();long now=System.currentTimeMillis();f.playing.set(item("History being watched",now-86400000,now-86300000));f.replay.set(true);f.b.recycler.setVisibility(View.VISIBLE);f.layout(390,844);assertTrue(f.b.widget.play.getText().toString().contains("History being watched"));assertEquals(View.GONE,f.b.widget.programmeProgress.getVisibility());f.layout(390,844);assertEquals(1,f.metadata.get());}
 @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void footerStaysAtBottomInFrontAndReservesSpaceInFourSizes(){for(int[] size:new int[][]{{800,360},{640,320},{390,844},{320,640}}){Fixture f=new Fixture();Object background=f.b.widget.info.getBackground();f.b.recycler.setVisibility(View.VISIBLE);f.layout(size[0],size[1]);assertSame(f.b.root,f.b.widget.info.getParent());assertSame(background,f.b.widget.info.getBackground());assertEquals(size[1]-56,f.b.widget.info.getTop());assertTrue(f.b.root.indexOfChild(f.b.widget.info)>f.b.root.indexOfChild(f.b.recycler));assertTrue(f.b.recycler.getBottom()<=f.b.widget.info.getTop());f.b.recycler.setVisibility(View.GONE);f.b.control.root.setVisibility(View.VISIBLE);f.layout(size[0],size[1]);assertTrue(f.b.control.bottom.getBottom()<=f.b.widget.info.getTop());for(View buttons:new View[]{f.b.control.center,f.b.control.right.root})assertTrue(buttons.getBottom()+buttons.getTranslationY()<=f.b.control.bottom.getTop());f.b.control.root.setVisibility(View.GONE);f.layout(size[0],size[1]);assertEquals(View.GONE,f.b.widget.info.getVisibility());assertEquals(0,((ViewGroup.MarginLayoutParams)f.b.recycler.getLayoutParams()).bottomMargin);}}
 @Test public void sourceSwitchKeepsNativeInformationLifetimeAndLockedControlsHideIt(){Fixture f=new Fixture();f.b.control.root.setVisibility(View.VISIBLE);f.layout(640,320);assertEquals(View.VISIBLE,f.b.widget.info.getVisibility());f.helper.onNativeInfoShown();f.b.control.root.setVisibility(View.GONE);f.b.widget.info.setVisibility(View.VISIBLE);f.layout(640,320);assertEquals(View.VISIBLE,f.b.widget.info.getVisibility());f.b.widget.info.setVisibility(View.GONE);f.layout(640,320);assertEquals(View.GONE,f.b.widget.info.getVisibility());f.b.control.root.setVisibility(View.VISIBLE);f.b.control.bottom.setVisibility(View.GONE);f.layout(640,320);assertEquals(View.GONE,f.b.widget.info.getVisibility());}
 @Test public void failedProviderClearsOldChannelAndTheNextChannelRecovers() {
  Fixture f=new Fixture();long now=System.currentTimeMillis();f.b.recycler.setVisibility(View.VISIBLE);
  f.playing.set(item("Old channel",now-10000,now+10000));f.layout(640,320);
  f.owner.set(new Channel());f.failProvider=true;f.layout(640,320);
  assertEquals("No programme information",f.b.widget.play.getText().toString());
  assertEquals(View.GONE,f.b.widget.programmeProgress.getVisibility());
  f.failProvider=false;f.playing.set(item("Recovered channel",now-10000,now+10000));f.layout(640,320);
  assertTrue(f.b.widget.play.getText().toString().contains("Recovered channel"));
 }
 @Test public void callbackFailuresCannotFreezeTheProgrammeBarOrItsProgress() {
  Fixture f=new Fixture();long now=System.currentTimeMillis();f.b.recycler.setVisibility(View.VISIBLE);
  f.failMetadata=true;f.failRefresh=true;f.playing.set(item("First",now-10000,now+10000));f.layout(640,320);
  assertTrue(f.b.widget.play.getText().toString().contains("First"));
  f.playing.set(item("Second",now-10000,now+30000));f.layout(640,320);
  assertTrue(f.b.widget.play.getText().toString().contains("Second"));assertEquals(250,f.b.widget.programmeProgress.getProgress());
  assertTrue(f.requests.get()>1);assertTrue(f.metadata.get()>1);
 }

}
