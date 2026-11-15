import type { CourierDelivery } from "../../api/dispatchApi";
import type { ParcelStatus } from "../../api/parcelApi";

/** Words a courier sees for each parcel status. */
export const COURIER_STATUS_LABELS: Record<ParcelStatus, string> = {
  CREATED: "To pick up",
  PICKED_UP: "Picked up",
  IN_TRANSIT: "In transit",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Failed",
};

/** Button text for moving a parcel to each status. */
export const ACTION_LABELS: Record<ParcelStatus, string> = {
  CREATED: "Booked",
  PICKED_UP: "Picked up",
  IN_TRANSIT: "On the way",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Delivery failed",
};

/** The delivery route a parcel goes through, in order (FAILED replaces the last step). */
export const ROUTE: ParcelStatus[] = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"];

const NEXT: Record<ParcelStatus, ParcelStatus[]> = {
  CREATED: ["PICKED_UP"],
  PICKED_UP: ["IN_TRANSIT"],
  IN_TRANSIT: ["OUT_FOR_DELIVERY"],
  OUT_FOR_DELIVERY: ["DELIVERED", "FAILED"],
  DELIVERED: [],
  FAILED: [],
};

/**
 * The parcel's current status as far as this screen knows. Deliveries assigned before dispatch recorded it have
 * none; a not-yet-started one is then still CREATED, a started one is unknown (null).
 */
export function currentStatus(delivery: CourierDelivery): ParcelStatus | null {
  if (delivery.parcelStatus) {
    return delivery.parcelStatus;
  }
  if (delivery.status === "ASSIGNED") {
    return "CREATED";
  }
  if (delivery.status === "COMPLETED") {
    return "DELIVERED";
  }
  return delivery.status === "FAILED" ? "FAILED" : null;
}

/**
 * Which buttons to offer. A hint only: parcel-service owns the rules and answers 409 if this screen is out of date.
 * Unknown status: every forward step, and parcel-service picks out the wrong ones.
 */
export function nextSteps(delivery: CourierDelivery): ParcelStatus[] {
  if (!isOpen(delivery)) {
    return [];
  }
  const status = currentStatus(delivery);
  return status ? NEXT[status] : ["PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED"];
}

export function isOpen(delivery: CourierDelivery): boolean {
  return delivery.status === "ASSIGNED" || delivery.status === "IN_PROGRESS";
}

export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;
const PHOTO_TYPES = ["image/jpeg", "image/png", "image/webp"];

/** Why a chosen photo cannot be sent, or null if it is fine. dispatch-service checks the content again. */
export function photoProblem(photo: File): string | null {
  if (!PHOTO_TYPES.includes(photo.type)) {
    return "The photo must be a JPEG, PNG or WebP image.";
  }
  return photo.size > MAX_PHOTO_BYTES ? "The photo must be 5 MB or smaller." : null;
}
