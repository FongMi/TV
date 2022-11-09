# TV

### Based on CatVod  
https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### Download
[TV-Java](https://github.com/FongMi/TV/blob/main/release/leanback-java.apk?raw=true "TV-Java")  
[TV-Python](https://github.com/FongMi/TV/blob/main/release/leanback-python.apk?raw=true "TV-Python")  

### Config Example
[Vod-Online](other/vod-online.json)  
[Vod-Offline](other/vod-offline.json)  
[Live-Online](other/live-online.json)  
[Live-Offline](other/live-offline.json)  

### Subtitle Format
In playerContent put "sub"

#### Single
    title#MIME#url
#### Multiple
    title#MIME#url$$$title#MIME#url
#### MIME Type
    .srt = application/x-subrip
    .vtt = text/vtt
    .ass = text/x-ssa
#### Example
    Avatar#application/x-subrip#https://github.com/FongMi/TV/subtitle.srt
