export const playbackSections = [
  ["start", "新增指令按鈕"],
  ["examples", "常用範例"],
  ["properties", "支援的屬性"],
  ["seek", "跳轉位置"],
  ["cycle-values", "輪切數值"],
  ["limits", "使用限制"],
] as const;

export const playbackProperties = [
  {
    name: "pause",
    modes: ["set", "cycle-values", "cycle"],
    values: "yes 暫停；no 播放",
    description: "cycle pause 切換播放／暫停。",
  },
  {
    name: "speed",
    modes: ["set", "add", "cycle-values"],
    values: "0.1～5 倍；1 為原速",
    description: "add 超出範圍時取上下限。",
  },
  {
    name: "volume",
    modes: ["set", "add", "cycle-values"],
    values: "0～100；0 靜音",
    description: "播放器音量，系統音量不變；add 超出範圍時取上下限。",
  },
  {
    name: "sub-delay",
    modes: ["set", "add", "cycle-values"],
    values: "秒，可用小數；0 清除",
    description: "主字幕：正值延後、負值提前；副字幕不變。",
  },
  {
    name: "audio-delay",
    modes: ["set", "add", "cycle-values"],
    values: "秒，可用小數；0 清除",
    description: "聲音：正值延後、負值提前。",
  },
  {
    name: "loop-file",
    modes: ["set", "cycle-values"],
    values: "inf 或 yes 開啟；no 關閉",
    description: "單片無限循環，不支援指定次數或整份清單。",
  },
  {
    name: "chapter",
    modes: ["set", "add", "cycle-values"],
    values: "從 0 起算；add 僅接受 1、-1",
    description: "需有章節；set chapter 2 跳第三章。",
  },
  {
    name: "video-scale-x",
    modes: ["set", "cycle-values"],
    values: "0 < 倍率 ≤ 10000；1 取消縮放",
    description: "寬度倍率；1.15 加寬 15%。",
  },
  {
    name: "video-scale-y",
    modes: ["set", "cycle-values"],
    values: "0 < 倍率 ≤ 10000；1 取消縮放",
    description: "高度倍率；1.15 增高 15%。",
  },
] as const;

export const playbackExamples = [
  ["快轉 90 秒", "seek +90"],
  ["播放／暫停", "cycle pause"],
  ["切換倍速", "cycle-values speed 1 1.25 1.5 2"],
  ["切換音量", "cycle-values volume 30 60 100"],
  ["字幕提早 0.1 秒", "add sub-delay -0.1"],
  ["聲音延後 0.2 秒", "set audio-delay 0.2"],
  ["切換單片循環", "cycle-values loop-file no inf"],
  ["下一章", "add chapter 1"],
  ["切換影片高度", "cycle-values video-scale-y 1 1.15 1.3"],
] as const;

export const seekExamples = [
  ["seek 10", "快轉 10 秒（預設 relative）"],
  ["seek -10 relative", "倒退 10 秒"],
  ["seek 120 absolute", "跳到第 120 秒"],
  ["seek -10 absolute", "跳到結束前 10 秒"],
  ["seek 50 absolute-percent", "跳到 50% 位置"],
  ["seek 10 relative-percent", "快轉整部片長的 10%"],
] as const;
