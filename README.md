# 影視

### 基於 CatVod 項目

https://github.com/CatVodTVOfficial/CatVodTVJarLoader

### 點播欄位

| 欄位名稱       | 預設值  | 說明   | 其他               |
|------------|------|------|------------------|
| searchable | 1    | 是否搜索 | 0：關閉；1：啟用        |
| changeable | 1    | 是否換源 | 0：關閉；1：啟用        |
| recordable | 1    | 是否紀錄 | 0：關閉；1：啟用        |
| playerType | none | 播放器  | 0：系統；1：IJK；2：EXO |
| timeout    | 15   | 播放超時 | 單位：秒             |
| header     | none | 請求標頭 | 格式：json          |
| click      | none | 點擊js | javascript       |

### 直播欄位

| 欄位名稱       | 預設值   | 說明    | 其他               |
|------------|-------|-------|------------------|
| ua         | none  | 用戶代理  |                  |
| origin     | none  | 來源    |                  |
| referer    | none  | 參照地址  |                  |
| epg        | none  | 節目地址  |                  |
| logo       | none  | 台標地址  |                  |
| pass       | false | 是否免密碼 |                  |
| boot       | false | 是否自啟動 |                  |
| playerType | none  | 播放器   | 0：系統；1：IJK；2：EXO |
| timeout    | 15    | 播放超時  | 單位：秒             |
| header     | none  | 請求標頭  | 格式：json          |
| click      | none  | 點擊js  | javascript       |

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

配置 rules 新增 proxy 判斷 host 是否走代理

```json
{
  "name": "proxy",
  "hosts": [
    "api.nivodz.com"
  ]
}
```

### 配置範例

[點播-線上](other/sample/vod/online.json)  
[點播-本地](other/sample/vod/offline.json)  
[直播-線上](other/sample/live/online.json)  
[直播-本地](other/sample/live/offline.json)

### 飛機群

[討論群組](https://t.me/fongmi_offical)  
[發布頻道](https://t.me/fongmi_release)

### 贊助
![photo_2024-01-10_11-39-12](https://github.com/FongMi/TV/assets/3471963/fdc12771-386c-4d5d-9a4d-d0bec0276fa7)

