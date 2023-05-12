# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * { @com.google.gson.annotations.SerializedName <fields>; }
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# SimpleXML
-keep interface org.simpleframework.xml.core.Label { public *; }
-keep class * implements org.simpleframework.xml.core.Label { public *; }
-keep interface org.simpleframework.xml.core.Parameter { public *; }
-keep class * implements org.simpleframework.xml.core.Parameter { public *; }
-keep interface org.simpleframework.xml.core.Extractor { public *; }
-keep class * implements org.simpleframework.xml.core.Extractor { public *; }
-keepclassmembers,allowobfuscation class * { @org.simpleframework.xml.Text <fields>; }
-keepclassmembers,allowobfuscation class * { @org.simpleframework.xml.Path <fields>; }
-keepclassmembers,allowobfuscation class * { @org.simpleframework.xml.ElementList <fields>; }

# OkHttp
-dontwarn okhttp3.**
-keep class okio.** { *; }
-keep class okhttp3.** { *; }

# Cronet
-keep class org.chromium.net.** { *; }
-keep class com.google.net.cronet.** { *; }

# CatVod
-keep class com.github.catvod.crawler.** { *; }
-keep class * extends com.github.catvod.crawler.Spider

# Cling
-keep class org.fourthline.cling.** { *; }

# EXO
-keep class org.xmlpull.v1.** { *; }

# IJK
-keep class tv.danmaku.ijk.media.player.** { *; }
-keep class tv.danmaku.ijk.media.player.IjkMediaPlayer { *; }
-keep class tv.danmaku.ijk.media.player.ffmpeg.FFmpegApi { *; }

# Sardine
-keep class com.thegrizzlylabs.sardineandroid.** { *; }

# TVBus
-keep class com.tvbus.engine.** { *; }

# ZLive
-keep class com.sun.jna.** { *; }
-keep class com.east.android.zlive.** { *; }

# Zxing
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}