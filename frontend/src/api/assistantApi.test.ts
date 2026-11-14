import { afterEach, describe, expect, it, vi } from "vitest";
import { AssistantApiError, ChatDone, assistantApi, parseServerSentEvents } from "./assistantApi";

const DONE: ChatDone = {
  conversationId: "c0ffee00-0000-4000-8000-000000000001",
  model: "groq",
  toolsUsed: ["track_parcel"],
  sources: [{ article: "prohibited-items", section: "Liquids" }],
};

/** A streamed response whose body arrives in the given chunks. */
function streamResponse(chunks: string[], status = 200): Response {
  const encoder = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
      controller.close();
    },
  });
  return new Response(body, { status, headers: { "Content-Type": "text/event-stream" } });
}

function problem(status: number, detail: string): Response {
  return new Response(JSON.stringify({ status, detail }), {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

function collect() {
  const tokens: string[] = [];
  const done: ChatDone[] = [];
  return { tokens, done, handlers: { onToken: (t: string) => tokens.push(t), onDone: (d: ChatDone) => done.push(d) } };
}

describe("parseServerSentEvents", () => {
  it("reads event names and data and keeps an unfinished event for later", () => {
    const { events, rest } = parseServerSentEvents('event:token\ndata:{"text":"Hi"}\n\nevent: done\ndata: {}\n\nevent:tok');

    expect(events).toEqual([
      { event: "token", data: '{"text":"Hi"}' },
      { event: "done", data: "{}" },
    ]);
    expect(rest).toBe("event:tok");
  });

  it("handles CRLF line endings, comments and multi-line data", () => {
    const { events } = parseServerSentEvents(":keep-alive\r\n\r\ndata:a\r\ndata:b\r\n\r\n");

    expect(events).toEqual([{ event: "message", data: "a\nb" }]);
  });
});

describe("assistantApi.streamChat", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("posts the question with the login token and streams tokens until done", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      streamResponse([
        'event:token\ndata:{"text":"Your parcel"}\n\nevent:tok',
        'en\ndata:{"text":" is in transit."}\n\n',
        `event:done\ndata:${JSON.stringify(DONE)}\n\n`,
      ]),
    );
    vi.stubGlobal("fetch", fetchMock);
    const { tokens, done, handlers } = collect();

    await assistantApi.streamChat({ message: "Where is SD-7K3Q-9P2M?", conversationId: "c-1" }, "token-1", handlers);

    expect(tokens.join("")).toBe("Your parcel is in transit.");
    expect(done).toEqual([DONE]);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/assistant/chat/stream");
    expect(init.method).toBe("POST");
    expect(init.headers).toMatchObject({ Authorization: "Bearer token-1", "Content-Type": "application/json" });
    expect(JSON.parse(init.body as string)).toEqual({ message: "Where is SD-7K3Q-9P2M?", conversationId: "c-1" });
  });

  it("rejects with the detail of an error event sent mid-stream", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        streamResponse(['event:token\ndata:{"text":"Hm"}\n\n', 'event:error\ndata:{"detail":"The model timed out."}\n\n']),
      ),
    );
    const { tokens, handlers } = collect();

    await expect(assistantApi.streamChat({ message: "hi" }, "t", handlers)).rejects.toEqual(
      new AssistantApiError(200, "The model timed out."),
    );
    expect(tokens).toEqual(["Hm"]);
  });

  it("rejects when the stream ends without a done event", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(streamResponse(['event:token\ndata:{"text":"Hal"}\n\n'])));

    await expect(assistantApi.streamChat({ message: "hi" }, "t", collect().handlers)).rejects.toBeInstanceOf(
      AssistantApiError,
    );
  });

  it("uses the ProblemDetail of a failed request as the message", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(problem(503, "The assistant model is not configured.")));

    await expect(assistantApi.streamChat({ message: "hi" }, "t", collect().handlers)).rejects.toEqual(
      new AssistantApiError(503, "The assistant model is not configured."),
    );
  });

  it("asks the user to sign in again on 401 without showing the token error", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(problem(401, "Jwt expired at 2026-11-13T10:00:00Z")));

    await expect(assistantApi.streamChat({ message: "hi" }, "t", collect().handlers)).rejects.toEqual(
      new AssistantApiError(401, "Your session has expired. Please sign in again."),
    );
  });

  it("explains a rate limit even without a body", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 429 })));

    await expect(assistantApi.streamChat({ message: "hi" }, "t", collect().handlers)).rejects.toThrow(/too quickly/);
  });
});

describe("assistantApi.deleteConversation", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("deletes the conversation with the login token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await assistantApi.deleteConversation("c-1", "token-1");

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/assistant/conversations/c-1");
    expect(init).toMatchObject({ method: "DELETE", headers: { Authorization: "Bearer token-1" } });
  });

  it("treats a conversation that is already gone as deleted", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 404 })));

    await expect(assistantApi.deleteConversation("c-1", "t")).resolves.toBeUndefined();
  });
});
