# TV

### Based on CatVod  
https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### Download
[TV](https://github.com/FongMi/TV/blob/main/release/leanback.apk?raw=true "leanback.apk")  
[Mobile](https://github.com/FongMi/TV/ "mobile.apk")  ...incoming

### How to build
Use dev branch

### Local Example
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
