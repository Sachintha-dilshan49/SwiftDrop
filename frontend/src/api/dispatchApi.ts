// Client for dispatch-service through the gateway. VITE_API_BASE_URL is the gateway prefix ("/api" by
// default, like the other clients); every path below adds "/v1/...".
import type { ParcelSize } from "./parcelApi";

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
};
