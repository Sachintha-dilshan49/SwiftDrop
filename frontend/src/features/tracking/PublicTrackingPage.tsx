import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  ParcelApiError,
  ParcelStatus,
  PublicTrackingEvent,
  PublicTrackingResponse,
  TERMINAL_STATUSES,
  parcelApi,
} from "../../api/parcelApi";
import { useLiveTracking } from "../../hooks/useLiveTracking";
import { SIZE_LABELS, STATUS_LABELS, formatDateTime } from "../../lib/parcelFormat";
import { isValidTrackingNo, normalizeTrackingNo, trackingPath } from "./trackingNumber";
import "./PublicTrackingPage.css";

const PROGRESS: ParcelStatus[] = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"];

/** Adds a live status change to the page state; ignores other parcels' events and ones already shown. */
export function applyStatusChange(current: PublicTrackingResponse, event: PublicTrackingEvent): PublicTrackingResponse {
  if (event.trackingNo !== current.trackingNo) {
    return current;
  }
  const alreadyShown = current.history.some(
    (entry) => entry.status === event.status && entry.occurredAt === event.occurredAt,
  );
  if (alreadyShown) {
    return current;
  }
  return {
    ...current,
    status: event.status,
    history: [...current.history, { status: event.status, occurredAt: event.occurredAt }],
  };
}

function errorMessage(error: unknown): string {
  if (error instanceof ParcelApiError && error.status === 404) {
    return "We could not find a parcel with that tracking number.";
  }
  if (error instanceof ParcelApiError && error.status === 400) {
    return "That is not a valid tracking number. It looks like SD-7K3Q-9P2M.";
  }
  return "Could not load the tracking information. Please try again.";
}

type PublicTrackingPageProps = {
  /** From the address /track/{number}; empty shows only the search form. */
  initialTrackingNo?: string;
};

/** Public, no-login tracking page with live updates (diagrams seq-parcel-history-receipt part 4, seq-live-tracking). */
export function PublicTrackingPage({ initialTrackingNo = "" }: PublicTrackingPageProps) {
  const [query, setQuery] = useState(initialTrackingNo);
  const [tracking, setTracking] = useState<PublicTrackingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async (input: string) => {
    const trackingNo = normalizeTrackingNo(input);
    setError(null);
    if (!isValidTrackingNo(trackingNo)) {
      setTracking(null);
      setError("Enter a tracking number like SD-7K3Q-9P2M.");
      return;
    }
    setLoading(true);
    try {
      const result = await parcelApi.getPublicTracking(trackingNo);
      setTracking(result);
      setQuery(result.trackingNo);
      if (window.location.pathname !== trackingPath(result.trackingNo)) {
        window.history.replaceState(null, "", trackingPath(result.trackingNo));
      }
    } catch (loadError) {
      setTracking(null);
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (initialTrackingNo) {
      void load(initialTrackingNo);
    }
  }, [initialTrackingNo, load]);

  const activeTrackingNo = tracking?.trackingNo ?? null;
  const finished = tracking !== null && TERMINAL_STATUSES.includes(tracking.status);

  // Live updates until the parcel is delivered or failed; every reconnect starts with a fresh snapshot.
  const live = useLiveTracking(activeTrackingNo && !finished ? activeTrackingNo : null, {
    onSnapshot: setTracking,
    onStatusChange: (event) => setTracking((current) => (current ? applyStatusChange(current, event) : current)),
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void load(query);
  }

  const stepIndex = tracking ? PROGRESS.indexOf(tracking.status) : -1;

  return (
    <div className="tracking-page">
      <header className="tracking-header">
        <a className="dashboard-brand" href="/">
          <span className="dashboard-brand-mark" aria-hidden="true">S</span>
          SwiftDrop
        </a>
      </header>

      <main className="tracking-main">
        <h1 className="tracking-title">Track your parcel</h1>

        <form className="tracking-search" onSubmit={handleSubmit} noValidate>
          <label htmlFor="tracking-no">Tracking number</label>
          <div className="tracking-search-row">
            <input
              id="tracking-no"
              value={query}
              onChange={(event) => setQuery(event.target.value.toUpperCase())}
              placeholder="SD-7K3Q-9P2M"
              autoComplete="off"
              spellCheck={false}
            />
            <button type="submit" className="tracking-button" disabled={loading}>
              {loading ? "Searching…" : "Track"}
            </button>
          </div>
        </form>

        {error && (
          <p className="alert" role="alert">
            {error}
          </p>
        )}

        {tracking && (
          <section className="tracking-card" aria-labelledby="tracking-number">
            <div className="tracking-card-top">
              <div>
                <p className="tracking-label">Tracking number</p>
                <h2 id="tracking-number" className="tracking-number">
                  {tracking.trackingNo}
                </h2>
              </div>
              <span className={`tracking-status tracking-status-${tracking.status.toLowerCase()}`} role="status">
                {STATUS_LABELS[tracking.status]}
              </span>
            </div>

            {!finished && (
              <p className={live ? "tracking-live tracking-live-on" : "tracking-live"}>
                {live ? "Live: updates appear automatically" : "Connecting to live updates…"}
              </p>
            )}

            {tracking.status === "FAILED" ? (
              <p className="tracking-failed">The delivery attempt was not successful.</p>
            ) : (
              <ol className="tracking-steps" aria-label="Delivery progress">
                {PROGRESS.map((step, index) => (
                  <li
                    key={step}
                    className={index <= stepIndex ? "tracking-step tracking-step-done" : "tracking-step"}
                    aria-current={index === stepIndex ? "step" : undefined}
                  >
                    {STATUS_LABELS[step]}
                  </li>
                ))}
              </ol>
            )}

            <dl className="tracking-facts">
              <div>
                <dt>Size</dt>
                <dd>{SIZE_LABELS[tracking.size]}</dd>
              </div>
              <div>
                <dt>Weight</dt>
                <dd>{tracking.weightKg} kg</dd>
              </div>
              <div>
                <dt>Booked</dt>
                <dd>{formatDateTime(tracking.createdAt)}</dd>
              </div>
            </dl>

            <h3 className="tracking-history-title">History</h3>
            <ol className="tracking-history">
              {[...tracking.history].reverse().map((entry) => (
                <li key={`${entry.status}-${entry.occurredAt}`}>
                  <strong>{STATUS_LABELS[entry.status]}</strong>
                  <time dateTime={entry.occurredAt}>{formatDateTime(entry.occurredAt)}</time>
                </li>
              ))}
            </ol>
          </section>
        )}
      </main>
    </div>
  );
}
