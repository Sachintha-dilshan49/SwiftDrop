import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ParcelApiError, ParcelResponse, TrackingStreamHandlers, parcelApi } from "../../api/parcelApi";
import { ParcelDetails } from "./ParcelDetails";

vi.mock("../../api/parcelApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/parcelApi")>();
  return { ...actual, parcelApi: { getParcel: vi.fn(), subscribeTrackingStream: vi.fn() } };
});

const getParcel = vi.mocked(parcelApi.getParcel);
const subscribeTrackingStream = vi.mocked(parcelApi.subscribeTrackingStream);
const closeStream = vi.fn();
let stream: TrackingStreamHandlers | null = null;

const parcel: ParcelResponse = {
  id: "p-1",
  trackingNo: "SD-7K3Q-9P2M",
  customerId: "c-1",
  merchantId: null,
  courierId: "k-1",
  pickupAddress: "123 Main St, Colombo",
  dropoffAddress: "456 High St, Kandy",
  recipientName: "Nimal Perera",
  recipientPhone: "+94771234567",
  size: "MEDIUM",
  weightKg: 2.5,
  feeAmount: 750,
  feeCurrency: "LKR",
  status: "IN_TRANSIT",
  failureReason: null,
  createdAt: "2026-11-06T09:00:00Z",
  updatedAt: "2026-11-06T12:00:00Z",
  history: [
    { status: "CREATED", changedBy: "c-1", changedByRole: "CUSTOMER", note: "Delivery booking created",
      proofReference: null, createdAt: "2026-11-06T09:00:00Z" },
    { status: "PICKED_UP", changedBy: "k-1", changedByRole: "COURIER", note: "Picked up at the gate",
      proofReference: null, createdAt: "2026-11-06T10:30:00Z" },
    { status: "IN_TRANSIT", changedBy: "k-1", changedByRole: "COURIER", note: null,
      proofReference: null, createdAt: "2026-11-06T12:00:00Z" },
  ],
};

