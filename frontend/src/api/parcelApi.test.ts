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

describe("parcelApi.createParcel", () => {
  const booking = {
    pickupAddress: "123 Main St, Colombo", pickupZone: "COLOMBO_SOUTH", dropoffAddress: "456 High St, Kandy",
    dropoffZone: "KANDY", recipientName: "Nimal Perera", recipientPhone: "0771234567", size: "MEDIUM" as const,
    weightKg: 2.5, speed: "STANDARD" as const,
  };

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("posts the booking as JSON with the login token and returns the created parcel", async () => {
    const created = { id: "p-1", trackingNo: "SD-7K3Q-9P2M", feeAmount: 650, feeCurrency: "LKR" };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(201, created));
    vi.stubGlobal("fetch", fetchMock);

    await expect(parcelApi.createParcel(booking, "token-1")).resolves.toEqual(created);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/parcels");
    expect(init.method).toBe("POST");
    expect(init.headers).toMatchObject({ Authorization: "Bearer token-1", "Content-Type": "application/json" });
    expect(JSON.parse(init.body as string)).toEqual(booking);
  });

  it("keeps the field errors of a 400 response so the form can show them", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(400, {
      status: 400,
      detail: "One or more fields are invalid.",
      errors: [{ field: "recipientPhone", message: "Recipient phone must be 9 to 15 digits" }, { bad: true }],
    })));

    const error = await parcelApi.createParcel(booking, "token-1").catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(ParcelApiError);
    expect(error).toMatchObject({
      status: 400,
      fieldErrors: [{ field: "recipientPhone", message: "Recipient phone must be 9 to 15 digits" }],
    });
  });
});

describe("parcelApi.getMyParcels", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("asks for one page of the caller's parcels, newest first, with the login token", async () => {
    const page = { content: [], page: 2, size: 10, totalElements: 21 };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, page));
    vi.stubGlobal("fetch", fetchMock);

    await expect(parcelApi.getMyParcels("token-1", { page: 2, size: 10, status: "DELIVERED" })).resolves.toEqual(page);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/parcels?page=2&size=10&sort=createdAt%2Cdesc&status=DELIVERED");
    expect(init.headers).toMatchObject({ Authorization: "Bearer token-1" });
  });

  it("leaves out the status filter when there is none", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, { content: [], page: 0, size: 10, totalElements: 0 }));
    vi.stubGlobal("fetch", fetchMock);

    await parcelApi.getMyParcels("token-1");

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/parcels?page=0&size=10&sort=createdAt%2Cdesc");
  });

  it("reports an expired login as a 401 ParcelApiError", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(401, { status: 401, detail: "Unauthorized" })));

    await expect(parcelApi.getMyParcels("expired")).rejects.toMatchObject({ status: 401 });
  });
});

describe("parcelApi.getParcel", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads one parcel with its history using the login token", async () => {
    const parcel = { id: "p-1", trackingNo: "SD-7K3Q-9P2M", history: [] };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, parcel));
    vi.stubGlobal("fetch", fetchMock);

    await expect(parcelApi.getParcel("p-1", "token-1")).resolves.toEqual(parcel);

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/parcels/p-1");
    expect(init.headers).toMatchObject({ Authorization: "Bearer token-1" });
  });

  it("reports someone else's or an unknown parcel as a 404", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(404, { status: 404, detail: "Parcel not found" })));

    await expect(parcelApi.getParcel("other", "token-1")).rejects.toMatchObject({ status: 404 });
  });
});

describe("parcelApi.getReceipt", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("downloads the receipt PDF with the login token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response("%PDF-1.4", { status: 200, headers: { "Content-Type": "application/pdf" } }));
    vi.stubGlobal("fetch", fetchMock);

    const pdf = await parcelApi.getReceipt("c41d0000-2222-4c2e-9a71-0d6f2b9c1e44", "token-1");

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/parcels/c41d0000-2222-4c2e-9a71-0d6f2b9c1e44/receipt");
    expect(init.headers).toMatchObject({ Authorization: "Bearer token-1" });
    expect(pdf.size).toBe("%PDF-1.4".length);
  });

  it("turns a 404 for someone else's parcel into a ParcelApiError", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(404, { status: 404, detail: "Parcel not found" })));

    await expect(parcelApi.getReceipt("other", "token-1")).rejects.toMatchObject({ status: 404 });
  });
});

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
