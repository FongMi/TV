# 字幕／彈幕設定對話框版面 — 2026-09-24

手機與 Leanback 的字幕、彈幕設定外層版面改用既有 PlaybackDialog 樣式，收斂標題、按鈕、分頁與內文留白；分頁、內容 view ID 和可見狀態不變。Leanback 根 view 改用 `CustomTabLayout`，方向鍵從內容回到分頁時優先已選項；手機仍是原本的 LinearLayout。

這一組不修改字幕／彈幕設定資料或播放器行為，但 Leanback 焦點可能改變。資源處理與兩個版型 Java 編譯通過後，仍需電視上逐頁實測上下左右焦點與視覺間距。
