import { useEffect, useRef, useState } from "react";
import { PublicTrackingEvent, PublicTrackingResponse, parcelApi } from "../api/parcelApi";

export type LiveTrackingHandlers = {
  /** Sent on every (re)connect; missed events are not replayed, so treat it as the current state. */
  onSnapshot?: (snapshot: PublicTrackingResponse) => void;
  onStatusChange?: (event: PublicTrackingEvent) => void;
};

/**
 * Keeps the live tracking stream (Server-Sent Events) open for one parcel and returns whether it is
 * connected. Pass null to stay closed, e.g. before the parcel is loaded or once it is delivered or failed.
 * The browser reconnects by itself; the stream closes after DELIVERED or FAILED (parcelApi).
 */
export function useLiveTracking(trackingNo: string | null, handlers: LiveTrackingHandlers): boolean {
  const [live, setLive] = useState(false);
  // The latest handlers, so a new render does not reopen the stream.
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  useEffect(() => {
    if (!trackingNo) {
      return undefined;
    }
    const close = parcelApi.subscribeTrackingStream(trackingNo, {
      onSnapshot: (snapshot) => handlersRef.current.onSnapshot?.(snapshot),
      onStatusChange: (event) => handlersRef.current.onStatusChange?.(event),
      onLiveChange: setLive,
    });
    return () => {
      close();
      setLive(false);
    };
  }, [trackingNo]);

  return live;
}
