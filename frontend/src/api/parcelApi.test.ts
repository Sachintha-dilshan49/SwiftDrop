import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ParcelApiError, PublicTrackingResponse, parcelApi } from "./parcelApi";

const tracking: PublicTrackingResponse = {
  trackingNo: "SD-7K3Q-9P2M",
  status: "IN_TRANSIT",
  size: "MEDIUM",
  weightKg: 2.5,
  createdAt: "2026-11-06T09:00:00Z",
  history: [{ status: "CREATED", occurredAt: "2026-11-06T09:00:00Z" }],
};

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** Stands in for the browser's EventSource so tests can push events. */
class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;
  private readonly listeners = new Map<string, Array<(event: MessageEvent<string>) => void>>();

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: (event: MessageEvent<string>) => void) {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener]);
  }

  emit(type: string, data: string) {
    this.listeners.get(type)?.forEach((listener) => listener(new MessageEvent(type, { data })));
  }

  close() {
    this.closed = true;
  }
}

describe("parcelApi.getPublicTracking", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("calls the public tracking route through the gateway without a login token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, tracking));
    vi.stubGlobal("fetch", fetchMock);

    await expect(parcelApi.getPublicTracking("SD-7K3Q-9P2M")).resolves.toEqual(tracking);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/track/SD-7K3Q-9P2M");
    expect(init.headers).not.toHaveProperty("Authorization");
  });

  it("turns a ProblemDetail error into a ParcelApiError with its status and detail", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      jsonResponse(404, { status: 404, detail: "Tracking number not found: SD-2222-2222" })));

    const error = await parcelApi.getPublicTracking("SD-2222-2222").catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(ParcelApiError);
    expect(error).toMatchObject({ status: 404, message: "Tracking number not found: SD-2222-2222" });
  });

  it("uses a general message when the error body is not a ProblemDetail", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("Bad gateway", { status: 502 })));

    await expect(parcelApi.getPublicTracking("SD-7K3Q-9P2M")).rejects.toMatchObject({
      status: 502,
      message: "Could not load the tracking information. Please try again.",
    });
  });
});

describe("parcelApi.subscribeTrackingStream", () => {
  beforeEach(() => {
    FakeEventSource.instances = [];
    vi.stubGlobal("EventSource", FakeEventSource);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function subscribe() {
    const handlers = { onSnapshot: vi.fn(), onStatusChange: vi.fn(), onLiveChange: vi.fn() };
    const close = parcelApi.subscribeTrackingStream("SD-7K3Q-9P2M", handlers);
    return { handlers, close, source: FakeEventSource.instances[0] };
  }

  it("opens the stream route and passes on the snapshot and status changes", () => {
    const { handlers, source } = subscribe();
    const change = { trackingNo: "SD-7K3Q-9P2M", status: "OUT_FOR_DELIVERY", previousStatus: "IN_TRANSIT",
      occurredAt: "2026-11-06T12:00:00Z" };

    source.onopen?.();
    source.emit("SNAPSHOT", JSON.stringify(tracking));
    source.emit("STATUS_CHANGE", JSON.stringify(change));

    expect(source.url).toBe("/api/v1/track/SD-7K3Q-9P2M/stream");
    expect(handlers.onLiveChange).toHaveBeenCalledWith(true);
    expect(handlers.onSnapshot).toHaveBeenCalledWith(tracking);
    expect(handlers.onStatusChange).toHaveBeenCalledWith(change);
    expect(source.closed).toBe(false);
  });

  it("closes the stream after DELIVERED so the browser does not reconnect", () => {
    const { handlers, source } = subscribe();

    source.emit("STATUS_CHANGE", JSON.stringify({ trackingNo: "SD-7K3Q-9P2M", status: "DELIVERED",
      previousStatus: "OUT_FOR_DELIVERY", occurredAt: "2026-11-06T15:00:00Z" }));

    expect(source.closed).toBe(true);
    expect(handlers.onLiveChange).toHaveBeenLastCalledWith(false);
  });

  it("closes the stream when the first snapshot is already FAILED", () => {
    const { source } = subscribe();

    source.emit("SNAPSHOT", JSON.stringify({ ...tracking, status: "FAILED" }));

    expect(source.closed).toBe(true);
  });

  it("ignores events that are not valid JSON", () => {
    const { handlers, source } = subscribe();

    source.emit("STATUS_CHANGE", "not json");

    expect(handlers.onStatusChange).not.toHaveBeenCalled();
  });

  it("reports a lost connection and closes when the caller unsubscribes", () => {
    const { handlers, close, source } = subscribe();

    source.onerror?.();
    expect(handlers.onLiveChange).toHaveBeenLastCalledWith(false);

    close();
    expect(source.closed).toBe(true);
  });
});
