// Client for dispatch-service through the gateway. VITE_API_BASE_URL is the gateway prefix ("/api" by
// default, like the other clients); every path below adds "/v1/...".
import type { ParcelSize, ParcelStatus } from "./parcelApi";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

export type CourierStatus = "AVAILABLE" | "BUSY" | "OFFLINE";
export type AssignmentStatus = "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "FAILED";

/** One row of GET /api/v1/dispatch/unassigned: a CREATED parcel without an active assignment. */
export interface UnassignedParcel {
  parcelId: string;
  trackingNo: string;
  pickupAddress: string;
  dropoffAddress: string;
  size: ParcelSize;
  weightKg: number;
  createdAt: string;
}

/** One row of GET /api/v1/dispatch/workload, least busy first. firstName is null if auth-service had none. */
export interface CourierWorkload {
  courierId: string;
  userId: string;
  firstName: string | null;
  status: CourierStatus;
  activeDeliveries: number;
}

/** Counts for one Sri Lanka day: assigned that day, delivered or failed that day. */
export interface ReportDay {
  date: string;
  assigned: number;
  delivered: number;
  failed: number;
}

/** One courier's counts over the report's range. firstName is null if auth-service had none. */
export interface ReportCourier {
  courierId: string;
  firstName: string | null;
  assigned: number;
  delivered: number;
  failed: number;
}

/** GET /api/v1/reports/daily: every day in the range (oldest first), totals and couriers (most deliveries first). */
export interface DailyReport {
  from: string;
  to: string;
  days: ReportDay[];
  totals: { assigned: number; delivered: number; failed: number };
  couriers: ReportCourier[];
}

/** Reply of POST /api/v1/dispatch/assignments. */
export interface Assignment {
  id: string;
  parcelId: string;
  trackingNo: string | null;
  courierId: string;
  assignedByUserId: string;
  status: AssignmentStatus;
  assignedAt: string;
}

/**
 * One of the signed-in courier's deliveries. parcelStatus is the parcel's status as last accepted by
 * parcel-service; it and the parcel fields are null for deliveries assigned before they were recorded.
 */
export interface CourierDelivery {
  assignmentId: string;
  parcelId: string;
  status: AssignmentStatus;
  parcelStatus: ParcelStatus | null;
  assignedAt: string;
  trackingNo: string | null;
  pickupAddress: string | null;
  dropoffAddress: string | null;
  recipientName: string | null;
  size: ParcelSize | null;
  weightKg: number | null;
}

/** The courier's status change; the photo is optional proof (JPEG, PNG or WebP, at most 5 MB). */
export interface DeliveryStatusUpdate {
  status: ParcelStatus;
  note?: string;
  recipientName?: string;
  failureReason?: string;
  photo?: File;
}

/** An error response from dispatch-service, with the ProblemDetail `detail` as message when there is one. */
export class DispatchApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "DispatchApiError";
    this.status = status;
  }
}

async function toError(response: Response, fallback: string): Promise<DispatchApiError> {
  let detail: string | undefined;
  try {
    const body = (await response.json()) as { detail?: unknown };
    if (typeof body.detail === "string") {
      detail = body.detail;
    }
  } catch {
    // Not a ProblemDetail body; use the fallback message.
  }
  return new DispatchApiError(response.status, detail ?? fallback);
}

async function getJson<T>(path: string, token: string, fallback: string): Promise<T> {
  const response = await fetch(`${API_BASE}/v1${path}`, {
    headers: { Accept: "application/json", Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw await toError(response, fallback);
  }
  return (await response.json()) as T;
}

export const dispatchApi = {
  /** Parcels waiting for a courier, oldest first (up to 100). */
  getUnassignedParcels(token: string): Promise<UnassignedParcel[]> {
    return getJson("/dispatch/unassigned", token, "Could not load the parcels waiting for a courier.");
  },

  /** Every courier with their active deliveries, least busy first. */
  getWorkload(token: string): Promise<CourierWorkload[]> {
    return getJson("/dispatch/workload", token, "Could not load the courier workload.");
  },

  /** Deliveries per day between two Sri Lanka dates (YYYY-MM-DD, both included); without dates, the last 7 days. */
  getDailyReport(token: string, range: { from?: string; to?: string } = {}): Promise<DailyReport> {
    const query = new URLSearchParams();
    if (range.from) query.set("from", range.from);
    if (range.to) query.set("to", range.to);
    const suffix = query.toString() ? `?${query}` : "";
    return getJson(`/reports/daily${suffix}`, token, "Could not load the delivery report.");
  },

  /** Gives the parcel to the courier; 409 if it is already assigned, the courier is offline or the parcel moved on. */
  async assign(parcelId: string, courierId: string, token: string): Promise<Assignment> {
    const response = await fetch(`${API_BASE}/v1/dispatch/assignments`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ parcelId, courierId }),
    });
    if (!response.ok) {
      throw await toError(response, "Could not assign the parcel. Please try again.");
    }
    return (await response.json()) as Assignment;
  },

  /** The courier's open deliveries plus any finished today, oldest assignment first. */
  getTodaysDeliveries(token: string): Promise<CourierDelivery[]> {
    return getJson("/courier/deliveries/today", token, "Could not load your deliveries.");
  },

  /** Moves the parcel one step through parcel-service, with optional proof photo (multipart). */
  async updateDeliveryStatus(
    assignmentId: string,
    update: DeliveryStatusUpdate,
    token: string,
  ): Promise<CourierDelivery> {
    const form = new FormData();
    form.append("status", update.status);
    for (const field of ["note", "recipientName", "failureReason"] as const) {
      const value = update[field]?.trim();
      if (value) {
        form.append(field, value);
      }
    }
    if (update.photo) {
      form.append("photo", update.photo);
    }
    // No Content-Type header: the browser sets multipart/form-data with its boundary
    const response = await fetch(`${API_BASE}/v1/courier/deliveries/${encodeURIComponent(assignmentId)}/status`, {
      method: "POST",
      headers: { Accept: "application/json", Authorization: `Bearer ${token}` },
      body: form,
    });
    if (!response.ok) {
      throw await toError(response, "Could not update the delivery. Please try again.");
    }
    return (await response.json()) as CourierDelivery;
  },
};
