package androidx.media3.common;
public class MediaMetadata {
 public static final int MEDIA_TYPE_VIDEO=6, MEDIA_TYPE_TV_CHANNEL=17;
 public final String title,subtitle;public final int mediaType;
 public MediaMetadata(String title,String subtitle,int type){this.title=title;this.subtitle=subtitle;this.mediaType=type;}
 public Builder buildUpon(){return new Builder(title,subtitle,mediaType);}
 public static class Builder{String title,subtitle;int type;public Builder(String t,String s,int m){title=t;subtitle=s;type=m;}public Builder setMediaType(int t){type=t;return this;}public MediaMetadata build(){return new MediaMetadata(title,subtitle,type);}}
}
