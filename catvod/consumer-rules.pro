# External spider JARs compile against this runtime API.
-keep class com.github.catvod.Proxy { *; }
-keep class com.github.catvod.crawler.** { *; }
-keep class * extends com.github.catvod.crawler.Spider

# Python spiders import this helper by its fully qualified Java name.
-keep class com.github.catvod.net.OkHttp { *; }
