# mpv 腳本按鈕提交邊界（2026-09-24）

本組只加入手機與 Leanback 的 mpv 腳本快捷列、管理對話框及 VOD 控制列的焦點捲動。既有 `MpvScripts`、`MpvScriptSession`、`PlayerManager` 腳本 API 已在前面的提交，不改動腳本執行或載入規則。按鈕點擊時才從目前 `PlaybackActivity` 取播放器；非 mpv 引擎時隱藏快捷列。

`PlaybackActivity.getPlaybackPlayer()` 是本組唯一共用活動 API 增量。四個控制列 XML 僅暫存腳本快捷列相關 hunk；解碼按鈕移除、ISO 選單仍留在工作樹，未混入本提交。Leanback VOD 改用 `CustomHorizontalScrollView`，讓水平方向的顯式焦點連結可在捲動列上生效。對話框返回回呼只處理內頁返回，根頁仍交給原有 dialog 關閉行為。

隔離工作樹只套用本組暫存 patch 後，`:app:compileMobileDebugJavaWithJavac` 與 `:app:compileLeanbackDebugJavaWithJavac` 通過。這是編譯證據，尚未在手機或實體電視驗證腳本載入、點擊、焦點與返回動畫；不得宣稱裝置互動已通過。
