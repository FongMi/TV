# TV

### Based on CatVod  
https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### Download
[TV-Java](https://github.com/FongMi/TV/blob/main/release/leanback-java.apk?raw=true "TV-Java")  
[TV-Python](https://github.com/FongMi/TV/blob/main/release/leanback-python.apk?raw=true "TV-Python")  

### Local Config Example
    file://cat.json

```json
{
   "spider":"spider.jar",
   "sites":[
      {
         "key":"one",
         "name":"One",
         "type":3,
         "api":"csp_Csp",
         "searchable":1,
         "quickSearch":1,
         "filterable":1,
         "ext":"file://one.json"
      },
      {
         "key":"two",
         "name":"Two",
         "type":3,
         "api":"csp_XPath",
         "searchable":1,
         "quickSearch":1,
         "filterable":1,
         "ext":"two.json"
      }
   ]
}
```
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
