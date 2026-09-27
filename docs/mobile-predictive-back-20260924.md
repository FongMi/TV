# 手機版返回手勢版本設定 — 2026-09-24

手機 Manifest 將 `android:enableOnBackInvokedCallback` 指向資源值：預設 `true`，API 34 覆寫為 `false`，API 35 以上覆寫為 `true`。此設定只在 mobile source set，Leanback 不受影響；它會影響手機整個 app 的返回手勢，不只 mpv 腳本對話框。

這組只提交 Manifest 與三個布林資源，不修改對話框 callback 或播放邏輯。合併 Manifest、手機 Java 編譯與 Leanback Java 編譯可檢查資源／建置，但無法證明各 Android 版本的手勢動畫效果；仍需在對應裝置上測試。
