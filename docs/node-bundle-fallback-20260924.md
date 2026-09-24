# Node bundle 回退提示 — 2026-09-24

## 本次邏輯

新版 Node bundle 下載／校驗失敗而沿用磁碟上的舊版，或新版啟動、`/config` 驗證失敗後回退舊版時，將失敗原因隨 `LoadedConfig` 傳回 VOD 設定，顯示「已使用舊版」提示。已被拒絕的新版在隔離期內略過時也顯示提示。一般成功載入仍使用設定本身的 `notice`。

`BaseConfig` 在背景載入完成時擷取提示內容，主執行緒顯示前再核對 task id，避免後來的設定載入覆蓋或延遲顯示上一筆提示。本次沒有改變 Node bundle 的下載、接受、拒絕及回退條件；NodeJS 設定依目前契約不回傳 `urls` 來源清單。

## 已知邊界

若新版 Node 已通過 `NodeClient` 驗證，但 App 的 `VodConfig.checkJson()`／`parseConfig()` 才拋錯，現有流程會拒絕待接受 bundle 並回報錯誤，卻不會在同一次載入重啟舊版 runtime。這是獨立問題，不能把本次提示改動宣稱為涵蓋所有 App 層解析失敗；詳見 `commit-convergence-20260924.md`。

## 驗證範圍

本次以 staged diff 單獨核對功能邊界與 `git diff --cached --check`；NodeJS lint、手機與 Leanback Java 編譯通過。未做實體裝置回退情境測試，不提交測試檔案。
