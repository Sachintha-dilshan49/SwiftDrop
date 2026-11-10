import { describe, expect, it } from "vitest";
import { ZONES } from "./bookingOptions";
import { BookingFormValues, EMPTY_BOOKING, normalizePhone, toCreateRequest, validateBooking } from "./bookingValidation";

const valid: BookingFormValues = {
  pickupAddress: " 123 Main St, Colombo ",
  pickupZone: "COLOMBO_SOUTH",
  dropoffAddress: "456 High St, Kandy",
  dropoffZone: "KANDY",
  recipientName: "Nimal Perera",
  recipientPhone: "077 123-4567",
  size: "MEDIUM",
  weightKg: "2.5",
  speed: "EXPRESS",
  promoCode: "",
};

describe("validateBooking", () => {
  it("accepts a complete booking", () => {
    expect(validateBooking(valid)).toEqual({});
  });

  it("lists every missing field of an empty form", () => {
    expect(Object.keys(validateBooking(EMPTY_BOOKING)).sort()).toEqual([
      "dropoffAddress", "dropoffZone", "pickupAddress", "pickupZone", "recipientName", "recipientPhone", "size",
      "weightKg",
    ]);
  });

  it.each([
    ["12345", "Enter 9 to 15 digits, optionally starting with +"],
    ["+94 77 abc 4567", "Enter 9 to 15 digits, optionally starting with +"],
  ])("rejects the phone number %j", (recipientPhone, message) => {
    expect(validateBooking({ ...valid, recipientPhone }).recipientPhone).toBe(message);
  });

  it.each(["0", "-1", "2.505", "abc", "10000"])("rejects the weight %j", (weightKg) => {
    expect(validateBooking({ ...valid, weightKg }).weightKg).toBe("Enter a weight above 0 kg with up to 2 decimals");
  });

  it("rejects a weight the size cannot take", () => {
    expect(validateBooking({ ...valid, size: "SMALL", weightKg: "5.01" }).weightKg)
      .toBe("A small parcel can weigh at most 5 kg");
    expect(validateBooking({ ...valid, size: "EXTRA_LARGE", weightKg: "40" }).weightKg).toBeUndefined();
  });

  it("rejects text that is longer than the backend allows", () => {
    expect(validateBooking({ ...valid, recipientName: "x".repeat(151) }).recipientName)
      .toBe("Recipient name cannot exceed 150 characters");
  });

  it("checks the promo code only when one is given", () => {
    expect(validateBooking({ ...valid, promoCode: "DROP 100" }).promoCode)
      .toBe("Promo code must be 1 to 30 letters, digits, - or _");
    expect(validateBooking({ ...valid, promoCode: "DROP100" }).promoCode).toBeUndefined();
  });
});

describe("toCreateRequest", () => {
  it("trims text, cleans the phone number, converts the weight and leaves out an empty promo code", () => {
    expect(toCreateRequest(valid)).toEqual({
      pickupAddress: "123 Main St, Colombo",
      pickupZone: "COLOMBO_SOUTH",
      dropoffAddress: "456 High St, Kandy",
      dropoffZone: "KANDY",
      recipientName: "Nimal Perera",
      recipientPhone: "0771234567",
      size: "MEDIUM",
      weightKg: 2.5,
      speed: "EXPRESS",
    });
  });

  it("sends a promo code when one is given", () => {
    expect(toCreateRequest({ ...valid, promoCode: " DROP100 " }).promoCode).toBe("DROP100");
  });
});

describe("booking options", () => {
  it("uses the nine zone codes agreed with pricing", () => {
    expect(ZONES.map((zone) => zone.code)).toEqual([
      "COLOMBO_CENTRAL", "COLOMBO_SOUTH", "COLOMBO_NORTH", "KOTTE_RAJAGIRIYA", "NUGEGODA_MAHARAGAMA",
      "DEHIWALA_MT_LAVINIA", "KANDY", "GALLE", "NEGOMBO",
    ]);
  });

  it("removes spaces and dashes from phone numbers", () => {
    expect(normalizePhone("+94 77-123 4567")).toBe("+94771234567");
  });
});
