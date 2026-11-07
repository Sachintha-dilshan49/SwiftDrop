import { describe, expect, it } from "vitest";
import { STATUS_LABELS, formatDateTime, formatMoney } from "./parcelFormat";

describe("parcel formatting", () => {
  it.each([
    [650, "LKR 650.00"],
    [1250.5, "LKR 1,250.50"],
    [0, "LKR 0.00"],
  ])("shows %s as %s", (amount, expected) => {
    expect(formatMoney(amount, "LKR")).toBe(expected);
  });

  it("shows text that is not a date unchanged", () => {
    expect(formatDateTime("not a date")).toBe("not a date");
  });

  it("formats a valid timestamp", () => {
    expect(formatDateTime("2026-11-06T09:00:00Z")).not.toBe("2026-11-06T09:00:00Z");
  });

  it("has a label for every status", () => {
    expect(Object.keys(STATUS_LABELS)).toHaveLength(6);
  });
});
