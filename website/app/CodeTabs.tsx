"use client";

import { useId, useRef, useState } from "react";
import { CopyButton } from "./CopyButton";

export type CodeSample = {
  id: string;
  label: string;
  file: string;
  code: string;
};

export function CodeTabs({ samples }: { samples: CodeSample[] }) {
  const [active, setActive] = useState(samples[0].id);
  const id = useId();
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  const sample = samples.find((item) => item.id === active) ?? samples[0];
  return (
    <div className="code-tabs">
      <div className="code-tabs-head">
        <div role="tablist" aria-label="Demo 語言">
          {samples.map((item, index) => (
            <button
              type="button"
              ref={(element) => {
                tabs.current[index] = element;
              }}
              id={`${id}-tab-${item.id}`}
              role="tab"
              aria-controls={`${id}-panel-${item.id}`}
              aria-selected={active === item.id}
              tabIndex={active === item.id ? 0 : -1}
              key={item.id}
              onClick={() => setActive(item.id)}
              onKeyDown={(event) => {
                const next =
                  event.key === "ArrowRight"
                    ? (index + 1) % samples.length
                    : event.key === "ArrowLeft"
                      ? (index - 1 + samples.length) % samples.length
                      : event.key === "Home"
                        ? 0
                        : event.key === "End"
                          ? samples.length - 1
                          : -1;
                if (next < 0) return;
                event.preventDefault();
                setActive(samples[next].id);
                tabs.current[next]?.focus();
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
        <CopyButton
          key={sample.id}
          text={sample.code}
          label={`${sample.label} Demo`}
        />
      </div>
      <div className="code-file">{sample.file}</div>
      {samples.map((item) => (
        <pre
          hidden={active !== item.id}
          tabIndex={0}
          key={item.id}
          id={`${id}-panel-${item.id}`}
          role="tabpanel"
          aria-labelledby={`${id}-tab-${item.id}`}
        >
          <code>{item.code}</code>
        </pre>
      ))}
    </div>
  );
}
