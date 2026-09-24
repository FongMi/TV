# 字幕／彈幕搜尋對話框 — 2026-09-24

Leanback 的搜尋輸入框使用遙控器專用 `CustomEditText`，並明確連接右側設定按鈕與左側輸入框的焦點。游標在文字尾端按右鍵時由 `CustomEditText.onKeyDown()` 導向設定按鈕；兩個對話框重複的 `OnKeyListener` 攔截已移除，向下到結果清單的原有處理保留。

設定按鈕改用現有 `DialogActionView` 提供提示及可及性名稱；Leanback 的字幕／彈幕按鈕統一 40dp 與 chip 焦點背景，手機維持 borderless 背景。結果 RecyclerView 停用巢狀捲動，配合對話框內短結果清單。這些是焦點、觸控與捲動行為變更；編譯無法證明實際手勢或遙控器體驗，需裝置驗證。
