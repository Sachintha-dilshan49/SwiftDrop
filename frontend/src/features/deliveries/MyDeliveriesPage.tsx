import { useEffect, useState } from "react";
import { PageResponse, ParcelApiError, ParcelStatus, ParcelSummary, parcelApi } from "../../api/parcelApi";
import { STATUS_LABELS, formatDateTime, formatMoney } from "../../lib/parcelFormat";
import { trackingPath } from "../tracking/trackingNumber";
import "./MyDeliveriesPage.css";

export const PAGE_SIZE = 10;

const STATUS_FILTERS: ParcelStatus[] = ["CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED"];

const SESSION_EXPIRED = "Your session has expired. Please sign out and sign in again.";

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ParcelApiError && error.status === 401 ? SESSION_EXPIRED : fallback;
}

/** Starts a browser download of a file the page already has. */
function saveFile(file: Blob, fileName: string) {
  const url = URL.createObjectURL(file);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

type MyDeliveriesPageProps = {
  /** The signed-in customer's access token. */
  token: string;
  /** Changing it loads the list again, for example after a new booking. */
  refreshKey?: number;
};

/** The customer's delivery history (diagram seq-parcel-history-receipt, parts 1 and 3). */
export function MyDeliveriesPage({ token, refreshKey = 0 }: MyDeliveriesPageProps) {
  const [status, setStatus] = useState<ParcelStatus | "">("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PageResponse<ParcelSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    parcelApi
      .getMyParcels(token, { page, size: PAGE_SIZE, status: status || undefined })
      .then((parcels) => {
        if (current) {
          setResult(parcels);
        }
      })
      .catch((loadError: unknown) => {
        if (current) {
          setResult(null);
          setError(errorMessage(loadError, "Could not load your deliveries. Please try again."));
        }
      })
      .finally(() => {
        if (current) {
          setLoading(false);
        }
      });
    return () => {
      current = false;
    };
  }, [token, page, status, refreshKey]);

  async function downloadReceipt(parcel: ParcelSummary) {
    setDownloadingId(parcel.id);
    setError(null);
    try {
      saveFile(await parcelApi.getReceipt(parcel.id, token), `receipt-${parcel.trackingNo}.pdf`);
    } catch (receiptError) {
      setError(errorMessage(receiptError, "Could not download the receipt. Please try again."));
    } finally {
      setDownloadingId(null);
    }
  }

  const parcels = result?.content ?? [];
  const totalPages = result ? Math.max(1, Math.ceil(result.totalElements / PAGE_SIZE)) : 1;

  return (
    <section className="deliveries" aria-labelledby="deliveries-title">
      <div className="deliveries-heading">
        <h2 id="deliveries-title">My deliveries</h2>
        <label className="deliveries-filter">
          Status
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as ParcelStatus | "");
              setPage(0);
            }}
          >
            <option value="">All</option>
            {STATUS_FILTERS.map((option) => (
              <option key={option} value={option}>
                {STATUS_LABELS[option]}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}

      {loading && <p className="deliveries-note">Loading your deliveries…</p>}

      {!loading && result && parcels.length === 0 && (
        <p className="deliveries-note">
          {status ? "No deliveries with this status." : "You have no deliveries yet."}
        </p>
      )}

      {!loading && parcels.length > 0 && (
        <ul className="deliveries-list">
          {parcels.map((parcel) => (
            <li key={parcel.id} className="deliveries-item">
              <div className="deliveries-item-top">
                <a className="deliveries-tracking" href={trackingPath(parcel.trackingNo)}>
                  {parcel.trackingNo}
                </a>
                <span className={`deliveries-status deliveries-status-${parcel.status.toLowerCase()}`}>
                  {STATUS_LABELS[parcel.status]}
                </span>
              </div>
              <p className="deliveries-route">
                {parcel.pickupAddress} → {parcel.dropoffAddress}
              </p>
              <p className="deliveries-meta">
                To {parcel.recipientName} · {formatMoney(parcel.feeAmount, parcel.feeCurrency)} · Booked{" "}
                {formatDateTime(parcel.createdAt)}
              </p>
              <button
                type="button"
                className="deliveries-receipt"
                onClick={() => void downloadReceipt(parcel)}
                disabled={downloadingId === parcel.id}
                aria-label={`Download the receipt for ${parcel.trackingNo}`}
              >
                {downloadingId === parcel.id ? "Downloading…" : "Receipt (PDF)"}
              </button>
            </li>
          ))}
        </ul>
      )}

      {result && result.totalElements > PAGE_SIZE && (
        <nav className="deliveries-pages" aria-label="Pages">
          <button type="button" onClick={() => setPage((current) => current - 1)} disabled={loading || page === 0}>
            Previous
          </button>
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            onClick={() => setPage((current) => current + 1)}
            disabled={loading || page + 1 >= totalPages}
          >
            Next
          </button>
        </nav>
      )}
    </section>
  );
}
