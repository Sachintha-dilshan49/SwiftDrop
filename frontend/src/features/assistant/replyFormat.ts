// Pure helpers for showing assistant replies, kept apart from the component for fast refresh.
import type { SourceRef } from "../../api/assistantApi";

/** "prohibited-items" → "Prohibited items" */
export function articleTitle(slug: string): string {
  const words = slug.replace(/[-_]+/g, " ").trim();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** How many source articles are listed under a reply. */
const MAX_SOURCES = 3;

/** Unique article titles, in order: sections of the same article are listed once. */
export function sourceTitles(sources: SourceRef[]): string[] {
  return [...new Set(sources.map((source) => articleTitle(source.article)))].slice(0, MAX_SOURCES);
}

function tableCells(row: string): string[] {
  return row.trim().replace(/^\|/, "").replace(/\|$/, "").split("|").map((cell) => cell.trim());
}

const TABLE_SEPARATOR = /^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?$/;

export type Block =
  | { kind: "line"; text: string }
  | { kind: "bullet"; text: string; marker?: string }
  | { kind: "heading"; text: string }
  | { kind: "table"; header: string[]; rows: string[][] };

/** Splits a reply into lines, list items, headings and tables. */
export function replyBlocks(text: string): Block[] {
  const lines = text.split("\n");
  const blocks: Block[] = [];
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim().startsWith("|")) {
      const tableLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith("|")) {
        tableLines.push(lines[i]);
        i++;
      }
      i--;
      const rows = tableLines.filter((row) => !TABLE_SEPARATOR.test(row.trim())).map(tableCells);
      const hasHeader = tableLines.length > 1 && TABLE_SEPARATOR.test(tableLines[1].trim());
      blocks.push({ kind: "table", header: hasHeader ? rows[0] : [], rows: hasHeader ? rows.slice(1) : rows });
      continue;
    }
    if (/^\s*([-*_])\s*\1\s*\1[\s\-*_]*$/.test(line)) {
      continue; // horizontal rule
    }
    const heading = /^\s*#{1,6}\s+(.*)$/.exec(line);
    if (heading) {
      blocks.push({ kind: "heading", text: heading[1].replace(/\*\*/g, "") });
      continue;
    }
    const bullet = /^\s*[-*•]\s+(.*)$/.exec(line);
    if (bullet) {
      blocks.push({ kind: "bullet", text: bullet[1] });
      continue;
    }
    const numbered = /^\s*(\d+)[.)]\s+(.*)$/.exec(line);
    if (numbered) {
      blocks.push({ kind: "bullet", text: numbered[2], marker: `${numbered[1]}.` });
      continue;
    }
    blocks.push({ kind: "line", text: line });
  }
  return blocks;
}
