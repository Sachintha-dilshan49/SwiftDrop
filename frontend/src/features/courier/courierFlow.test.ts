import { describe, expect, it } from "vitest";
import type { CourierDelivery } from "../../api/dispatchApi";
import { currentStatus, nextSteps, photoProblem } from "./courierFlow";

function delivery(overrides: Partial<CourierDelivery> = {}): CourierDelivery {
  return {
    assignmentId: "a-1",
    parcelId: "p-1",
    status: "ASSIGNED",
    parcelStatus: "CREATED",
    assignedAt: "2026-11-11T04:00:00Z",
    trackingNo: "SD-0001",
    pickupAddress: "Liberty Plaza",
    dropoffAddress: "Rajagiriya Flyover",
    recipientName: "Nimal",
    size: "SMALL",
    weightKg: 1.2,
    ...overrides,
  };
}

describe("nextSteps", () => {
  it("offers one step at a time, and delivered or failed at the door", () => {
    expect(nextSteps(delivery({ parcelStatus: "CREATED" }))).toEqual(["PICKED_UP"]);
    expect(nextSteps(delivery({ status: "IN_PROGRESS", parcelStatus: "PICKED_UP" }))).toEqual(["IN_TRANSIT"]);
    expect(nextSteps(delivery({ status: "IN_PROGRESS", parcelStatus: "IN_TRANSIT" }))).toEqual(["OUT_FOR_DELIVERY"]);
    expect(nextSteps(delivery({ status: "IN_PROGRESS", parcelStatus: "OUT_FOR_DELIVERY" })))
      .toEqual(["DELIVERED", "FAILED"]);
  });

  it("offers nothing once the delivery is closed", () => {
    expect(nextSteps(delivery({ status: "COMPLETED", parcelStatus: "DELIVERED" }))).toEqual([]);
    expect(nextSteps(delivery({ status: "FAILED", parcelStatus: "FAILED" }))).toEqual([]);
  });

  it("offers every forward step when an older delivery has no recorded status", () => {
    expect(nextSteps(delivery({ status: "IN_PROGRESS", parcelStatus: null })))
      .toEqual(["PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED"]);
  });
});

describe("currentStatus", () => {
  it("falls back to the assignment status for older deliveries", () => {
    expect(currentStatus(delivery({ status: "ASSIGNED", parcelStatus: null }))).toBe("CREATED");
    expect(currentStatus(delivery({ status: "COMPLETED", parcelStatus: null }))).toBe("DELIVERED");
    expect(currentStatus(delivery({ status: "IN_PROGRESS", parcelStatus: null }))).toBeNull();
  });
});

describe("photoProblem", () => {
  it("accepts JPEG, PNG and WebP up to 5 MB", () => {
    expect(photoProblem(new File(["x"], "door.jpg", { type: "image/jpeg" }))).toBeNull();
    expect(photoProblem(new File(["x"], "door.webp", { type: "image/webp" }))).toBeNull();
  });

  it("rejects other files and photos over 5 MB", () => {
    expect(photoProblem(new File(["x"], "doc.pdf", { type: "application/pdf" }))).toMatch(/JPEG, PNG or WebP/);
    const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], "big.jpg", { type: "image/jpeg" });
    expect(photoProblem(big)).toMatch(/5 MB/);
  });
});
