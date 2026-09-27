# 音訊／影像設定對話框版面 — 2026-09-24

手機與 Leanback 的音訊、影像設定版面改用既有 `PlaybackDialogHeader`／`Title`／`Action`／`Tabs`／`Body` 樣式，收斂標題、按鈕與分頁留白；分類標籤補 4dp 下方空間。音訊的 loudness 說明與 switch 改排在同一列，只有 switch 接收切換；switch 加上可及性名稱。

Leanback 根 view 改為 `CustomTabLayout`：方向鍵從其他區域回到 tab 或 chip 群組時，先找目前已選項；群組內方向鍵仍交由既有順序處理。這是焦點行為變更，需要在電視實測上下左右；編譯及版面資源處理只驗證型別、ID 與資源引用，不能代替視覺／遙控器驗證。
