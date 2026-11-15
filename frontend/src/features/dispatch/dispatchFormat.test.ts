import { describe, expect, it } from "vitest";
import { courierName, waitingTime } from "./dispatchFormat";

describe("waitingTime", () => {
  const now = new Date("2026-11-10T12:00:00Z");

  it("rounds down to minutes, hours or days", () => {
    expect(waitingTime("2026-11-10T11:59:30Z", now)).toBe("just now");
    expect(waitingTime("2026-11-10T11:48:00Z", now)).toBe("12 min");
    expect(waitingTime("2026-11-10T09:00:00Z", now)).toBe("3 h");
    expect(waitingTime("2026-11-09T11:00:00Z", now)).toBe("1 day");
    expect(waitingTime("2026-11-07T12:00:00Z", now)).toBe("3 days");
  });

  it("treats an unreadable date as just now", () => {
    expect(waitingTime("not a date", now)).toBe("just now");
  });
});

describe("courierName", () => {
  it("uses the first name, or a short ID when there is none", () => {
    expect(courierName({ courierId: "c-1", firstName: "Kasun" })).toBe("Kasun");
    expect(courierName({ courierId: "abcdef12-3456-7890", firstName: null })).toBe("Courier abcdef12");
  });
});
