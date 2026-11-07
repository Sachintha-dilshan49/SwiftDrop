// Client for parcel-service through the gateway. VITE_API_BASE_URL is the gateway prefix ("/api" by
// default, like the other clients); every path below adds "/v1/...".
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

export type ParcelStatus =
  | "CREATED"
  | "PICKED_UP"
  | "IN_TRANSIT"
  | "OUT_FOR_DELIVERY"
  | "DELIVERED"
  | "FAILED";

export type ParcelSize = "SMALL" | "MEDIUM" | "LARGE" | "EXTRA_LARGE";

/** One public history entry: status and time only. */
export interface PublicStatusHistoryItem {
  status: ParcelStatus;
  occurredAt: string;
}

/** GET /api/v1/track/{trackingNo}, and the SNAPSHOT event of the live stream. */
export interface PublicTrackingResponse {
  trackingNo: string;
  status: ParcelStatus;
  size: ParcelSize;
  weightKg: number;
  createdAt: string;
  history: PublicStatusHistoryItem[];
}

/** STATUS_CHANGE event of the live stream. */
export interface PublicTrackingEvent {
  trackingNo: string;
  status: ParcelStatus;
  previousStatus: ParcelStatus | null;
  occurredAt: string;
}

export const TERMINAL_STATUSES: readonly ParcelStatus[] = ["DELIVERED", "FAILED"];

/** An error response from parcel-service, with the ProblemDetail `detail` as message when there is one. */
export class ParcelApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ParcelApiError";
    this.status = status;
  }
}

async function toError(response: Response, fallback: string): Promise<ParcelApiError> {
  let detail: string | undefined;
  try {
    const body = (await response.json()) as { detail?: unknown };
    if (typeof body.detail === "string") {
      detail = body.detail;
    }
  } catch {
    // Not a ProblemDetail body; use the fallback message.
  }
  return new ParcelApiError(response.status, detail ?? fallback);
}

function trackPath(trackingNo: string): string {
  return `${API_BASE}/v1/track/${encodeURIComponent(trackingNo)}`;
}

export interface TrackingStreamHandlers {
  /** Sent first on every (re)connect; replace the page state with it, because missed events are not replayed. */
  onSnapshot: (snapshot: PublicTrackingResponse) => void;
  onStatusChange: (event: PublicTrackingEvent) => void;
  /** true while the stream is open, false while the browser reconnects or after it closed. */
  onLiveChange?: (live: boolean) => void;
}

function parseData<T>(event: Event): T | null {
  try {
    return JSON.parse((event as MessageEvent<string>).data) as T;
  } catch {
    return null;
  }
}

export const parcelApi = {
  /** Public tracking, no login and no Authorization header. */
  async getPublicTracking(trackingNo: string): Promise<PublicTrackingResponse> {
    const response = await fetch(trackPath(trackingNo), { headers: { Accept: "application/json" } });
    if (!response.ok) {
      throw await toError(response, "Could not load the tracking information. Please try again.");
    }
    return (await response.json()) as PublicTrackingResponse;
  },

  /**
   * Opens the live tracking stream (Server-Sent Events). The stream is closed here after DELIVERED or
   * FAILED, so the browser does not reconnect. Returns a function that closes the stream.
   */
  subscribeTrackingStream(trackingNo: string, handlers: TrackingStreamHandlers): () => void {
    const source = new EventSource(`${trackPath(trackingNo)}/stream`);
    const close = () => {
      source.close();
      handlers.onLiveChange?.(false);
    };

    source.onopen = () => handlers.onLiveChange?.(true);
    source.onerror = () => handlers.onLiveChange?.(false);

    source.addEventListener("SNAPSHOT", (event) => {
      const snapshot = parseData<PublicTrackingResponse>(event);
      if (snapshot) {
        handlers.onSnapshot(snapshot);
        if (TERMINAL_STATUSES.includes(snapshot.status)) {
          close();
        }
      }
    });

    source.addEventListener("STATUS_CHANGE", (event) => {
      const update = parseData<PublicTrackingEvent>(event);
      if (update) {
        handlers.onStatusChange(update);
        if (TERMINAL_STATUSES.includes(update.status)) {
          close();
        }
      }
    });

    return close;
  },
};
