import type { ParcelSize, ParcelStatus } from "../api/parcelApi";

/** Words shown to people for each parcel status. */
export const STATUS_LABELS: Record<ParcelStatus, string> = {
  CREATED: "Booked",
  PICKED_UP: "Picked up",
  IN_TRANSIT: "In transit",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  FAILED: "Delivery failed",
};

export const SIZE_LABELS: Record<ParcelSize, string> = {
  SMALL: "Small",
  MEDIUM: "Medium",
  LARGE: "Large",
  EXTRA_LARGE: "Extra large",
};

const dateTimeFormat = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" });
const amountFormat = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** A UTC timestamp from the API in the browser's local time; the raw text if it is not a date. */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : dateTimeFormat.format(date);
}

/** For example "LKR 1,250.00". */
export function formatMoney(amount: number, currency: string): string {
  return `${currency} ${amountFormat.format(amount)}`;
}
