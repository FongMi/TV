"use client";

import { useState } from "react";

export function CopyButton({ text, label }: { text: string; label: string }) {
  const [status, setStatus] = useState<"idle" | "copying" | "copied" | "error">(
    "idle",
  );

  async function copy() {
    setStatus("copying");
    try {
      await navigator.clipboard.writeText(text);
      setStatus("copied");
    } catch {
      setStatus("error");
    }
  }

  return (
    <span className="copy-control">
      <button
        type="button"
        className="copy-button"
        onClick={copy}
        aria-label={`複製 ${label}`}
        aria-busy={status === "copying"}
        disabled={status === "copying"}
      >
        {status === "copying"
          ? "稍候"
          : status === "copied"
            ? "已複製"
            : "複製"}
      </button>
      <span className="sr-only" role="status" aria-atomic="true">
        {status === "copying"
          ? `正在複製 ${label}`
          : status === "copied"
            ? `${label}已複製`
            : ""}
      </span>
      {status === "error" && (
        <span className="copy-error" role="alert">
          無法存取剪貼簿，請選取程式碼後複製。
        </span>
      )}
    </span>
  );
}
