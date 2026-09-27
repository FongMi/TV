# TV 提交整理紀錄 — 2026-09-24

## 備份與邊界

- 整理前 HEAD：`093604ddaa3f6d9c223bdbb01790882fb61bca1b`，分支 `fongmi`；相對本機 `origin/fongmi` 超前 94 筆提交。
- 備份參照：`refs/backups/tv-pre-organize-20260924-132539`。
- 異動備份：`D:\Temp\TV-commit-backup-20260924-132539`，含 `tracked.patch`、140 個實存異動／新增檔案的原樣複本及 SHA-256 清單；另有 2 個刪除路徑記在 patch 中。
- `tracked.patch` SHA-256：`53AF58B5A4BA5A60B4E21BDE55A170AA0B2DF29CEC13F8AC4CE2E166C77C7BF5`。複製後逐檔比對 SHA-256，且備份前後 HEAD 與工作樹狀態一致。
- 未追蹤的 `app/src/androidTest/java/com/fongmi/android/tv/ui/custom/BaseInputSeekTapTest.java` 也已備份，但不得提交。未經逐功能核對，不把其他工作樹異動混入提交。
- 最後一輪拆分前另存 `D:\Temp\TV-worktree-backup-20260924-172206`：包含 `tracked.patch`、26 份原樣檔案與 `manifest.csv`；備份前後 HEAD／工作樹狀態相同。原始 patch 的 SHA-256 為 `51A99BD9846178B5912566C837C37BCD7E598493489586FBD5DD4ACD4E92B12B`。

## 既有 94 筆本地提交的重排

第一輪逐筆核對提交標題與檔案範圍；大範圍的版面拆分、略過片段、Node runtime 與跨模組 keep rules 均屬各自單一功能。既有 94 筆沒有新增測試檔案。只移動兩筆有具體依賴理由的提交，其餘保留原順序：

1. `Add isolated NodeJS crawler runtime library` 從第 49 筆移到 WebView／QuickJS 基礎提交後、VOD Node 接線前，使 Node 主體與接線相鄰；跨越的 8 筆提交與它沒有共同修改路徑。
2. `Expose danmaku clearing to Node runtime` 從第 69 筆移到 VOD Node 接線前。原本 `Connect NodeJS crawler runtime to VOD configuration` 已呼叫 `PlayerManager.clearDanmaku()`，方法卻在後面的提交才加入，形成中間提交的前向依賴。先加入方法再接線，功能邊界不變。

重排完成時的 HEAD：`400720fb6d6d4c47f7e9ec496c1a0ad1cb15dc5a`。舊 HEAD 仍由上述備份參照保留。兩邊都有 94 筆本地提交；`git range-diff` 顯示 94/94 patch 相同，沒有增刪或改寫 patch；兩邊最終 tree 都是 `28dacd33d64da0d9841b5b25279741f5ab7626ba`，`git diff --exit-code` 為空。更新 `fongmi` 指標後，暫存區仍為空，原有 142 個工作樹異動路徑仍在，未推送。

隔離工作樹以主工作樹相同的本機 AAR 編譯最終 tree，手機與 Leanback Java 均通過。嘗試編譯較早的 Node 接線中間提交時，當前 `lib-mpvplayer-release.aar` 已沒有該歷史原始碼要求的 `setDecode(int)`，因此失敗與本次 Node 排序無法直接歸因；不把該次建置當成前向依賴已被完整動態驗證。

## 功能分組（待逐項確認）

| 範圍 | 提交邊界 |
| --- | --- |
| NodeJS 更新回退提示 | Node bundle/client 的原因傳遞、VOD 顯示、必要的字串資源；與純程式碼去重分開。 |
| NodeJS 純整理 | 下載函式、SHA-256 與 Mapper 的等價去重；不得改變更新或回退條件。 |
| 播放與解碼 | 需再按手機／Leanback、解碼、字幕、track、mpv、ISO 等功能切開；共用檔案須按 hunk 核對。 |
| UI 與輸入 | 需確認對話框、焦點、輸入、播放內容與手機雙擊快轉的依賴後再提交。 |

