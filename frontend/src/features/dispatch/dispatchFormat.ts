import type { CourierStatus, CourierWorkload } from "../../api/dispatchApi";

export const COURIER_STATUS_LABELS: Record<CourierStatus, string> = {
  AVAILABLE: "Available",
  BUSY: "Busy",
  OFFLINE: "Offline",
};

/** The courier's first name, or a short ID when auth-service had no name. */
export function courierName(courier: Pick<CourierWorkload, "courierId" | "firstName">): string {
  return courier.firstName ?? `Courier ${courier.courierId.slice(0, 8)}`;
}

/** How long a parcel has waited, for example "12 min", "3 h" or "2 days". */
export function waitingTime(createdAt: string, now: Date = new Date()): string {
  const minutes = Math.floor((now.getTime() - new Date(createdAt).getTime()) / 60_000);
  if (Number.isNaN(minutes) || minutes < 1) {
    return "just now";
  }
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours} h`;
  }
  const days = Math.floor(hours / 24);
  return days === 1 ? "1 day" : `${days} days`;
}
