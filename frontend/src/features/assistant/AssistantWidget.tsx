import { FormEvent, KeyboardEvent, ReactNode, useEffect, useRef, useState } from "react";
import { AssistantApiError, MAX_MESSAGE_LENGTH, SourceRef, assistantApi } from "../../api/assistantApi";
import "./AssistantWidget.css";

type ChatMessage =
  | { id: number; role: "user"; text: string }
  | { id: number; role: "assistant"; text: string; sources: SourceRef[]; error?: string; pending: boolean };

const SUGGESTIONS = [
  "Where is my latest parcel?",
  "How much to send 2 kg from Colombo 3 to Nugegoda?",
  "What items can't I send?",
];

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

/** Renders **bold** inside one line of plain text. */
function renderInline(line: string): ReactNode[] {
  return line.split(/(\*\*[^*]+\*\*)/g).map((part, index) =>
    part.startsWith("**") && part.endsWith("**") && part.length > 4 ? <strong key={index}>{part.slice(2, -2)}</strong> : part,
  );
}

function tableCells(row: string): string[] {
  return row.trim().replace(/^\|/, "").replace(/\|$/, "").split("|").map((cell) => cell.trim());
}

const TABLE_SEPARATOR = /^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?$/;

type Block =
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

/** Plain text with lists, headings, tables and **bold**; no HTML from the model is rendered. */
function ReplyText({ text }: { text: string }) {
  return (
    <>
      {replyBlocks(text).map((block, index) => {
        switch (block.kind) {
          case "table":
            return (
              <div className="assistant-table-wrap" key={index}>
                <table className="assistant-table">
                  {block.header.length > 0 && (
                    <thead>
                      <tr>{block.header.map((cell, c) => <th key={c}>{renderInline(cell)}</th>)}</tr>
                    </thead>
                  )}
                  <tbody>
                    {block.rows.map((row, r) => (
                      <tr key={r}>{row.map((cell, c) => <td key={c}>{renderInline(cell)}</td>)}</tr>
                    ))}
                  </tbody>
                </table>
              </div>
            );
          case "heading":
            return <strong className="assistant-line assistant-heading" key={index}>{block.text}</strong>;
          case "bullet":
            return (
              <span className="assistant-line assistant-bullet" data-marker={block.marker ?? "•"} key={index}>
                {renderInline(block.text)}
              </span>
            );
          default:
            return <span className="assistant-line" key={index}>{renderInline(block.text)}</span>;
        }
      })}
    </>
  );
}

type AssistantWidgetProps = {
  token: string;
  /** Opens the panel on first render (tests and demos). */
  initiallyOpen?: boolean;
};

