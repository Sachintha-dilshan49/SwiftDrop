import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PricingApiError, PricingEstimate, pricingApi } from "../../api/pricingApi";
import { BookingFormValues } from "./bookingValidation";
import { FareStep } from "./FareStep";

vi.mock("../../api/pricingApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/pricingApi")>();
  return { ...actual, pricingApi: { getEstimate: vi.fn() } };
});

const getEstimate = vi.mocked(pricingApi.getEstimate);

const values: BookingFormValues = {
  pickupAddress: "No. 24, Duplication Road",
  pickupZone: "COLOMBO_SOUTH",
  dropoffAddress: "Liberty Plaza",
  dropoffZone: "COLOMBO_CENTRAL",
  recipientName: "Nimal Fernando",
  recipientPhone: "0771234567",
  size: "SMALL",
  weightKg: "1.2",
  speed: "EXPRESS",
  promoCode: "DROP100",
};

const express: PricingEstimate = {
  routeType: "NEIGHBOURING",
  baseFee: 350,
  weightFee: 40,
  sizeFee: 0,
  expressFee: 100,
  discount: 100,
  total: 390,
  currency: "LKR",
};

function renderStep(overrides: Partial<Parameters<typeof FareStep>[0]> = {}) {
  const props = {
    values,
    submitting: false,
    error: null,
    onSpeedChange: vi.fn(),
    onBack: vi.fn(),
    onConfirm: vi.fn(),
    ...overrides,
  };
  render(<FareStep {...props} />);
  return props;
}

describe("FareStep", () => {
  beforeEach(() => {
    getEstimate.mockReset();
  });

  it("shows the breakdown of the chosen speed, with the promo discount", async () => {
    getEstimate.mockImplementation(async ({ speed }) =>
      speed === "EXPRESS" ? express : { ...express, expressFee: 0, total: speed === "STANDARD" ? 290 : 240 },
    );
    const { onConfirm } = renderStep();

    const breakdown = await screen.findByRole("region", { name: "Fare breakdown" });
    expect(within(breakdown).getByText("Base fee · neighbouring zone").nextSibling).toHaveTextContent("LKR 350.00");
    expect(within(breakdown).getByText("Weight · 1.2 kg")).toBeInTheDocument();
    expect(within(breakdown).getByText("Express delivery")).toBeInTheDocument();
    expect(within(breakdown).getByText("Promo DROP100").nextSibling).toHaveTextContent("− LKR 100.00");
    expect(within(breakdown).queryByText(/^Size/)).not.toBeInTheDocument();
    expect(screen.getByText(/No\. 24, Duplication Road → Liberty Plaza · Small, 1\.2 kg/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Confirm & book · LKR 390.00" }));
    expect(onConfirm).toHaveBeenCalled();
    expect(getEstimate).toHaveBeenCalledTimes(3);
  });

  it("changes the speed when another card is chosen", async () => {
    getEstimate.mockResolvedValue(express);
    const { onSpeedChange } = renderStep();

    fireEvent.click(await screen.findByRole("radio", { name: /Next day/ }));

    expect(onSpeedChange).toHaveBeenCalledWith("NEXT_DAY");
  });

  it("explains what cannot be priced and does not offer booking", async () => {
    getEstimate.mockRejectedValue(new PricingApiError(400, "Parcels over 40 kg cannot be delivered"));
    renderStep();

    expect(await screen.findByRole("alert")).toHaveTextContent("Parcels over 40 kg cannot be delivered");
    expect(screen.queryByRole("button", { name: /Confirm & book/ })).not.toBeInTheDocument();
  });

  it("tries again when pricing was not reachable", async () => {
    getEstimate
      .mockRejectedValueOnce(new PricingApiError(503, "Unavailable"))
      .mockRejectedValueOnce(new Error("x"))
      .mockRejectedValueOnce(new Error("x"))
      .mockResolvedValue(express);
    renderStep();

    expect(await screen.findByRole("alert")).toHaveTextContent("We could not get a price right now.");
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(await screen.findByRole("button", { name: "Confirm & book · LKR 390.00" })).toBeInTheDocument();
  });

  it("goes back to the details", async () => {
    getEstimate.mockResolvedValue(express);
    const { onBack } = renderStep();

    fireEvent.click(screen.getByRole("button", { name: "← Edit details" }));

    expect(onBack).toHaveBeenCalled();
  });
});
