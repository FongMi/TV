# APK 新版發布流程

本流程適用於 `D:\Project\TV` 的 `leanback`／`mobile` 正式版，以及獨立的 `D:\Project\TV\Release` 儲存庫。新版 APK 發布為 `FongMi/Release` 的 GitHub Release 附件；TV App 從同一筆 `releases/latest` API 回應讀取版本、說明及相符 ABI 的附件網址。

## 1. 確定版本與來源

1. 查詢 `FongMi/Release` 目前的最新正式 Release，選定更高的 `versionName`、`versionCode`。Release 標籤與 `versionName` 完全一致，例如 `5.6.6`／`566`；不要沿用已公開的版號替換 APK。
2. 在 `app/build.gradle` 設定版本。列出這次要納入的 TV、Media 與原生庫提交，記錄各儲存庫的 commit SHA；先檢查工作樹與遠端差異。不要把不相關的未提交修改、被忽略的 AAR 或其他本地提交默默算進發行版，也不要為了發布而 reset、clean 或 stash 它們。
3. 若 Media 庫有變更，在 `D:\Project\media` 執行 `.\gradlew.bat assembleLibs`，確認 `move.txt` 選定的 release AAR 已複製到 TV 的 `app/libs`，並逐一比對來源與目的檔的 SHA-256。若原生庫有變更，另外核對對應 `.so` 的來源、ABI 和封裝；`app/libs/lib-*.aar` 被 Git 忽略，不能用 `git status` 當交付證明。
4. 依此次改動執行有關的單元／儀器測試與 lint。測試紀錄須標出當前原始碼、AAR 與 APK 的對應關係；舊 APK 或舊裝置測試不能代替本次驗證。

## 2. 建置與核對四個 APK

在 TV 根目錄執行：

```powershell
.\gradlew.bat :app:assembleRelease
```

若 R8 因預設 2 GB heap 不足而失敗，可只為這次建置提高 Gradle heap 並限制工作緒；先記錄失敗原因，不要把上一輪 APK 當成本次產物。正式 APK 的來源是：

| 版本 | ABI | TV 建置輸出 |
| --- | --- | --- |
| 電視 | arm64-v8a | `app/build/outputs/apk/leanback/release/leanback-arm64_v8a.apk` |
| 電視 | armeabi-v7a | `app/build/outputs/apk/leanback/release/leanback-armeabi_v7a.apk` |
| 手機 | arm64-v8a | `app/build/outputs/apk/mobile/release/mobile-arm64_v8a.apk` |
| 手機 | armeabi-v7a | `app/build/outputs/apk/mobile/release/mobile-armeabi_v7a.apk` |

四個檔案都要逐一確認：

- 檔案是這次建置產生的；套件名稱、`versionCode`、`versionName`、ABI 與檔名相符。
- `apksigner verify --verbose --print-certs` 通過，簽章憑證指紋與上一個公開正式版相同；`zipalign` 與原生庫封裝也通過檢查。
- 記錄每個 APK 的大小與 SHA-256。使用目前未壓縮 `.so` 的封裝時，APK 可能超過 GitHub 一般 Git 檔案的大小限制；這四個檔案要作為 Release 附件上傳。

例如在 PowerShell 中：

```powershell
$apk = @(
  'app/build/outputs/apk/leanback/release/leanback-arm64_v8a.apk',
  'app/build/outputs/apk/leanback/release/leanback-armeabi_v7a.apk',
  'app/build/outputs/apk/mobile/release/mobile-arm64_v8a.apk',
  'app/build/outputs/apk/mobile/release/mobile-armeabi_v7a.apk'
)
$apk | ForEach-Object { Get-Item $_ | Select-Object FullName, Length, LastWriteTime; Get-FileHash $_ -Algorithm SHA256 }
```

使用本機 Android SDK 的 `aapt.exe dump badging`、`apksigner.bat verify --verbose --print-certs` 與 `zipalign.exe -c -P 16 4` 逐一檢查 `$apk`；任一指令失敗就停止發布。

## 3. 用本次 APK 驗證