/** Floating chat with the SwiftDrop assistant, for every signed-in role. */
export function AssistantWidget({ token, initiallyOpen = false }: AssistantWidgetProps) {
  const [open, setOpen] = useState(initiallyOpen);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const conversationId = useRef<string | undefined>(undefined);
  const abort = useRef<AbortController | null>(null);
  const nextId = useRef(1);
  const listEnd = useRef<HTMLDivElement>(null);
  const input = useRef<HTMLTextAreaElement>(null);

  useEffect(() => () => abort.current?.abort(), []);

  useEffect(() => {
    listEnd.current?.scrollIntoView?.({ block: "end" });
  }, [messages]);

  useEffect(() => {
    if (open) {
      input.current?.focus();
    }
  }, [open]);

  function updateAssistant(id: number, change: (message: Extract<ChatMessage, { role: "assistant" }>) => ChatMessage) {
    setMessages((current) =>
      current.map((message) => (message.id === id && message.role === "assistant" ? change(message) : message)),
    );
  }

  async function send(text: string) {
    const question = text.trim();
    if (question === "" || busy) {
      return;
    }
    const userId = nextId.current++;
    const replyId = nextId.current++;
    setMessages((current) => [
      ...current,
      { id: userId, role: "user", text: question },
      { id: replyId, role: "assistant", text: "", sources: [], pending: true },
    ]);
    setDraft("");
    setBusy(true);
    const controller = new AbortController();
    abort.current = controller;

    try {
      await assistantApi.streamChat(
        { message: question, conversationId: conversationId.current },
        token,
        {
          onToken: (piece) => updateAssistant(replyId, (message) => ({ ...message, text: message.text + piece })),
          onDone: (done) => {
            conversationId.current = done.conversationId;
            // Help articles are looked up for every message; when a tool supplied the answer they are
            // only loosely related, so they are not shown as its source
            const sources = done.toolsUsed.length > 0 ? [] : done.sources;
            updateAssistant(replyId, (message) => ({ ...message, sources, pending: false }));
          },
        },
        controller.signal,
      );
    } catch (error) {
      if (controller.signal.aborted) {
        updateAssistant(replyId, (message) => ({ ...message, pending: false, error: message.text ? undefined : "Stopped." }));
      } else {
        const detail = error instanceof AssistantApiError ? error.message : "Could not reach the assistant. Please check your connection.";
        updateAssistant(replyId, (message) => ({ ...message, pending: false, error: detail }));
      }
    } finally {
      if (abort.current === controller) {
        abort.current = null;
      }
      setBusy(false);
      input.current?.focus();
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    void send(draft);
  }

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void send(draft);
    }
  }

  function newChat() {
    abort.current?.abort();
    const previous = conversationId.current;
    conversationId.current = undefined;
    setMessages([]);
    setDraft("");
    if (previous) {
      // Best effort: the server forgets idle conversations anyway
      assistantApi.deleteConversation(previous, token).catch(() => undefined);
    }
  }

  if (!open) {
    return (
      <button className="assistant-launcher" type="button" onClick={() => setOpen(true)} aria-label="Open SwiftDrop assistant">
        <span aria-hidden="true">✦</span> Ask SwiftDrop
      </button>
    );
  }

  return (
    <section className="assistant-panel" role="dialog" aria-label="SwiftDrop assistant">
      <header className="assistant-header">
        <div>
          <strong>SwiftDrop assistant</strong>
          <small>Answers can be wrong. Check important details.</small>
        </div>
        <div className="assistant-header-actions">
          <button type="button" onClick={newChat} disabled={messages.length === 0}>New chat</button>
          <button type="button" onClick={() => setOpen(false)} aria-label="Close assistant">✕</button>
        </div>
      </header>

      <div className="assistant-messages" aria-live="polite">
        {messages.length === 0 && (
          <div className="assistant-welcome">
            <p>Hi! I can track your parcels, estimate delivery fees and answer questions about SwiftDrop.</p>
            <div className="assistant-suggestions">
              {SUGGESTIONS.map((suggestion) => (
                <button type="button" key={suggestion} onClick={() => void send(suggestion)}>{suggestion}</button>
              ))}
            </div>
          </div>
        )}
        {messages.map((message) =>
          message.role === "user" ? (
            <div className="assistant-message assistant-message-user" key={message.id}>{message.text}</div>
          ) : (
            <div className="assistant-message assistant-message-bot" key={message.id}>
              {message.text && <ReplyText text={message.text} />}
              {message.pending && !message.text && <span className="assistant-typing" aria-label="Assistant is typing">•••</span>}
              {message.error && <p className="assistant-error" role="alert">{message.error}</p>}
              {message.sources.length > 0 && (
                <p className="assistant-sources">Based on: {sourceTitles(message.sources).join(", ")}</p>
              )}
            </div>
          ),
        )}
        <div ref={listEnd} />
      </div>

      <form className="assistant-form" onSubmit={onSubmit}>
        <textarea
          ref={input}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={onKeyDown}
          maxLength={MAX_MESSAGE_LENGTH}
          rows={2}
          placeholder="Ask about a parcel, a fee or a policy"
          aria-label="Message the assistant"
        />
        {busy ? (
          <button type="button" onClick={() => abort.current?.abort()}>Stop</button>
        ) : (
          <button type="submit" disabled={draft.trim() === ""}>Send</button>
        )}
      </form>
    </section>
  );
}
