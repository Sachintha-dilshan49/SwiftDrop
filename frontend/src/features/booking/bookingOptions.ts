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
export const SPEED_OPTIONS: ReadonlyArray<{ speed: DeliverySpeed; name: string; promise: string }> = [
  { speed: "STANDARD", name: "Standard", promise: "Today by 8 PM" },
  { speed: "EXPRESS", name: "Express", promise: "Within 3 hours" },
  { speed: "NEXT_DAY", name: "Next day", promise: "Tomorrow by 6 PM" },
];