describe("ParcelDetails", () => {
  beforeEach(() => {
    getParcel.mockReset();
    closeStream.mockReset();
    stream = null;
    subscribeTrackingStream.mockReset();
    subscribeTrackingStream.mockImplementation((_trackingNo, handlers) => {
      stream = handlers;
      return closeStream;
    });
  });

  it("shows the delivery, its fee and the full history newest first with who made each change", async () => {
    getParcel.mockResolvedValue(parcel);

    render(<ParcelDetails parcelId="p-1" token="token-1" onBack={vi.fn()} />);

    expect(await screen.findByRole("heading", { name: "SD-7K3Q-9P2M" })).toBeInTheDocument();
    expect(screen.getByText("Nimal Perera, +94771234567")).toBeInTheDocument();
    expect(screen.getByText("Medium, 2.5 kg")).toBeInTheDocument();
    expect(screen.getByText("LKR 750.00")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Track live" })).toHaveAttribute("href", "/track/SD-7K3Q-9P2M");

    const entries = within(screen.getByRole("list")).getAllByRole("listitem");
    expect(entries).toHaveLength(3);
    expect(entries[0]).toHaveTextContent("In transit");
    expect(entries[1]).toHaveTextContent("Picked up");
    expect(entries[1]).toHaveTextContent("By: Courier");
    expect(entries[1]).toHaveTextContent("Picked up at the gate");
    expect(entries[2]).toHaveTextContent("By: You");
    expect(getParcel).toHaveBeenCalledWith("p-1", "token-1");
  });

  it("explains why a failed delivery failed and notes recorded proof", async () => {
    getParcel.mockResolvedValue({
      ...parcel,
      status: "FAILED",
      failureReason: "Recipient not at home",
      history: [...parcel.history, { status: "FAILED", changedBy: "k-1", changedByRole: "COURIER",
        note: null, proofReference: "proofs/p-1.jpg", createdAt: "2026-11-06T15:00:00Z" }],
    });

    render(<ParcelDetails parcelId="p-1" token="token-1" onBack={vi.fn()} />);

    expect(await screen.findByText("Recipient not at home")).toBeInTheDocument();
    expect(screen.getByText("Why the delivery failed:")).toBeInTheDocument();
    expect(screen.getByText("Proof of delivery recorded")).toBeInTheDocument();
  });

  it("says when the delivery cannot be found", async () => {
    getParcel.mockRejectedValue(new ParcelApiError(404, "Parcel not found"));

    render(<ParcelDetails parcelId="other" token="token-1" onBack={vi.fn()} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("This delivery could not be found.");
  });

  it("goes back to the list", async () => {
    getParcel.mockResolvedValue(parcel);
    const onBack = vi.fn();
    render(<ParcelDetails parcelId="p-1" token="token-1" onBack={onBack} />);

    fireEvent.click(screen.getByRole("button", { name: "← Back to my deliveries" }));

    expect(onBack).toHaveBeenCalled();
  });

  describe("live updates", () => {
    const outForDelivery: ParcelResponse = {
      ...parcel,
      status: "OUT_FOR_DELIVERY",
      history: [...parcel.history, { status: "OUT_FOR_DELIVERY", changedBy: "k-1", changedByRole: "COURIER",
        note: "On the way to Kandy", proofReference: null, createdAt: "2026-11-06T14:00:00Z" }],
    };

    it("reloads the delivery when its status changes, so the new entry shows who made it", async () => {
      getParcel.mockResolvedValueOnce(parcel).mockResolvedValueOnce(outForDelivery);
      render(<ParcelDetails parcelId="p-1" token="token-1" onBack={vi.fn()} />);
      await screen.findByRole("heading", { name: "SD-7K3Q-9P2M" });
      expect(subscribeTrackingStream).toHaveBeenCalledWith("SD-7K3Q-9P2M", expect.any(Object));

      act(() => stream?.onLiveChange?.(true));
      expect(screen.getByText("Live: updates appear automatically")).toBeInTheDocument();

      act(() =>
        stream?.onStatusChange({ trackingNo: "SD-7K3Q-9P2M", status: "OUT_FOR_DELIVERY",
          previousStatus: "IN_TRANSIT", occurredAt: "2026-11-06T14:00:00Z" }),
      );

      expect(await screen.findByText("On the way to Kandy")).toBeInTheDocument();
      expect(getParcel).toHaveBeenCalledTimes(2);
    });

    it("reloads on connect only when the snapshot shows a newer status", async () => {
      getParcel.mockResolvedValue(parcel);
      render(<ParcelDetails parcelId="p-1" token="token-1" onBack={vi.fn()} />);
      await screen.findByRole("heading", { name: "SD-7K3Q-9P2M" });
      const snapshot = { trackingNo: "SD-7K3Q-9P2M", size: "MEDIUM" as const, weightKg: 2.5,
        createdAt: parcel.createdAt, history: [] };

      act(() => stream?.onSnapshot({ ...snapshot, status: "IN_TRANSIT" }));
      expect(getParcel).toHaveBeenCalledTimes(1);

      act(() => stream?.onSnapshot({ ...snapshot, status: "OUT_FOR_DELIVERY" }));
      expect(getParcel).toHaveBeenCalledTimes(2);
    });

    it("does not open the stream for a delivered parcel", async () => {
      getParcel.mockResolvedValue({ ...parcel, status: "DELIVERED" });
      render(<ParcelDetails parcelId="p-1" token="token-1" onBack={vi.fn()} />);
      await screen.findByRole("heading", { name: "SD-7K3Q-9P2M" });

      expect(subscribeTrackingStream).not.toHaveBeenCalled();
      expect(screen.queryByText(/live updates/i)).not.toBeInTheDocument();
    });

    it("closes the stream when the page is left", async () => {
      getParcel.mockResolvedValue(parcel);
      const { unmount } = render(<ParcelDetails parcelId="p-1" token="token-1" onBack={vi.fn()} />);
      await screen.findByRole("heading", { name: "SD-7K3Q-9P2M" });

      unmount();

      expect(closeStream).toHaveBeenCalledTimes(1);
    });
  });
});