截至本紀錄提交時，另有三組已獨立提交：`1e8b4290d` 僅傳遞並顯示 Node 舊版回退原因，`9e7ddeb9b` 縮減 Node helper 重複碼並區分 MD5/manifest 過大訊息，`775437799` 將遙控器專用 `CustomEditText` 移至 Leanback 並修正焦點處理。每組都先核對暫存 patch 與測試檔排除，再編譯手機與 Leanback；Node 兩組另通過 NodeJS lint。尚未做實體裝置互動或回退情境驗證。

## 已發現的邊界問題

`NodeClient.load()` 對新版 bundle 下載、啟動及 `/config` 驗證失敗有舊版回退。若錯誤發生在較後面的 `VodConfig.checkJson()`／`parseConfig()`，目前 `VodConfig.load()` 會呼叫 `rejectPending(loaded)` 後直接拋錯；`NodeBundle.rollback()` 雖會把舊 bundle 移回 active，`NodeClient.rejectPending()` 隨後仍會清掉執行中的 runtime，並未在同一次載入重新啟動舊版。`BaseConfig.loadConfig()` 會將錯誤送到畫面，但 `parseConfig()` 若已部分執行，也沒有整體回滾。既有畫面資料可能仍在，Node 站點要等下一次設定載入才能恢復。這是原始碼流程推論，尚未做裝置重現；在修正或明確接受此行為前，不將「所有新版載入失敗都可立即使用舊版」列為已驗證。

`CustomEditText` 從共用 source set 移到 Leanback source set 時還包含焦點行為修正：焦點搜尋改用 `View.focusSearch()`、有修飾鍵時不攔截方向鍵、只有真正取得下一焦點才消耗按鍵，並要求文字選取的頭尾兩端都在邊界。手機版沒有該類別的引用；這一組不是單純搬檔，需獨立提交並保留實體電視方向鍵驗證項目。

未提交的 `TrackUtil.describeFormat()` 改動原本把 `averageBitrate` 換成 `Format.bitrate`，並用 `Format.NO_VALUE` 取代 sample-rate sentinel。查本機 Media3 原始碼後確認 `Format.bitrate` 優先採峰值，`Format.NO_VALUE` 為 -1，而 `C.RATE_UNSET_INT` 不是 -1。這些變更會改動儲存在 Track 資料庫中的描述字串；`TrackUtil.find()` 與 Exo 軌道選擇會用該字串精確匹配舊紀錄，檔內也明示須與資料庫 v36 前相容。因此這組未提交改動已撤回為原本邏輯；若日後要修正未知 sample rate 的描述，需另設相容讀取或資料遷移，不應在本次提交整理中偷偷更改軌道身分。

## 驗證原則

每組提交前檢查 staged diff、`git diff --cached --check`、對應編譯／lint，並核對沒有測試檔案。邏輯變動或新發現的問題更新本紀錄；不以編譯成功代替裝置驗證，也不推送到 GitHub。

## 後續未提交改動的分界（2026-09-24 再核對）

目前共用的 `PlayerManager`、`PlaybackActivity`、`ExoPlayerEngine`、`PlaybackService`、手機與 Leanback `VideoActivity` 同時含多個功能，不可整檔加入任何單一提交。後續至少需按以下呼叫流程分組並做 staged-only 編譯：

