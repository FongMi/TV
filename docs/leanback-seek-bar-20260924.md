# Leanback 長按時間軸 — 2026-09-24

Leanback 專用 `exo_player_seek_view.xml` 保留本機 Media3 `PlayerSeekView` 預設版面的時間與進度 ID，只把 `DefaultTimeBar` 換成 `LeanbackTimeBar`。按住遙控器左右鍵時，進度條依按下時間每滿一秒增加一個 10 秒的跳轉單位；放鍵、失焦或離開視窗後恢復 10 秒。手機版仍使用 Media3 預設時間軸。

這會改變 Leanback 長按跳轉速度及焦點處理，並非等價重構。已對照本機 Media3 原版 layout 與 `PlayerSeekView` 載入流程；編譯能檢查資源覆寫與型別，仍需電視或空閒的 Leanback 模擬器驗證實際按鍵、seek-to-end 和焦點。