先用自動測試覆蓋改動，再在相符裝置上驗證安裝、啟動、更新提示與下載安裝。電視以 `leanback`／`armeabi-v7a` 正式 APK 在實體 Android TV 驗證遙控焦點及代表性的 Live／VOD 播放；手機流程以對應 `mobile` APK 驗證。裝置操作前以 `adb devices -l` 和裝置屬性確認型號、ABI、serial，所有 ADB 指令都指定 serial。雷電模擬器先租用閒置實例，不碰其他任務的裝置。

更新測試要從**已公開、同簽章的舊版 APK**起步，確認保留資料的覆蓋安裝；debug 簽章安裝失敗不能當作正式升級失敗。記錄裝置、安裝包雜湊、版本、播放狀態及失敗日誌。沒有實機播放或升級證據時，在發布紀錄明說尚未驗證的項目。

## 4. 準備 Release 草稿

1. 更新 `Release/RELEASE.md` 的電視與手機說明；一個 GitHub Release 的 body 同時包含兩版重點。檢查 `Release` 儲存庫差異，此時只提交需要更新的說明，**不要把新 APK 提交到 `Release/apk` 的 Git 歷史**。該目錄仍保留舊版過渡 APK，不能把它誤認為這次的正式產物。
2. 檢查 TV 與 Release 各自要推送的提交及遠端狀態，確認來源 SHA 可追溯。GitHub Release 的 tag 建在 `FongMi/Release` 對應說明的提交上；發布紀錄另列 TV 與 Media 的來源 SHA。
3. 在前面四個 APK、測試、版本及簽章均核對後，建立 `5.6.6` 這類**新 tag 的草稿 Release**，以上述四個 TV 建置輸出作為附件。草稿要有四個精確檔名；不要使用 `--clobber` 覆蓋已公開附件，也不要把 `latest` 提前指向未核對的檔案。
4. 從草稿 API 核對每個附件的 `state=uploaded`、名稱、大小、SHA-256 digest 和 URL，並確認 Release body。草稿完整後才發布為正式、非 prerelease 的最新版本。

發布命令範例（先將 `$version`、`$releaseCommit` 設為本次核對過的值）：

```powershell
$version = '5.6.6'
$releaseCommit = git -C Release rev-parse HEAD
gh release create $version $apk -R FongMi/Release --draft --target $releaseCommit --title $version --notes-file Release/RELEASE.md
gh release view $version -R FongMi/Release --json tagName,isDraft,isPrerelease,assets
# 草稿檢查通過並確認要公開後：
gh release edit $version -R FongMi/Release --draft=false --latest
```

## 5. 公開後核對更新鏈

1. 查 `https://api.github.com/repos/FongMi/Release/releases/latest`：`tag_name` 必須等於本次版本，`body` 是本次說明，四個 `assets` 的名稱、`state`、大小、digest 和本地 APK 對得上。抽查附件實際下載與安裝。新版 App 使用**同一筆 API 回應**中的 `browser_download_url`，不另組 `latest/download`，也不讀 JSON。
2. 舊版入口要分開處理。`Release/apk/*.json` 與其中的 `5.6.4` APK 是最舊客戶端的過渡路徑；`Release/apk/release/*.json` 是後續仍讀 JSON 的客戶端入口。先從對應**已公開 APK**確認它實際讀取哪個 JSON、下載哪個 URL，再決定是否把該入口更新到新版本。若 `5.6.5` 客戶端仍讀第二組 JSON，應在新附件可下載後才把其 `code/name/desc` 更新為新版本，並驗證它下載的是新附件。不要因 `5.6.6` 自身不用 JSON 就刪除舊入口。
3. 用舊版與新版各走一次更新檢查，確認顯示版號、說明、ABI 與下載的 APK 一致。GitHub Raw 可能有快取，核對公開 URL 的實際回應後再宣布舊版更新路徑可用。
4. 留下發布紀錄：TV／Media／Release commit、tag、四個 APK 的 SHA-256、GitHub 附件 digest、測試命令及裝置證據。若任何檢查失敗，保留草稿或停止舊 JSON 切換，先修正再公開；已公開的壞版以**更高版號**修復。

`2026-09-27` 現況：GitHub 最新正式 Release 為 `5.6.5`；TV 工作樹正在準備 `5.6.6 / 566`，但尚非已驗證發布包。每次執行本流程都要重新查最新 Release 與工作樹，不能沿用這個快照。
