// Client for pricing-service through the gateway. VITE_API_BASE_URL is the gateway prefix ("/api" by
// default, like the other clients); every path below adds "/v1/...".
import type { DeliverySpeed, ParcelSize } from "./parcelApi";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api";

export type RouteType = "SAME_ZONE" | "NEIGHBOURING" | "COLOMBO_SUBURBS" | "INTER_CITY";

/** What to price; the same fields as the booking (docs/architecture.md §8). */
export interface EstimateQuery {
  pickupZone: string;
  dropoffZone: string;
  weightKg: number;
  size: ParcelSize;
  speed: DeliverySpeed;
  promoCode?: string;
}

/** GET /api/v1/pricing/estimate: the expected fee and how it is made up. */
export interface PricingEstimate {
  routeType: RouteType;
  baseFee: number;
  weightFee: number;
  sizeFee: number;
  expressFee: number;
  discount: number;
  total: number;
  currency: string;
}

export class PricingApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "PricingApiError";
    this.status = status;
  }
}

export const pricingApi = {
  /** Public and rate-limited; no token. The fee charged is still the quote parcel-service takes when booking. */
  async getEstimate(query: EstimateQuery): Promise<PricingEstimate> {
    const params = new URLSearchParams({
      pickupZone: query.pickupZone,
      dropoffZone: query.dropoffZone,
      weightKg: String(query.weightKg),
      size: query.size,
      speed: query.speed,
    });
    if (query.promoCode) {
      params.set("promoCode", query.promoCode);
    }
    const response = await fetch(`${API_BASE}/v1/pricing/estimate?${params}`, {
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      let detail: string | undefined;
      try {
        const body = (await response.json()) as { detail?: unknown };
        detail = typeof body.detail === "string" ? body.detail : undefined;
      } catch {
        // Not a ProblemDetail body; use the fallback message.
      }
      throw new PricingApiError(response.status, detail ?? "Could not get a price. Please try again.");
    }
    return (await response.json()) as PricingEstimate;
  },
};
