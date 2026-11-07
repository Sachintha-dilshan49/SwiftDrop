import type { CreateParcelRequest, DeliverySpeed, ParcelSize } from "../../api/parcelApi";
import { SIZE_OPTIONS } from "./bookingOptions";

/** The booking form as typed; the field names match CreateParcelRequest, so server errors map 1:1. */
export interface BookingFormValues {
  pickupAddress: string;
  pickupZone: string;
  dropoffAddress: string;
  dropoffZone: string;
  recipientName: string;
  recipientPhone: string;
  size: ParcelSize | "";
  weightKg: string;
  speed: DeliverySpeed;
  promoCode: string;
}

export type BookingErrors = Partial<Record<keyof BookingFormValues, string>>;

export const EMPTY_BOOKING: BookingFormValues = {
  pickupAddress: "",
  pickupZone: "",
  dropoffAddress: "",
  dropoffZone: "",
  recipientName: "",
  recipientPhone: "",
  size: "",
  weightKg: "",
  speed: "STANDARD",
  promoCode: "",
};

// Same patterns as parcel-service's ValidationPatterns
const PHONE = /^\+?[0-9]{9,15}$/;
const PROMO_CODE = /^[A-Za-z0-9_-]{1,30}$/;
const WEIGHT = /^\d{1,4}(\.\d{1,2})?$/;

/** Spaces and dashes people type in phone numbers are removed before checking and sending. */
export function normalizePhone(phone: string): string {
  return phone.replace(/[\s-]/g, "");
}

function checkText(value: string, label: string, max: number): string | undefined {
  if (!value.trim()) {
    return `${label} is required`;
  }
  return value.trim().length > max ? `${label} cannot exceed ${max} characters` : undefined;
}

/** The same rules as the backend's CreateParcelRequest, plus the weight limit of each size (architecture §8). */
export function validateBooking(values: BookingFormValues): BookingErrors {
  const errors: BookingErrors = {
    pickupAddress: checkText(values.pickupAddress, "Pickup address", 500),
    pickupZone: values.pickupZone ? undefined : "Choose the pickup zone",
    dropoffAddress: checkText(values.dropoffAddress, "Drop-off address", 500),
    dropoffZone: values.dropoffZone ? undefined : "Choose the drop-off zone",
    recipientName: checkText(values.recipientName, "Recipient name", 150),
    size: values.size ? undefined : "Choose a parcel size",
  };

  const phone = normalizePhone(values.recipientPhone);
  if (!phone) {
    errors.recipientPhone = "Recipient phone is required";
  } else if (!PHONE.test(phone)) {
    errors.recipientPhone = "Enter 9 to 15 digits, optionally starting with +";
  }

  const weight = values.weightKg.trim();
  const sizeOption = SIZE_OPTIONS.find((option) => option.size === values.size);
  if (!weight) {
    errors.weightKg = "Weight is required";
  } else if (!WEIGHT.test(weight) || Number(weight) <= 0) {
    errors.weightKg = "Enter a weight above 0 kg with up to 2 decimals";
  } else if (sizeOption && Number(weight) > sizeOption.maxWeightKg) {
    errors.weightKg = `A ${sizeOption.name.toLowerCase()} parcel can weigh at most ${sizeOption.maxWeightKg} kg`;
  }

  const promoCode = values.promoCode.trim();
  if (promoCode && !PROMO_CODE.test(promoCode)) {
    errors.promoCode = "Promo code must be 1 to 30 letters, digits, - or _";
  }

  return Object.fromEntries(Object.entries(errors).filter(([, message]) => message)) as BookingErrors;
}

/** The request for valid form values: trimmed text, the phone without spaces, no empty promo code. */
export function toCreateRequest(values: BookingFormValues): CreateParcelRequest {
  const promoCode = values.promoCode.trim();
  return {
    pickupAddress: values.pickupAddress.trim(),
    pickupZone: values.pickupZone,
    dropoffAddress: values.dropoffAddress.trim(),
    dropoffZone: values.dropoffZone,
    recipientName: values.recipientName.trim(),
    recipientPhone: normalizePhone(values.recipientPhone),
    size: values.size as ParcelSize,
    weightKg: Number(values.weightKg.trim()),
    speed: values.speed,
    ...(promoCode ? { promoCode } : {}),
  };
}
