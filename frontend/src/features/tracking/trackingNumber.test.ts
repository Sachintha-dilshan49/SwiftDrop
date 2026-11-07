import { describe, expect, it } from "vitest";
import { isValidTrackingNo, normalizeTrackingNo, trackingNumberFromPath, trackingPath } from "./trackingNumber";

describe("tracking numbers", () => {
  it.each([
    ["SD-7K3Q-9P2M", true],
    ["SD-2222-2222", true],
    ["SD-0K3Q-9P2M", false], // 0, 1, I and O are never used
    ["SD-7K3Q-9P2", false],
    ["XX-7K3Q-9P2M", false],
    ["", false],
  ])("%s is valid: %s", (value, expected) => {
    expect(isValidTrackingNo(value)).toBe(expected);
  });

  it("trims and upper-cases input", () => {
    expect(normalizeTrackingNo("  sd-7k3q-9p2m ")).toBe("SD-7K3Q-9P2M");
  });

  it.each([
    ["/track", ""],
    ["/track/", ""],
    ["/track/sd-7k3q-9p2m", "SD-7K3Q-9P2M"],
    ["/track/SD-7K3Q-9P2M/", "SD-7K3Q-9P2M"],
    ["/track/%E0%A4%A", ""],
  ])("%s is the public tracking page for %j", (path, expected) => {
    expect(trackingNumberFromPath(path)).toBe(expected);
  });

  it.each(["/", "/auth/callback", "/tracking", "/track/SD-7K3Q-9P2M/extra", "/customer"])(
    "%s is not the tracking page",
    (path) => {
      expect(trackingNumberFromPath(path)).toBeNull();
    },
  );

  it("builds the page address that emails link to", () => {
    expect(trackingPath("SD-7K3Q-9P2M")).toBe("/track/SD-7K3Q-9P2M");
  });
});