| 功能 | 主要待拆範圍 | 邊界／驗證 |
| --- | --- | --- |
| 解碼模式與錯誤回退 | `DecodeSetting`、`ExoDecoderFallback`、`ExoPlayerSession`、引擎 API、track／解碼對話框、設定畫面、相關字串及 `PlayerManager` 分段 | 新播放自動、播放中暫時切換、音視訊分軌與錯誤重試須連同 UI 核對；不可混入 ISO 或字幕。 |
| ISO／BD-J 選單 | `DiscMenuController`、overlay、Exo navigation、VOD 歷史、活動與 action 按鈕及共用播放類別分段 | 與 Media3 ISO 提交和 AAR 契約對照；不能以目前全工作樹可編譯代替中間提交可用。 |
| Web 播放與刷新 | `PlaybackActivity`、`PlaybackService`、兩版 Live／Video 活動和 `PlaybackAction` 分段 | 另核對外部播放的通知控制、暫停／續播、錯誤刷新及活動生命週期。 |
| 字幕內容與偏移 | `ExoMediaSourceFactory`、字幕控制器、`SubtitleSettingPanel`、`SubtitleOffsetPanel`／util、track UI 和相關版面 | 字幕載入、雙字幕、逐字內容及角色標示需分成相依提交；不混入解碼選單。 |
| mpv 腳本操作 | 新腳本 dialog／panel／view、兩版 layout、`MpvPlayerEngine` 與活動接線分段 | 返回動畫、按鈕狀態及播放中腳本路徑需要裝置驗證。 |
| 手機觸控與倍速手勢 | `TouchInput`、`TapSeekFeedback`、`CustomKeyDown` 刪除、手機 Live／Video 活動與控制版面分段 | 手機雙擊快轉是新功能，不能視為舊行為清理；避免跨入 Leanback 遙控器行為。 |
| 子清單捲動 | chapter／device／filter／parse／restore 等對話框 XML，另含手機選集 `fragment_episode` | 對話框與選集已按觸控用途分別提交；改用 `CustomRecyclerView` 會影響觸控及焦點，未取得裝置證據前不宣稱互動已驗證。 |

解碼規則已由使用者明確確認：每次進入 Video／Live 從自動開始；播放中的手動切換只限當次，不寫入偏好。`1d3ebe564` 已將解碼 API、UI 與 session 重設獨立提交；`PlaybackService.claimBinding()` 觸發重設，Exo／mpv 新實例也預設自動。重設不再共用可能對舊錯誤媒體執行 `prepare()` 的手動切換路徑。新片源清除自動 fallback 嘗試紀錄，同片源錯誤重試則保留紀錄。該提交在隔離工作樹通過 mobile／Leanback Debug Java 編譯與 `git diff --check`；未做實體裝置播放驗證。

`app/src/androidTest` 與 `app/build.gradle` 中的測試配置仍在工作樹，依要求不加入提交。整份工作樹的 mobile／Leanback Java 編譯通過，僅證明目前合併狀態可建置；各功能提交仍須各自隔離驗證。

`2a799e2fb` 已將字幕核心與設定面板獨立提交：副字幕位置滑桿從「進階」移到「調整」；字幕時間偏移改為 100 ms 步進，主／副字幕可各自調整或連動，重設時依目前啟用的字幕提供選擇。Exo／mpv 接入字幕選擇狀態、雙路偏移與字幕內容來源；新片源會重新套用副字幕模式。這些是明確的行為變更，不應視為純排版整理。該候選在隔離工作樹通過 mobile／Leanback Debug Java 編譯及 `git diff --check`；尚未驗證實機的滑桿、焦點、字幕內容及跨片源行為。

`6e0af6136` 已將字幕／彈幕內容瀏覽列表、搜尋、跟隨目前時間、重試字幕內容和手機／Leanback 入口獨立提交；未包含字幕角色徽章、ISO 選單或手機選集捲動變更。彈幕來源的 Media3 timeline 僅在內容變動時更新快照，面板週期更新不會每次複製整份列表。此組在隔離工作樹通過兩版 Debug Java 編譯與 `git diff --check`，但未實測瀏覽列表間距、焦點方向、搜尋輸入、手勢返回或彈幕流暢度，不能以建置通過視為畫面驗證。

`26a719f6f` 已將主／副字幕軌道角色徽章與選擇還原獨立提交。使用者切換軌道後才保存選擇；新片源第一次取得軌道時依紀錄還原，暫時缺席的軌道在後續軌道更新時重試。副字幕被提升為主字幕時同步更新兩個角色的紀錄。此處記憶的是軌道選擇，與解碼模式不同；解碼仍依使用者最新確認，每次進入 Video／Live 從自動開始。`TrackDialog` 的 `showNow()` 與初始焦點時序改動留在工作樹，未混入本提交。此組在隔離工作樹通過 mobile／Leanback Debug Java 編譯，暫存 patch 與隔離工作樹 patch 相同且 `git diff --check` 通過；主／副字幕實際還原及電視焦點尚未做裝置驗證。

