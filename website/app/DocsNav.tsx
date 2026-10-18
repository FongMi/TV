"use client";

import { useId, useState } from "react";

export function DocsNav({
  items,
}: {
  items: readonly (readonly [string, string])[];
}) {
  const id = useId();
  const [expanded, setExpanded] = useState(false);

  return (
    <aside className="docs-nav">
      <b>本頁導覽</b>
      <button
        className="docs-nav-toggle"
        type="button"
        aria-expanded={expanded}
        aria-controls={id}
        onClick={() => setExpanded(!expanded)}
      >
        本頁導覽
        <span aria-hidden="true">{expanded ? "收合 −" : "展開 +"}</span>
      </button>
      <nav
        id={id}
        className="docs-nav-links"
        aria-label="本頁導覽"
        data-expanded={expanded}
      >
        {items.map(([anchor, title]) => (
          <a href={`#${anchor}`} key={anchor}>
            {title}
          </a>
        ))}
      </nav>
    </aside>
  );
}
