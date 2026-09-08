"use client";

import { useState } from "react";
import { SearchIcon } from "./icons";
import { filterFields, type Field } from "./config-fields";

export function FieldTable({
  fields,
  label,
}: {
  fields: Field[];
  label: string;
}) {
  const [query, setQuery] = useState("");
  const visible = filterFields(fields, query);
  return (
    <div className="field-box">
      <label className="field-search">
        <SearchIcon />
        <input
          aria-label={`搜尋 ${label} 欄位`}
          placeholder={`搜尋 ${label} 欄位…`}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <b aria-live="polite">{visible.length} 欄位</b>
      </label>
      <div className="table-scroll">
        <table className="field-table" aria-label={`${label} 欄位說明`}>
          <thead>
            <tr>
              <th scope="col">欄位</th>
              <th scope="col">類型</th>
              <th scope="col">用途</th>
              <th scope="col">預設</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((field) => (
              <tr key={field.name}>
                <td>
                  <code>{field.name}</code>
                  {field.required && <small>必要</small>}
                </td>
                <td>{field.type}</td>
                <td>{field.description}</td>
                <td>{field.default ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {visible.length === 0 && (
        <p className="field-empty" role="status">
          找不到符合的欄位，請換個關鍵字。
        </p>
      )}
    </div>
  );
}