`cefb97120` 將 Exo 不良 HTTP 狀態時重新向 Live／VOD／Cast 來源取得播放位址的流程獨立提交：同一次未成功的載入最多刷新一次，成功進入 READY 或使用者重設／切換來源才清除標記；刷新後仍失敗就回報播放錯誤。審查時發現 DLNA 也共用 `PlaybackService`，但沒有刷新處理者，原未提交寫法會吞掉第一次 HTTP 錯誤；因此刷新回呼改為回報是否由播放頁接手，無處理者直接顯示原錯誤。這是刻意的邏輯修正，不是單純拆提交。此組在隔離工作樹通過 mobile／Leanback Debug Java 編譯、patch 等同性與 `git diff --check`；未以真實失效 URL 或 DLNA 裝置驗證。

## Web 播放提交依賴再核對（2026-09-24）

上表的 mpv 腳本 UI 已由 `1a4cba406` 獨立提交；手機對話框剩餘清單觸控設定已由 `50b99f9cd` 獨立提交，後者的兩個 XML 仍有其他功能 hunk 留在工作樹。

`BrowserWebView` 與 `UrlUtil` 的 `webview://` 辨識已在既有提交，未提交的是 UI／服務接線：`PlaybackActivity` 管理 WebView 生命週期、觸控與挑戰頁，`PlaybackService` 將外部播放的播放／暫停／換集轉回活動，手機及 Leanback 的 Live／Video Activity 接入入口、控制列、進度與返回。只提交其中一個活動或服務回呼都不能代表 Web 播放功能完成。

目前 `PlaybackActivity.startWebPlayback()` 會呼叫 ISO 選單請求清理，隱藏原生播放器時會處理 ISO overlay；兩版 Video Activity 的播放狀態、進度與按鍵 hunk 也交疊 ISO。ISO／Media3 下沉仍由另一任務處理，因此 Web 提交要等這層契約確定，或先明確拆成 Web 基礎與後續 ISO 整合，不能把整個共用類別一次加入 Web 提交。這是原始碼依賴核對，不是 Web 裝置播放或通知控制已通過的證據。

服務動作仍有待核對的缺口：`PlaybackService.handleAction()` 接收應用內 `ActionEvent.PLAY`／`PAUSE` 時會轉給 Web 活動，但 MediaSession 的 `ForwardingPlayer` 只覆寫換集、停止等命令，沒有覆寫外部控制器的 `play()`／`pause()`；`startWebPlayback()` 又會清空原生 Player。依目前原始碼，不能推定系統媒體控制器的播放狀態與按鍵會控制 Web 影片。這是靜態流程判斷，尚未用外部媒體控制器重現，不在整理提交時偷偷補一套新的狀態代理。

Web 基礎接線已先拆出 `PlaybackService` 對外部播放活動的動作轉接，以及 `PlaybackAction` 隱藏原生專用控制的 helper；未加入 ISO 的 `onBdjPreparing()`，也未加入手機長按倍速改動。活動尚未接上前，外部播放判斷預設為 `false`，原生播放動作維持原路徑。候選 tree 在隔離工作樹通過 mobile／Leanback Debug Java 編譯，`git diff --cached --check` 通過且沒有測試檔；這不代表 Web 影片或系統媒體控制已在裝置驗證。

track 角色徽章與還原已由 `26a719f6f` 提交；`TrackDialog` 的開啟與焦點時序另列獨立功能：`show()` 換成同步 `showNow()`，Leanback 開啟時先聚焦對話框根節點，再於首次繪製前嘗試聚焦選取軌道。品質核對發現目標 item 若尚未建立或拒絕焦點，原改動會撤掉根節點可聚焦狀態而沒有後續聚焦；現在退回既有 `CustomRecyclerView.scrollToPosition()` 的延遲聚焦路徑。手機版則只捲到選取軌道，不再延遲搶焦點。這是邏輯修正，不是純重構；電視方向鍵上下左右與首次焦點仍需裝置驗證。

