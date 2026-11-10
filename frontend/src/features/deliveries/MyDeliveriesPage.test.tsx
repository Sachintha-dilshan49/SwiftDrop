import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ParcelApiError, ParcelSummary, parcelApi } from "../../api/parcelApi";
import { MyDeliveriesPage } from "./MyDeliveriesPage";

vi.mock("../../api/parcelApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/parcelApi")>();
  return {
    ...actual,
    parcelApi: { getMyParcels: vi.fn(), getReceipt: vi.fn(), getParcel: vi.fn() },
  };
});

const getMyParcels = vi.mocked(parcelApi.getMyParcels);
const getReceipt = vi.mocked(parcelApi.getReceipt);
const getParcel = vi.mocked(parcelApi.getParcel);

function parcel(trackingNo: string, overrides: Partial<ParcelSummary> = {}): ParcelSummary {
  return {
    id: `id-${trackingNo}`,
    trackingNo,
    status: "IN_TRANSIT",
    pickupAddress: "123 Main St, Colombo",
    dropoffAddress: "456 High St, Kandy",
    recipientName: "Nimal Perera",
    recipientPhone: "+94771234567",
    size: "MEDIUM",
    weightKg: 2.5,
    feeAmount: 650,
    feeCurrency: "LKR",
    createdAt: "2026-11-06T09:00:00Z",
    updatedAt: null,
    ...overrides,
  };
}

function page(content: ParcelSummary[], totalElements = content.length, pageNumber = 0) {
  return { content, page: pageNumber, size: 10, totalElements };
}

describe("MyDeliveriesPage", () => {
  beforeEach(() => {
    getMyParcels.mockReset();
    getReceipt.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("lists the customer's parcels with status, route, recipient, fee and a tracking link", async () => {
    getMyParcels.mockResolvedValue(page([parcel("SD-7K3Q-9P2M"), parcel("SD-2222-3333", { status: "DELIVERED" })]));

    render(<MyDeliveriesPage token="token-1" />);

    const link = await screen.findByRole("link", { name: "SD-7K3Q-9P2M" });
    expect(link).toHaveAttribute("href", "/track/SD-7K3Q-9P2M");
    const row = link.closest("li") as HTMLElement;
    expect(within(row).getByText("In transit")).toBeInTheDocument();
    expect(within(row).getByText("123 Main St, Colombo → 456 High St, Kandy")).toBeInTheDocument();
    expect(within(row).getByText(/To Nimal Perera · LKR 650\.00 · Booked/)).toBeInTheDocument();
    const deliveredRow = screen.getByRole("link", { name: "SD-2222-3333" }).closest("li") as HTMLElement;
    expect(within(deliveredRow).getByText("Delivered")).toBeInTheDocument();
    expect(getMyParcels).toHaveBeenCalledWith("token-1", { page: 0, size: 10, status: undefined });
  });

  it("filters by status and starts again from the first page", async () => {
    getMyParcels.mockResolvedValue(page([parcel("SD-7K3Q-9P2M")]));
    render(<MyDeliveriesPage token="token-1" />);
    await screen.findByRole("link", { name: "SD-7K3Q-9P2M" });

    fireEvent.change(screen.getByLabelText("Status"), { target: { value: "DELIVERED" } });

    await waitFor(() =>
      expect(getMyParcels).toHaveBeenLastCalledWith("token-1", { page: 0, size: 10, status: "DELIVERED" }));
  });

  it("moves between pages when there are more than ten parcels", async () => {
    getMyParcels.mockResolvedValue(page([parcel("SD-7K3Q-9P2M")], 21));
    render(<MyDeliveriesPage token="token-1" />);

    expect(await screen.findByText("Page 1 of 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    await waitFor(() =>
      expect(getMyParcels).toHaveBeenLastCalledWith("token-1", { page: 1, size: 10, status: undefined }));
    expect(await screen.findByText("Page 2 of 3")).toBeInTheDocument();
  });

  it("says when there are no deliveries yet", async () => {
    getMyParcels.mockResolvedValue(page([]));

    render(<MyDeliveriesPage token="token-1" />);

    expect(await screen.findByText("You have no deliveries yet.")).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Pages" })).not.toBeInTheDocument();
  });

  it("asks the customer to sign in again when the login has expired", async () => {
    getMyParcels.mockRejectedValue(new ParcelApiError(401, "Unauthorized"));

    render(<MyDeliveriesPage token="expired" />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Your session has expired. Please sign out and sign in again.");
  });

  it("downloads the receipt of a parcel as a PDF file", async () => {
    getMyParcels.mockResolvedValue(page([parcel("SD-7K3Q-9P2M")]));
    getReceipt.mockResolvedValue(new Blob(["%PDF-1.4"], { type: "application/pdf" }));
    // jsdom has no object URLs, so add just these two functions
    const createObjectURL = vi.fn(() => "blob:receipt");
    Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() });
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    render(<MyDeliveriesPage token="token-1" />);

    fireEvent.click(await screen.findByRole("button", { name: "Download the receipt for SD-7K3Q-9P2M" }));

    await waitFor(() => expect(click).toHaveBeenCalled());
    expect(getReceipt).toHaveBeenCalledWith("id-SD-7K3Q-9P2M", "token-1");
    expect(createObjectURL).toHaveBeenCalled();
    click.mockRestore();
  });

  it("shows an error when the receipt cannot be downloaded", async () => {
    getMyParcels.mockResolvedValue(page([parcel("SD-7K3Q-9P2M")]));
    getReceipt.mockRejectedValue(new ParcelApiError(503, "Service temporarily unavailable"));
    render(<MyDeliveriesPage token="token-1" />);

    fireEvent.click(await screen.findByRole("button", { name: "Download the receipt for SD-7K3Q-9P2M" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Could not download the receipt. Please try again.");
  });

  it("opens a delivery's details and goes back to the same filtered list", async () => {
    getMyParcels.mockResolvedValue(page([parcel("SD-7K3Q-9P2M")]));
    getParcel.mockReturnValue(new Promise(() => {}));
    render(<MyDeliveriesPage token="token-1" />);
    await screen.findByRole("link", { name: "SD-7K3Q-9P2M" });
    fireEvent.change(screen.getByLabelText("Status"), { target: { value: "IN_TRANSIT" } });
    await screen.findByRole("link", { name: "SD-7K3Q-9P2M" });

    fireEvent.click(screen.getByRole("button", { name: "Show the details of SD-7K3Q-9P2M" }));

    expect(screen.getByRole("heading", { name: "Delivery details" })).toBeInTheDocument();
    expect(getParcel).toHaveBeenCalledWith("id-SD-7K3Q-9P2M", "token-1");

    fireEvent.click(screen.getByRole("button", { name: "← Back to my deliveries" }));

    expect(screen.getByRole("heading", { name: "My deliveries" })).toBeInTheDocument();
    expect(screen.getByLabelText("Status")).toHaveValue("IN_TRANSIT");
  });
});
