package com.fongmi.android.tv.bean;
import java.util.*;import java.time.*;import com.fongmi.android.tv.utils.Formatters;
public class Channel{
 public String name="Channel",id="fixture",epg="",logo="";public boolean replay=true,rtsp;public Group group;public int index;public Catchup catchup=Catchup.PLTV();private final List<Epg> days=new ArrayList<>();
 public String getTvgId(){return id;}public String getTvgName(){return name;}public String getName(){return name;}public String getShow(){return name;}public String getLogo(){return logo;}public void setLogo(String s){logo=s;}public boolean hasEpgOverride(){return epg.startsWith("http");}public String getEpg(){return epg;}public boolean hasCatchup(){return replay;}public boolean isRtsp(){return rtsp;}public Catchup getCatchup(){return catchup;}public void applyAuth(Result r){}public Result result(){return new Result();}
 public synchronized List<Epg> getDataList(){return new ArrayList<>(days);}public synchronized void setData(Epg e){days.removeIf(d->d.equal(e.getDate()));days.add(e);}public Epg getData(ZoneId z){return getData(z,LocalDate.now(z));}public Epg getData(ZoneId z,LocalDate d){return getDataList().stream().filter(e->e.equal(d.format(Formatters.DATE))).findFirst().orElse(Epg.create(id,d.format(Formatters.DATE)));}
 public Group getGroup(){return group;}public Channel group(Group g){return this;}public int getIndex(){return index;}public boolean isOnly(){return false;}public boolean isLast(){return true;}public void switchLine(boolean next){index+=next?1:-1;}
}
