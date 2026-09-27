# 對話框清單觸控邊界 — 2026-09-24

章節、裝置、版本、篩選、解析與還原對話框的清單停用巢狀捲動，讓短清單自行處理滾動事件，不再把 nested-scroll 手勢傳給外層對話框。手機裝置與篩選清單另改用專案既有 `CustomRecyclerView`，沿用它對單指觸控與橫向手勢的處理；Leanback 只有解析清單的 nested-scroll 設定變動。`fragment_episode` 不是對話框，暫不混入本提交。

這會改變觸控與外層 sheet 的手勢協調，不是純 XML 格式整理。手機／Leanback 資源與 Java 編譯能確認 binding 型別，實際清單捲動、拖曳 sheet、橫向手勢仍需在未被其他任務占用的裝置上驗證。

後續另將手機播放控制對話框的解析清單改用既有 `CustomRecyclerView` 並停用巢狀捲動；該清單沒有呼叫 `scrollToPosition()`。彈幕挑選對話框原本已使用 `CustomRecyclerView`，只停用巢狀捲動。兩者屬同一觸控歸屬調整，沒有納入新增內容按鈕或解碼 UI。

手機選集 `fragment_episode` 另列為獨立提交：它位於 `EpisodeGridDialog` 的 `ViewPager2` 內，改用 `CustomRecyclerView` 後，縱向滑動由清單優先處理，橫向超過觸控門檻則交回父層換頁；同時停用巢狀捲動。`EpisodeFragment` 初始 `scrollToPosition()` 因此會在 50 ms 後要求項目焦點，並非等價的 XML 換標籤。這組只改手機版面；實際縱向捲動、左右切頁、首次開啟焦點與點擊仍需裝置驗證。

手機軌道選擇對話框原本已使用 `CustomRecyclerView`；後續只為該清單加入 `nestedScrollingEnabled="false"`，與上述清單採相同的外層 sheet 捲動歸屬。這不涉及 Leanback 版面或 `TrackDialog` 的同步開啟／焦點時序改動；實際觸控仍需裝置驗證。
