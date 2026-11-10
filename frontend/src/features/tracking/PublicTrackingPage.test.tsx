import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ParcelApiError,
  PublicTrackingResponse,
  TrackingStreamHandlers,
  parcelApi,
} from "../../api/parcelApi";
import { PublicTrackingPage, applyStatusChange } from "./PublicTrackingPage";

vi.mock("../../api/parcelApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/parcelApi")>();
  return {
    ...actual,
    parcelApi: { getPublicTracking: vi.fn(), subscribeTrackingStream: vi.fn() },
  };
});

const getPublicTracking = vi.mocked(parcelApi.getPublicTracking);
const subscribeTrackingStream = vi.mocked(parcelApi.subscribeTrackingStream);

const inTransit: PublicTrackingResponse = {
  trackingNo: "SD-7K3Q-9P2M",
  status: "IN_TRANSIT",
  size: "MEDIUM",
  weightKg: 2.5,
  createdAt: "2026-11-06T09:00:00Z",
  history: [
    { status: "CREATED", occurredAt: "2026-11-06T09:00:00Z" },
    { status: "PICKED_UP", occurredAt: "2026-11-06T10:30:00Z" },
    { status: "IN_TRANSIT", occurredAt: "2026-11-06T12:00:00Z" },
  ],
};

let streamHandlers: TrackingStreamHandlers | undefined;
const closeStream = vi.fn();

/** The page opens the stream in an effect that runs after the data is shown, so wait for it. */
async function openedStream(): Promise<TrackingStreamHandlers> {
  await waitFor(() => expect(streamHandlers).toBeDefined());
  return streamHandlers as TrackingStreamHandlers;
}

describe("PublicTrackingPage", () => {
  beforeEach(() => {
    getPublicTracking.mockReset();
    subscribeTrackingStream.mockReset();
    closeStream.mockReset();
    streamHandlers = undefined;
    subscribeTrackingStream.mockImplementation((_trackingNo, handlers) => {
      streamHandlers = handlers;
      return closeStream;
    });
    window.history.replaceState(null, "", "/track");
  });

  it("loads the tracking number from the link and shows its status, details and history", async () => {
    getPublicTracking.mockResolvedValue(inTransit);

    render(<PublicTrackingPage initialTrackingNo="SD-7K3Q-9P2M" />);

    expect(await screen.findByRole("status")).toHaveTextContent("In transit");
    expect(screen.getByRole("heading", { name: "SD-7K3Q-9P2M" })).toBeInTheDocument();
    expect(screen.getByText("Medium")).toBeInTheDocument();
    expect(screen.getByText("2.5 kg")).toBeInTheDocument();
    expect(screen.getByRole("list", { name: "Delivery progress" })).toBeInTheDocument();
    expect(getPublicTracking).toHaveBeenCalledWith("SD-7K3Q-9P2M");
    await openedStream();
    expect(subscribeTrackingStream).toHaveBeenCalledWith("SD-7K3Q-9P2M", expect.any(Object));
  });

  it("searches a typed number, upper-cased, and puts it in the address so it can be shared", async () => {
    getPublicTracking.mockResolvedValue(inTransit);
    render(<PublicTrackingPage />);

    fireEvent.change(screen.getByLabelText("Tracking number"), { target: { value: " sd-7k3q-9p2m " } });
    fireEvent.click(screen.getByRole("button", { name: "Track" }));

    await screen.findByRole("status");
    expect(getPublicTracking).toHaveBeenCalledWith("SD-7K3Q-9P2M");
    expect(window.location.pathname).toBe("/track/SD-7K3Q-9P2M");
  });

  it("rejects a malformed number without calling the server", async () => {
    render(<PublicTrackingPage />);

    fireEvent.change(screen.getByLabelText("Tracking number"), { target: { value: "12345" } });
    fireEvent.click(screen.getByRole("button", { name: "Track" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Enter a tracking number like SD-7K3Q-9P2M.");
    expect(getPublicTracking).not.toHaveBeenCalled();
  });

  it("says when no parcel has that tracking number", async () => {
    getPublicTracking.mockRejectedValue(new ParcelApiError(404, "Tracking number not found: SD-2222-2222"));

    render(<PublicTrackingPage initialTrackingNo="SD-2222-2222" />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "We could not find a parcel with that tracking number.");
    expect(subscribeTrackingStream).not.toHaveBeenCalled();
  });

  it("shows live status changes from the stream and marks the connection as live", async () => {
    getPublicTracking.mockResolvedValue(inTransit);
    render(<PublicTrackingPage initialTrackingNo="SD-7K3Q-9P2M" />);
    await screen.findByRole("status");
    const stream = await openedStream();

    act(() => {
      stream.onLiveChange?.(true);
      stream.onStatusChange({ trackingNo: "SD-7K3Q-9P2M", status: "OUT_FOR_DELIVERY",
        previousStatus: "IN_TRANSIT", occurredAt: "2026-11-06T14:00:00Z" });
    });

    expect(screen.getByRole("status")).toHaveTextContent("Out for delivery");
    expect(screen.getByText("Live: updates appear automatically")).toBeInTheDocument();
  });

  it("stops live updates once the parcel is delivered", async () => {
    getPublicTracking.mockResolvedValue(inTransit);
    render(<PublicTrackingPage initialTrackingNo="SD-7K3Q-9P2M" />);
    await screen.findByRole("status");
    const stream = await openedStream();

    act(() => {
      stream.onStatusChange({ trackingNo: "SD-7K3Q-9P2M", status: "DELIVERED",
        previousStatus: "OUT_FOR_DELIVERY", occurredAt: "2026-11-06T15:00:00Z" });
    });

    expect(screen.getByRole("status")).toHaveTextContent("Delivered");
    await waitFor(() => expect(closeStream).toHaveBeenCalled());
    expect(screen.queryByText(/live updates|updates appear/i)).not.toBeInTheDocument();
  });

  it("does not open a live stream for a parcel that has already failed", async () => {
    getPublicTracking.mockResolvedValue({
      ...inTransit,
      status: "FAILED",
      history: [...inTransit.history, { status: "FAILED", occurredAt: "2026-11-06T16:00:00Z" }],
    });

    render(<PublicTrackingPage initialTrackingNo="SD-7K3Q-9P2M" />);

    expect(await screen.findByRole("status")).toHaveTextContent("Delivery failed");
    expect(screen.getByText("The delivery attempt was not successful.")).toBeInTheDocument();
    expect(subscribeTrackingStream).not.toHaveBeenCalled();
  });
});

describe("applyStatusChange", () => {
  const change = { trackingNo: "SD-7K3Q-9P2M", status: "OUT_FOR_DELIVERY" as const,
    previousStatus: "IN_TRANSIT" as const, occurredAt: "2026-11-06T14:00:00Z" };

  it("updates the status and adds the change to the history", () => {
    const updated = applyStatusChange(inTransit, change);

    expect(updated.status).toBe("OUT_FOR_DELIVERY");
    expect(updated.history[updated.history.length - 1])
      .toEqual({ status: "OUT_FOR_DELIVERY", occurredAt: "2026-11-06T14:00:00Z" });
  });

  it("ignores a change that is already shown, for example after a reconnect", () => {
    const once = applyStatusChange(inTransit, change);

    expect(applyStatusChange(once, change)).toBe(once);
  });

  it("ignores a change for another parcel", () => {
    expect(applyStatusChange(inTransit, { ...change, trackingNo: "SD-2222-2222" })).toBe(inTransit);
  });
});
