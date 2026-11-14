import { describe, expect, it } from "vitest";
import { resolveRoute } from "./routes";

describe("resolveRoute", () => {
  it("opens public tracking with or without a login", () => {
    expect(resolveRoute("/track/sd-7k3q-9p2m", null)).toEqual({ kind: "tracking", trackingNo: "SD-7K3Q-9P2M" });
    expect(resolveRoute("/track", ["CUSTOMER"])).toEqual({ kind: "tracking", trackingNo: "" });
  });

  it("shows sign-in on the home page and portal addresses when signed out", () => {
    expect(resolveRoute("/", null)).toEqual({ kind: "login" });
    expect(resolveRoute("/merchant", null)).toEqual({ kind: "login" });
    expect(resolveRoute("/merchant/register", null)).toEqual({ kind: "login" });
    expect(resolveRoute("/merchant/pending", null)).toEqual({ kind: "login" });
  });

  it("handles merchant registration and pending approval routes when signed in", () => {
    expect(resolveRoute("/merchant/register", ["CUSTOMER"])).toEqual({ kind: "merchant-register" });
    expect(resolveRoute("/merchant/pending", ["CUSTOMER"])).toEqual({ kind: "merchant-pending" });
  });

  it("opens forgot password while signed out and reset password always", () => {
    expect(resolveRoute("/forgot-password", null)).toEqual({ kind: "forgot-password" });
    expect(resolveRoute("/forgot-password", ["CUSTOMER"])).toEqual({ kind: "home" });
    expect(resolveRoute("/reset-password", null)).toEqual({ kind: "reset-password" });
    expect(resolveRoute("/reset-password/", ["CUSTOMER"])).toEqual({ kind: "reset-password" });
  });

  it("handles the Google callback only while signed out", () => {
    expect(resolveRoute("/auth/callback", null)).toEqual({ kind: "callback" });
    expect(resolveRoute("/auth/callback", ["CUSTOMER"])).toEqual({ kind: "home" });
  });

  it("sends a signed-in user on the home page to their default dashboard", () => {
    expect(resolveRoute("/", ["CUSTOMER"])).toEqual({ kind: "home" });
  });

  it.each([
    ["/customer", "CUSTOMER"],
    ["/merchant", "MERCHANT"],
    ["/courier", "COURIER"],
    ["/dispatch", "DISPATCHER"],
    ["/admin", "ADMIN"],
  ] as const)("opens %s for a user with the %s role", (path, role) => {
    expect(resolveRoute(path, [role])).toEqual({ kind: "dashboard", role });
  });

  it("ignores a trailing slash", () => {
    expect(resolveRoute("/customer/", ["CUSTOMER"])).toEqual({ kind: "dashboard", role: "CUSTOMER" });
  });

  it("forbids another role's portal", () => {
    expect(resolveRoute("/admin", ["CUSTOMER"])).toEqual({ kind: "forbidden" });
    expect(resolveRoute("/dispatch", ["COURIER", "MERCHANT"])).toEqual({ kind: "forbidden" });
  });

  it("lets an admin who is also a dispatcher open both portals", () => {
    expect(resolveRoute("/admin", ["ADMIN", "DISPATCHER"])).toEqual({ kind: "dashboard", role: "ADMIN" });
    expect(resolveRoute("/dispatch", ["ADMIN", "DISPATCHER"])).toEqual({ kind: "dashboard", role: "DISPATCHER" });
  });

  it("reports unknown addresses as not found, signed in or not", () => {
    expect(resolveRoute("/no-such-page", null)).toEqual({ kind: "notFound" });
    expect(resolveRoute("/customer/settings", ["CUSTOMER"])).toEqual({ kind: "notFound" });
  });
});
