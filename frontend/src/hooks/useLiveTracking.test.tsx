import { act, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TrackingStreamHandlers, parcelApi } from "../api/parcelApi";
import { LiveTrackingHandlers, useLiveTracking } from "./useLiveTracking";

vi.mock("../api/parcelApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../api/parcelApi")>();
  return { ...actual, parcelApi: { subscribeTrackingStream: vi.fn() } };
});

const subscribeTrackingStream = vi.mocked(parcelApi.subscribeTrackingStream);
const closeStream = vi.fn();
let stream: TrackingStreamHandlers | null = null;

function Probe({ trackingNo, handlers = {} }: { trackingNo: string | null; handlers?: LiveTrackingHandlers }) {
  const live = useLiveTracking(trackingNo, handlers);
  return <p>{live ? "live" : "offline"}</p>;
}

describe("useLiveTracking", () => {
  beforeEach(() => {
    closeStream.mockReset();
    stream = null;
    subscribeTrackingStream.mockReset();
    subscribeTrackingStream.mockImplementation((_trackingNo, handlers) => {
      stream = handlers;
      return closeStream;
    });
  });

  it("stays closed without a tracking number", () => {
    render(<Probe trackingNo={null} />);

    expect(subscribeTrackingStream).not.toHaveBeenCalled();
    expect(screen.getByText("offline")).toBeInTheDocument();
  });

  it("reports the connection state and passes events to the latest handlers", () => {
    const first = vi.fn();
    const latest = vi.fn();
    const { rerender } = render(<Probe trackingNo="SD-7K3Q-9P2M" handlers={{ onStatusChange: first }} />);
    rerender(<Probe trackingNo="SD-7K3Q-9P2M" handlers={{ onStatusChange: latest }} />);

    act(() => stream?.onLiveChange?.(true));
    expect(screen.getByText("live")).toBeInTheDocument();

    const event = { trackingNo: "SD-7K3Q-9P2M", status: "DELIVERED" as const, previousStatus: null, occurredAt: "t" };
    act(() => stream?.onStatusChange(event));
    expect(latest).toHaveBeenCalledWith(event);
    expect(first).not.toHaveBeenCalled();
    // A new handler object must not reopen the stream.
    expect(subscribeTrackingStream).toHaveBeenCalledTimes(1);
  });

  it("closes the stream when the tracking number goes away", () => {
    const { rerender } = render(<Probe trackingNo="SD-7K3Q-9P2M" />);
    act(() => stream?.onLiveChange?.(true));

    rerender(<Probe trackingNo={null} />);

    expect(closeStream).toHaveBeenCalledTimes(1);
    expect(screen.getByText("offline")).toBeInTheDocument();
  });
});
