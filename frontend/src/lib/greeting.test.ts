import { describe, expect, it } from "vitest";
import { greeting } from "./greeting";

describe("greeting", () => {
  it("follows the time of day in Sri Lanka (UTC+5:30)", () => {
    expect(greeting(new Date("2026-11-14T02:00:00Z"))).toBe("Good morning"); // 07:30
    expect(greeting(new Date("2026-11-14T07:00:00Z"))).toBe("Good afternoon"); // 12:30
    expect(greeting(new Date("2026-11-14T13:30:00Z"))).toBe("Good evening"); // 19:00
  });
});
