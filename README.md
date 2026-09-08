# 影視TV

適用於 Android TV 與手機的影音應用程式，整合媒體瀏覽與播放體驗，並支援外部配置與 [CatVod](https://github.com/CatVodTVOfficial/CatVodTVJarLoader) Spider 介面擴充。

**App 本身不內建或提供任何內容來源。** 外部內容需自行配置，也可開啟本地媒體檔案或推送媒體網址。

[使用與開發指南](https://fongmi.github.io/TV/) · [討論群組](https://t.me/fongmi_official)

## 開始使用

1. 安裝適合裝置的 APK：`leanback` 為電視版，`mobile` 為手機版；依 Android 系統支援的 ABI 選擇 `arm64-v8a` 或 `armeabi-v7a`。最低需求為 Android 7.0（API 24）。
2. 在設定中加入自己的配置，格式與欄位見[配置範例](https://fongmi.github.io/TV/config/#examples)。
3. 也可從系統檔案管理員開啟媒體檔案，或透過推送入口播放媒體網址。

## 主要功能

- **播放**：Media3／ExoPlayer、mpv、硬解與 FFmpeg 軟解；字幕、彈幕、音軌、倍速與片頭／片尾跳過。
- **瀏覽與管理**：分類篩選、搜尋、播放記錄、收藏與無痕模式。
- **播放清單**：M3U／TXT／JSON 格式、清單分組與 XMLTV 節目資訊。
- **操作**：電視遙控器、手機手勢、畫中畫與背景音訊。
- **互通**：DLNA 投放／接收、Android Auto、本地 HTTP 控制與裝置同步。

實際能力依配置、媒體、播放引擎與裝置而異；本地 HTTP API 僅供可信任區域網路使用，不要直接轉發到公網。

## 開發文件

| 文件 | 內容 |
| --- | --- |
| [App 功能](https://fongmi.github.io/TV/features/) | 操作與功能介紹 |
| [配置字典](https://fongmi.github.io/TV/config/) | 配置欄位、網路設定與 JSON 範例 |
| [擴充介接](https://fongmi.github.io/TV/spider/) | Java／Python／JavaScript 範例、方法與回傳格式 |
| [本地 API](https://fongmi.github.io/TV/local/) | 播放控制、推送、檔案與同步端點 |
| [網站維護](website/README.md) | 靜態網站建置與 GitHub Pages 發布 |

`app/src/main/` 為共用邏輯，`app/src/leanback/`、`app/src/mobile/` 為各自的 UI。模組清單見 [settings.gradle](settings.gradle)，SDK 與依賴版本見 [libs.versions.toml](gradle/libs.versions.toml)。

## Windows 建置

先準備以下環境與檔案：

- **JDK 21、Android SDK、Python 3.10**。SDK 平台版本依 `compileSdk` 設定；Python 可用 `py -3.10 --version` 確認，找不到時在 [chaquo/build.gradle](chaquo/build.gradle) 的 Python 區塊設定 `buildPython`。
- **配套 AAR**：放入 `app/libs/`。`lib-*.aar` 未納入 Git，單純 clone 不包含完整播放器依賴。
- **自己的簽章檔與 `local.properties`**：在儲存庫根目錄建立下列設定，將所有範例值替換成自己的資料。

```properties
sdk.dir=C:/Android/Sdk
storeFile=C:/keys/yingshi-tv.jks
keyAlias=your-key-alias
storePassword=your-keystore-password
```

金鑰密碼與 keystore 密碼共用 `storePassword`；不要提交簽章檔或真實密碼。

在儲存庫根目錄以 PowerShell 執行：

```powershell
# 電視版
.\gradlew.bat :app:assembleLeanbackRelease

# 手機版
.\gradlew.bat :app:assembleMobileRelease
```

APK 按 ABI 分包並輸出至 `Release/apk/`。簽章不同的 APK 不能直接覆蓋既有安裝。網站位於 `website/`，可獨立建置，不需編譯 Android App。

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=FongMi/TV&type=Date)](https://www.star-history.com/#FongMi/TV&Date)
