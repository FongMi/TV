# 影視

### 基於 CatVod 項目

https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### 點播欄位

| 欄位名稱       | 預設值  | 說明   | 其他         |
|------------|------|------|------------|
| searchable | 1    | 是否搜索 | 0：關閉；1：啟用  |
| changeable | 1    | 是否換源 | 0：關閉；1：啟用  |
| quickserch | 1    | 是否快搜 | 0：關閉；1：啟用  |
| indexs     | 0    | 是否聚搜 | 0：關閉；1：啟用  |
| hide       | 0    | 是否隱藏 | 0：顯示；1：隱藏  |
| timeout    | 15   | 播放超時 | 單位：秒       |
| header     | none | 請求標頭 | 格式：json    |
| click      | none | 點擊js | javascript |

### 直播欄位

| 欄位名稱     | 預設值   | 說明    | 其他         |
|----------|-------|-------|------------|
| ua       | none  | 用戶代理  |            |
| origin   | none  | 來源    |            |
| referer  | none  | 參照地址  |            |
| epg      | none  | 節目地址  |            |
| logo     | none  | 台標地址  |            |
| pass     | false | 是否免密碼 |            |
| boot     | false | 是否自啟動 |            |
| timeout  | 15    | 播放超時  | 單位：秒       |
| header   | none  | 請求標頭  | 格式：json    |
| click    | none  | 點擊js  | javascript |
| catchup  | none  | 回看參數  |            |
| timeZone | none  | 時區    |            |

### 樣式

| 欄位名稱  | 值    | 說明  |
|-------|------|-----|
| type  | rect | 矩形  |
|       | oval | 橢圓  |
|       | list | 列表  |
| ratio | 0.75 | 3：4 |
|       | 1.33 | 4：3 |

直式

```json
{
  "style": {
    "type": "rect"
  }
}
```

橫式

```json
{
  "style": {
    "type": "rect",
    "ratio": 1.33
  }
}
```

正方

```json
{
  "style": {
    "type": "rect",
    "ratio": 1
  }
}
```

正圓

```json
{
  "style": {
    "type": "oval"
  }
}
```

橢圓

```json
{
  "style": {
    "type": "oval",
    "ratio": 1.1
  }
}
```

### API

刷新詳情

```
http://127.0.0.1:9978/action?do=refresh&type=detail
```

刷新播放

```
http://127.0.0.1:9978/action?do=refresh&type=player
```

刷新直播

```
http://127.0.0.1:9978/action?do=refresh&type=live
```

推送字幕

```
http://127.0.0.1:9978/action?do=refresh&type=subtitle&path=http://xxx
```

推送彈幕

```
http://127.0.0.1:9978/action?do=refresh&type=danmaku&path=http://xxx
```

新增緩存字串

```
http://127.0.0.1:9978/cache?do=set&key=xxx&value=xxx
```

取得緩存字串

```
http://127.0.0.1:9978/cache?do=get&key=xxx
```

刪除緩存字串

```
http://127.0.0.1:9978/cache?do=del&key=xxx
```

### Proxy

scheme 支持 http, https, socks4, socks5

```
scheme://username:password@host:port
```

配置新增 proxy 判斷域名是否走代理  
全局只需要加上一條規則 ".*."

```json
{
  "spider": "",
  "proxy": [
    "raw.githubusercontent.com",
    "googlevideo.com"
  ]
}
```

### Hosts

```json
{
  "spider": "",
  "hosts": [
    "cache.ott.*.itv.cmvideo.cn=base-v4-free-mghy.e.cdn.chinamobile.com"
  ]
}
```

### Headers

```json
{
  "spider": "",
  "headers": [
    {
      "host": "gslbserv.itv.cmvideo.cn",
      "header": {
        "User-Agent": "okhttp/3.12.13",
        "Referer": "test"
      }
    }
  ]
}
```

### 爬蟲本地代理

Java

```
proxy://
```

```
Proxy.getUrl(boolean local)
```

Python

```
proxy://do=py
```

```
getProxyUrl(boolean local)
```

JS

```
proxy://do=js
```

```
getProxy(boolean local)
```

### 配置範例

[點播-線上](other/sample/vod/online.json)  
[點播-本地](other/sample/vod/offline.json)  
[直播-線上](other/sample/live/online.json)  
[直播-本地](other/sample/live/offline.json)

### 飛機群

[討論群組](https://t.me/+qTlg0qAVzP9kMmM1)  
[發布頻道](https://t.me/fongmi_release)

### 贊助

![photo_2024-01-10_11-39-12](https://github.com/FongMi/TV/assets/3471963/fdc12771-386c-4d5d-9a4d-d0bec0276fa7)

### Star

[![Star History Chart](https://api.star-history.com/svg?repos=FongMi/TV&type=Date)](https://www.star-history.com/#FongMi/TV&Date)
