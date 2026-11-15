import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ParcelResponse, parcelApi } from "../../api/parcelApi";
import { pricingApi } from "../../api/pricingApi";
import { CustomerHome } from "./CustomerHome";

vi.mock("../../api/parcelApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/parcelApi")>();
  return { ...actual, parcelApi: { createParcel: vi.fn(), getMyParcels: vi.fn(), getReceipt: vi.fn() } };
});

vi.mock("../../api/pricingApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/pricingApi")>();
  return { ...actual, pricingApi: { getEstimate: vi.fn() } };
});

const createParcel = vi.mocked(parcelApi.createParcel);
const getMyParcels = vi.mocked(parcelApi.getMyParcels);

describe("CustomerHome", () => {
  beforeEach(() => {
    createParcel.mockReset();
    getMyParcels.mockReset();
    getMyParcels.mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0 });
    vi.mocked(pricingApi.getEstimate).mockResolvedValue({ routeType: "INTER_CITY", baseFee: 650, weightFee: 0,
      sizeFee: 0, expressFee: 0, discount: 0, total: 650, currency: "LKR" });
  });

  it("offers merchant registration to a customer and the portal to a merchant", () => {
    const { unmount } = render(<CustomerHome token="token-1" />);
    expect(screen.getByRole("button", { name: "Register as a merchant" })).toBeInTheDocument();
    unmount();

    render(<CustomerHome token="token-1" isMerchant />);
    expect(screen.getByRole("button", { name: "Open merchant portal" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Register as a merchant" })).not.toBeInTheDocument();
  });

  it("opens and closes the booking form above the delivery list", async () => {
    render(<CustomerHome token="token-1" />);
    expect(await screen.findByText("You have no deliveries yet.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Book a delivery" }));
    expect(screen.getByRole("heading", { name: "Book a delivery" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.queryByRole("heading", { name: "Book a delivery" })).not.toBeInTheDocument();
  });

  it("loads the delivery list again after a booking", async () => {
    createParcel.mockResolvedValue({ id: "p-1", trackingNo: "SD-7K3Q-9P2M", feeAmount: 650,
      feeCurrency: "LKR" } as ParcelResponse);
    render(<CustomerHome token="token-1" />);
    await waitFor(() => expect(getMyParcels).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole("button", { name: "Book a delivery" }));

    const type = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
    type("Pickup address", "123 Main St, Colombo");
    type("Pickup zone", "COLOMBO_SOUTH");
    type("Drop-off address", "456 High St, Kandy");
    type("Drop-off zone", "KANDY");
    type("Recipient name", "Nimal Perera");
    type("Recipient phone", "0771234567");
    type("Size", "SMALL");
    type("Weight (kg)", "1");
    fireEvent.click(screen.getByRole("button", { name: "See the fare" }));
    fireEvent.click(await screen.findByRole("button", { name: "Confirm & book · LKR 650.00" }));

    expect(await screen.findByRole("heading", { name: "Delivery booked" })).toBeInTheDocument();
    await waitFor(() => expect(getMyParcels).toHaveBeenCalledTimes(2));
  });
});
