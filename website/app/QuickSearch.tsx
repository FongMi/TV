"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { SearchIcon } from "./icons";
import { searchDocumentation } from "./doc-search";

export function QuickSearch() {
  const [query, setQuery] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const focusSearch = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement;
      if (
        event.key !== "/" ||
        event.ctrlKey ||
        event.metaKey ||
        event.altKey ||
        target.isContentEditable ||
        target.matches("input, textarea, select")
      )
        return;
      event.preventDefault();
      inputRef.current?.focus();
    };
    window.addEventListener("keydown", focusSearch);
    return () => window.removeEventListener("keydown", focusSearch);
  }, []);
  const results = searchDocumentation(query);

  return (
    <div className="quick-search">
      <label htmlFor="doc-search">
        <SearchIcon />
        <input
          ref={inputRef}
          id="doc-search"
          type="search"
          aria-label="搜尋文件"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="搜尋 API、欄位或功能…"
        />
        <kbd aria-hidden="true">/</kbd>
      </label>
      <div className="search-results" aria-live="polite">
        {results.length ? (
          results.map(({ title, description, href }) => (
            <Link href={href} key={`${href}-${title}`}>
              <span>
                <b>{title}</b>
                <small>{description}</small>
              </span>
              <i aria-hidden="true">→</i>
            </Link>
          ))
        ) : (
          <p>找不到符合的條目，試試「Site」、「Live」或「Result」。</p>
        )}
      </div>
    </div>
  );
}