`94403d68c` 已將手機選集 `fragment_episode` 獨立提交：它位於 `EpisodeGridDialog` 的 `ViewPager2` 內，換成 `CustomRecyclerView` 後會依水平／垂直滑動方向改變父層觸控攔截，且 `EpisodeFragment` 初始 `scrollToPosition()` 會額外延遲 50 ms 要求項目焦點；`nestedScrollingEnabled="false"` 另改變巢狀捲動協調。候選 tree 通過隔離工作樹的 mobile Debug Java 編譯與 `git diff --cached --check`，沒有測試檔；選集頁垂直捲動、左右切頁、首次開啟焦點及點擊仍未做裝置驗證。

## 再驗證與剩餘分界

在 `1ba6174de` 時再核對：整理前備份與重排後第 94 筆提交各有 94 筆本地提交，`git range-diff` 全部 94 筆 patch 相同，兩邊 tree 均為 `28dacd33d64da0d9841b5b25279741f5ab7626ba`，直接 tree diff 為空。`D:\Temp\TV-commit-backup-20260924-132539` 的 `tracked.patch` SHA-256 仍與上方紀錄一致，manifest 的 140 份檔案重新比對均相符。至該 HEAD 的 131 筆本地提交涉及 358 個相異路徑，沒有測試檔路徑，`git log --check` 沒有 whitespace 問題。

前面各節對「未提交」功能的描述是當時工作樹快照；後續已依相依順序拆完，不能再當作目前待提交清單。

## 最後一輪功能收斂（截至 `f22f87250`）

`135b07599` 將共用輸入、手機雙擊快轉回饋與長按倍速還原收在同一提交；手機雙擊快轉是保留的新功能，不是回退。手機和 Leanback Debug Java 在隔離工作樹編譯通過，但尚未實測觸控、方向鍵、長按倍速與焦點。

| 提交 | 明確的邏輯變動 | 驗證邊界 |
| --- | --- | --- |
| `3ad710c4c` | 接入 ISO／BD-J 選單、overlay、引擎導覽及 VOD 歷史。 | TV 候選的兩版 Debug Java 編譯通過；實際 BD-J 裝置操作未在本輪 TV 候選驗證。Media3 原始碼與 AAR 由另一任務處理，不能把該任務的雷電結果視為本提交實測。 |
| `6d8cda90b` | mpv 取得 DRM 授權失敗時，同一媒體最多切換一次 ExoPlayer；新媒體清除嘗試標記。 | 兩版 Debug Java 編譯通過；未用真實 DRM 來源驗證。 |
| `4307941b0` | VOD 片頭／片尾按鈕文字改讀目前設定的跳過秒數，不再使用舊歷史快照。 | 兩版 Debug Java 編譯通過；未實測畫面。 |
| `7422d5062` | 不再於播放活動用 Android `MediaDrm` 提前阻擋 DRM scheme，交由選定的 mpv／Exo 引擎判定。 | 兩版 Debug Java 編譯通過；未用 ClearKey 片源實測。 |
| `f22f87250` | WebView 播放接入手機與 Leanback 的 Live／Video 畫面、進度、暫停續播、挑戰頁與返回流程。 | 兩版 Debug Java 編譯通過；未在裝置驗證 Web 播放。 |

Web 的應用內 `PLAY`／`PAUSE` 已轉發活動，但 MediaSession 外部控制器仍經 `ForwardingPlayer`，沒有 Web 專用 `play()`／`pause()` 狀態代理；先前靜態檢查的缺口仍存在，不宣稱通知或外部遙控可控制 Web 影片。本輪沒有擴大功能去實作代理。

每個候選提交均用隔離工作樹編譯 mobile／Leanback Debug Java，且 staged diff 無 whitespace 錯誤、無測試檔案。`app/build.gradle` 的測試配置與 `app/src/androidTest` 仍刻意留在工作樹、不提交；`PlayerManager.reset()` 上方一行既有未提交註解也留在工作樹，未混入功能提交。解碼維持每次新 Video／Live 從自動開始、播放中的切換僅限當次。未在本輪對手機、電視或雷電安裝並執行這些 TV 候選提交；編譯結果不等於播放或 UI 驗證。
