"use client";

import { CopyButton } from "./CopyButton";

function JsonCode({ text }: { text: string }) {
  const tokens = text.split(
    /("(?:\\.|[^"\\])*"\s*:|"(?:\\.|[^"\\])*"|\b(?:true|false|null|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\b)/g,
  );
  return (
    <>
      {tokens.map((token, index) => {
        const kind = token.startsWith('"')
          ? token.trimEnd().endsWith(":")
            ? "key"
            : "string"
          : /^(true|false|null|-?\d)/.test(token)
            ? "value"
            : "";
        return kind ? (
          <span key={index} className={`code-${kind}`}>
            {token}
          </span>
        ) : (
          token
        );
      })}
    </>
  );
}

export function CodeBlock({
  label = "JSON",
  path,
  children,
}: {
  label?: string;
  path?: string;
  children: string;
}) {
  const name = `${label}${path ? ` · ${path}` : ""} 範例`;
  return (
    <div className="code-card" role="region" aria-label={name}>
      <div className="code-card-head">
        <span>{label}</span>
        {path && <code>{path}</code>}
        <CopyButton text={children} label={name} />
      </div>
      <pre tabIndex={0} aria-label={name}>
        <code>
          {label.startsWith("JSON") ? <JsonCode text={children} /> : children}
        </code>
      </pre>
    </div>
  );
}
