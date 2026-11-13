import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AssistantWidget, articleTitle } from "./AssistantWidget";

const CONVERSATION = "c0ffee00-0000-4000-8000-000000000001";

function sse(...events: [string, unknown][]): Response {
  const text = events.map(([name, data]) => `event:${name}\ndata:${JSON.stringify(data)}\n\n`).join("");
  return new Response(text, { status: 200, headers: { "Content-Type": "text/event-stream" } });
}

function reply(text: string, sources: { article: string; section: string }[] = []): Response {
  return sse(["token", { text }], ["done", { conversationId: CONVERSATION, model: "groq", toolsUsed: [], sources }]);
}

function requestBody(fetchMock: ReturnType<typeof vi.fn>, call: number) {
  return JSON.parse((fetchMock.mock.calls[call][1] as RequestInit).body as string);
}

describe("AssistantWidget", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("opens from the launcher and shows suggestions", async () => {
    render(<AssistantWidget token="t" />);

    await userEvent.click(screen.getByRole("button", { name: "Open SwiftDrop assistant" }));

    expect(screen.getByRole("dialog", { name: "SwiftDrop assistant" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "What items can't I send?" })).toBeInTheDocument();
  });

  it("streams the reply with its sources and continues the same conversation", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(reply("Perfume is **not allowed**.", [{ article: "prohibited-items", section: "Liquids" }]))
      .mockResolvedValueOnce(reply("Yes."));
    vi.stubGlobal("fetch", fetchMock);
    render(<AssistantWidget token="token-1" initiallyOpen />);

    await userEvent.click(screen.getByRole("button", { name: "What items can't I send?" }));

    expect(await screen.findByText("not allowed")).toContainHTML("strong");
    expect(screen.getByText("Based on: Prohibited items · Liquids")).toBeInTheDocument();
    expect(requestBody(fetchMock, 0)).toEqual({ message: "What items can't I send?" });

    await userEvent.type(screen.getByRole("textbox", { name: "Message the assistant" }), "Even small bottles?{Enter}");

    expect(await screen.findByText("Yes.")).toBeInTheDocument();
    expect(requestBody(fetchMock, 1)).toEqual({ message: "Even small bottles?", conversationId: CONVERSATION });
  });

  it("does not send an empty message", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    render(<AssistantWidget token="t" initiallyOpen />);

    await userEvent.type(screen.getByRole("textbox", { name: "Message the assistant" }), "   {Enter}");

    expect(screen.getByRole("button", { name: "Send" })).toBeDisabled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows why the assistant could not answer", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ detail: "The assistant is not configured." }), { status: 503 }),
      ),
    );
    render(<AssistantWidget token="t" initiallyOpen />);

    await userEvent.type(screen.getByRole("textbox", { name: "Message the assistant" }), "hello{Enter}");

    expect(await screen.findByRole("alert")).toHaveTextContent("The assistant is not configured.");
    expect(screen.getByRole("button", { name: "Send" })).toBeInTheDocument();
  });

  it("new chat clears the messages and forgets the conversation on the server", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(reply("Hello!"))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(reply("Hi again."));
    vi.stubGlobal("fetch", fetchMock);
    render(<AssistantWidget token="t" initiallyOpen />);
    const textbox = screen.getByRole("textbox", { name: "Message the assistant" });

    await userEvent.type(textbox, "hi{Enter}");
    await screen.findByText("Hello!");
    await userEvent.click(screen.getByRole("button", { name: "New chat" }));

    expect(screen.queryByText("Hello!")).not.toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(fetchMock.mock.calls[1][0]).toBe(`/api/v1/assistant/conversations/${CONVERSATION}`);

    await userEvent.type(textbox, "hi{Enter}");
    await screen.findByText("Hi again.");
    expect(requestBody(fetchMock, 2)).toEqual({ message: "hi" });
  });

  it("does not render HTML from the model", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(reply('<img src=x onerror="alert(1)">')));
    const { container } = render(<AssistantWidget token="t" initiallyOpen />);

    await userEvent.type(screen.getByRole("textbox", { name: "Message the assistant" }), "hi{Enter}");

    expect(await screen.findByText('<img src=x onerror="alert(1)">')).toBeInTheDocument();
    expect(container.querySelector("img")).toBeNull();
  });
});

describe("articleTitle", () => {
  it("turns an article slug into a title", () => {
    expect(articleTitle("prohibited-items")).toBe("Prohibited items");
  });
});
