import type { DeliverySpeed, ParcelSize } from "../../api/parcelApi";

/**
 * Delivery zones and their codes, agreed with Role 1 (docs/architecture.md §8). This is the only place
 * they are listed: replace it with GET /api/v1/pricing/zones once pricing-service serves it.
 */
export const ZONES: ReadonlyArray<{ code: string; name: string }> = [
  { code: "COLOMBO_CENTRAL", name: "Colombo Central (Colombo 01–03)" },
  { code: "COLOMBO_SOUTH", name: "Colombo South (Colombo 04–06)" },
  { code: "COLOMBO_NORTH", name: "Colombo North (Colombo 07–15)" },
  { code: "KOTTE_RAJAGIRIYA", name: "Kotte & Rajagiriya" },
  { code: "NUGEGODA_MAHARAGAMA", name: "Nugegoda & Maharagama" },
  { code: "DEHIWALA_MT_LAVINIA", name: "Dehiwala & Mt Lavinia" },
  { code: "KANDY", name: "Kandy" },
  { code: "GALLE", name: "Galle" },
  { code: "NEGOMBO", name: "Negombo" },
];

/** Parcel sizes and the heaviest parcel each one takes (docs/architecture.md §8). */
export const SIZE_OPTIONS: ReadonlyArray<{ size: ParcelSize; name: string; maxWeightKg: number }> = [
  { size: "SMALL", name: "Small", maxWeightKg: 5 },
  { size: "MEDIUM", name: "Medium", maxWeightKg: 10 },
  { size: "LARGE", name: "Large", maxWeightKg: 20 },
  { size: "EXTRA_LARGE", name: "Extra large", maxWeightKg: 40 },
];

/** Delivery speeds and their promise; the fee itself always comes from pricing-service. */
export const SPEED_OPTIONS: ReadonlyArray<{ speed: DeliverySpeed; name: string }> = [
  { speed: "STANDARD", name: "Standard" },
  { speed: "EXPRESS", name: "Express" },
  { speed: "NEXT_DAY", name: "Next day" },
];

/** Last Sri Lanka hour a Standard booking can still be delivered the same day. */
export const STANDARD_SAME_DAY_CUTOFF_HOUR = 14;
/** Couriers work until 11 PM (login page); an Express delivery must fit before then. */
export const SERVICE_END_HOUR = 23;

/**
 * The delivery promise for a speed, given the time of booking in Sri Lanka (docs/architecture.md §8):
 * Standard is same day only when booked before 2 PM, Express only when 3 hours fit before 11 PM.
 */
export function speedPromise(speed: DeliverySpeed, now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Asia/Colombo", hour: "numeric", minute: "numeric", hourCycle: "h23",
  }).formatToParts(now);
  const hour = Number(parts.find((p) => p.type === "hour")?.value ?? 0);
  const minute = Number(parts.find((p) => p.type === "minute")?.value ?? 0);
  const minutes = hour * 60 + minute;
  switch (speed) {
    case "STANDARD":
      return minutes < STANDARD_SAME_DAY_CUTOFF_HOUR * 60 ? "Today by 8 PM" : "Tomorrow by 8 PM";
    case "EXPRESS":
      return minutes + 180 <= SERVICE_END_HOUR * 60 ? "Within 3 hours" : "Tomorrow morning";
    case "NEXT_DAY":
      return "Tomorrow by 6 PM";
  }
}
