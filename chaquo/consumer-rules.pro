-keep class com.chaquo.python.** { * ; }
-keep class kotlin.jvm.functions.** { * ; }
-keep class kotlin.jvm.internal.FunctionBase { * ; }
-keep class kotlin.reflect.KAnnotatedElement { *; }
-dontwarn org.jetbrains.annotations.NotNull

-keepclassmembers class com.fongmi.chaquo.Platform {
    public android.content.Context getApplication();
}
