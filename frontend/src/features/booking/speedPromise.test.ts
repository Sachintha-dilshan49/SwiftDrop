import { describe, expect, it } from "vitest";
import { speedPromise } from "./bookingOptions";

// Sri Lanka is UTC+5:30
const at = (colomboTime: string) => new Date(`2026-11-14T${colomboTime}:00+05:30`);

describe("speedPromise", () => {
  it("promises Standard the same day only when booked before 2 PM", () => {
    expect(speedPromise("STANDARD", at("09:00"))).toBe("Today by 8 PM");
    expect(speedPromise("STANDARD", at("13:59"))).toBe("Today by 8 PM");
    expect(speedPromise("STANDARD", at("19:20"))).toBe("Tomorrow by 8 PM");
  });

  it("promises Express within 3 hours only when that fits before 11 PM", () => {
    expect(speedPromise("EXPRESS", at("19:20"))).toBe("Within 3 hours");
    expect(speedPromise("EXPRESS", at("20:01"))).toBe("Tomorrow morning");
  });

  it("always promises Next day for tomorrow", () => {
    expect(speedPromise("NEXT_DAY", at("23:30"))).toBe("Tomorrow by 6 PM");
  });
});
