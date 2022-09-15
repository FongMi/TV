# TV

### Based on CatVod  
https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### Download
[TV](https://github.com/FongMi/TV/blob/main/release/leanback.apk?raw=true "leanback.apk")  
[Mobile](https://github.com/FongMi/TV/ "mobile.apk")  ...incoming

### How to build
Use dev branch

### Config Example
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
