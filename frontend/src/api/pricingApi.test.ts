import { afterEach, describe, expect, it, vi } from "vitest";
import { PricingApiError, pricingApi } from "./pricingApi";

const query = { pickupZone: "COLOMBO_SOUTH", dropoffZone: "KANDY", weightKg: 2.5, size: "MEDIUM" as const };

describe("pricingApi.getEstimate", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("asks the public estimate endpoint without a token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ total: 750, currency: "LKR" })));
    vi.stubGlobal("fetch", fetchMock);

    const estimate = await pricingApi.getEstimate({ ...query, speed: "EXPRESS", promoCode: "DROP100" });

    expect(estimate.total).toBe(750);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "/api/v1/pricing/estimate?pickupZone=COLOMBO_SOUTH&dropoffZone=KANDY&weightKg=2.5&size=MEDIUM&speed=EXPRESS&promoCode=DROP100",
    );
    expect(init.headers).toEqual({ Accept: "application/json" });
  });

  it("leaves out an empty promo code", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}"));
    vi.stubGlobal("fetch", fetchMock);

    await pricingApi.getEstimate({ ...query, speed: "STANDARD" });

    expect(fetchMock.mock.calls[0][0]).not.toContain("promoCode");
  });

  it("passes on the server's explanation", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: "Unknown promo code" }), { status: 400 })),
    );

    await expect(pricingApi.getEstimate({ ...query, speed: "STANDARD" })).rejects.toEqual(
      new PricingApiError(400, "Unknown promo code"),
    );
  });
});
