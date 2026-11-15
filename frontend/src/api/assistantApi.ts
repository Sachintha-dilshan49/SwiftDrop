// Client for ai-assistant through the gateway. VITE_API_BASE_URL is the gateway prefix ("/api" by
// default, like the other clients); every path below adds "/v1/assistant/...".
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

/** The longest question the service accepts (ChatRequest @Size(max = 2000)). */
export const MAX_MESSAGE_LENGTH = 2000;

/** A help article passage an answer was based on, e.g. prohibited-items / Liquids. */
export interface SourceRef {
  article: string;
  section: string;
}

/** POST /api/v1/assistant/chat/stream body. Leave conversationId out to start a new conversation. */
export interface ChatRequest {
  message: string;
  conversationId?: string;
  model?: string;
}

/** The "done" event, sent once after the last token. */
export interface ChatDone {
  conversationId: string;
  model: string;
  toolsUsed: string[];
  sources: SourceRef[];
}

export interface ChatStreamHandlers {
  /** A piece of the reply, in order. */
  onToken: (text: string) => void;
  onDone: (done: ChatDone) => void;
}

/** An error from ai-assistant (or the gateway in front of it), with a message that is safe to show. */
export class AssistantApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "AssistantApiError";
    this.status = status;
  }
}

function fallbackMessage(status: number): string {
  switch (status) {
    case 401:
      return "Your session has expired. Please sign in again.";
    case 429:
      return "You are sending messages too quickly. Please wait a moment and try again.";
    case 503:
      return "The assistant is not available right now. Please try again later.";
    default:
      return "The assistant could not answer. Please try again.";
  }
}

async function toError(response: Response): Promise<AssistantApiError> {
  let detail: string | undefined;
  // A 401 detail describes the token, which is not useful to the user
  if (response.status !== 401) {
    try {
      const body = (await response.json()) as { detail?: unknown };
      if (typeof body.detail === "string" && body.detail.trim() !== "") {
        detail = body.detail;
      }
    } catch {
      // Not a ProblemDetail body; use the fallback message.
    }
  }
  return new AssistantApiError(response.status, detail ?? fallbackMessage(response.status));
}

export interface ServerSentEvent {
  event: string;
  data: string;
}

/**
 * Splits Server-Sent Events text into complete events. Returns the events and the unfinished text
 * left over, which must be passed back in with the next chunk.
 */
export function parseServerSentEvents(buffer: string): { events: ServerSentEvent[]; rest: string } {
  const normalized = buffer.replace(/\r\n?/g, "\n");
  const blocks = normalized.split("\n\n");
  const rest = blocks.pop() ?? "";
  const events: ServerSentEvent[] = [];
  for (const block of blocks) {
    let event = "message";
    const data: string[] = [];
    for (const line of block.split("\n")) {
      if (line.startsWith(":")) {
        continue; // comment / keep-alive
      }
      const colon = line.indexOf(":");
      const field = colon === -1 ? line : line.slice(0, colon);
      let value = colon === -1 ? "" : line.slice(colon + 1);
      if (value.startsWith(" ")) {
        value = value.slice(1);
      }
      if (field === "event") {
        event = value;
      } else if (field === "data") {
        data.push(value);
      }
    }
    if (data.length > 0) {
      events.push({ event, data: data.join("\n") });
    }
  }
  return { events, rest };
}

function parseJson<T>(data: string): T | null {
  try {
    return JSON.parse(data) as T;
  } catch {
    return null;
  }
}

/** Handles one event; returns true once the reply is finished. */
function handleEvent({ event, data }: ServerSentEvent, handlers: ChatStreamHandlers): boolean {
  switch (event) {
    case "token": {
      const token = parseJson<{ text?: unknown }>(data);
      if (typeof token?.text === "string") {
        handlers.onToken(token.text);
      }
      return false;
    }
    case "done": {
      const done = parseJson<ChatDone>(data);
      if (!done) {
        throw new AssistantApiError(200, fallbackMessage(200));
      }
      handlers.onDone({ ...done, toolsUsed: done.toolsUsed ?? [], sources: done.sources ?? [] });
      return true;
    }
    case "error": {
      const error = parseJson<{ detail?: unknown }>(data);
      throw new AssistantApiError(
        200,
        typeof error?.detail === "string" && error.detail !== "" ? error.detail : fallbackMessage(200),
      );
    }
    default:
      return false;
  }
}

export const assistantApi = {
  /**
   * Asks the assistant and streams the reply. Resolves after the "done" event; rejects with an
   * AssistantApiError if the request fails or the reply breaks off. Pass a signal to cancel.
   */
  async streamChat(
    request: ChatRequest,
    token: string,
    handlers: ChatStreamHandlers,
    signal?: AbortSignal,
  ): Promise<void> {
    const response = await fetch(`${API_BASE}/v1/assistant/chat/stream`, {
      method: "POST",
      headers: {
        Accept: "text/event-stream, application/problem+json",
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(request),
      signal,
    });
    if (!response.ok) {
      throw await toError(response);
    }
    if (!response.body) {
      throw new AssistantApiError(response.status, fallbackMessage(response.status));
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    try {
      for (;;) {
        const { value, done } = await reader.read();
        buffer += done ? decoder.decode() + "\n\n" : decoder.decode(value, { stream: true });
        const parsed = parseServerSentEvents(buffer);
        buffer = parsed.rest;
        for (const event of parsed.events) {
          if (handleEvent(event, handlers)) {
            return;
          }
        }
        if (done) {
          // The stream ended without a "done" event, so the reply is incomplete
          throw new AssistantApiError(response.status, fallbackMessage(response.status));
        }
      }
    } finally {
      reader.cancel().catch(() => undefined);
    }
  },

  /** Forgets a conversation on the server. A conversation that is already gone is not an error. */
  async deleteConversation(conversationId: string, token: string): Promise<void> {
    const response = await fetch(`${API_BASE}/v1/assistant/conversations/${encodeURIComponent(conversationId)}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok && response.status !== 404) {
      throw await toError(response);
    }
  },
};
