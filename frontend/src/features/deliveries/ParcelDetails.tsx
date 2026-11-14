import { useEffect, useState } from "react";
import { ParcelApiError, ParcelResponse, parcelApi } from "../../api/parcelApi";
import { Button } from "../../components/Button";
import { StatusBadge } from "../../components/StatusBadge";
import { SIZE_LABELS, STATUS_LABELS, formatDateTime, formatMoney } from "../../lib/parcelFormat";
import { trackingPath } from "../tracking/trackingNumber";
import "./ParcelDetails.css";

/** Who made a status change, from the role stored in the history. */
const ROLE_LABELS: Record<string, string> = {
  CUSTOMER: "You",
  MERCHANT: "Merchant",
  COURIER: "Courier",
  DISPATCHER: "Dispatcher",
  ADMIN: "Admin",
};

function errorMessage(error: unknown): string {
  if (error instanceof ParcelApiError && error.status === 404) {
    return "This delivery could not be found.";
  }
  if (error instanceof ParcelApiError && error.status === 401) {
    return "Your session has expired. Please sign out and sign in again.";
  }
  return "Could not load the delivery. Please try again.";
}

type ParcelDetailsProps = {
  parcelId: string;
  token: string;
  onBack: () => void;
};

/** One delivery with its full history (diagram seq-parcel-history-receipt, part 2). */
export function ParcelDetails({ parcelId, token, onBack }: ParcelDetailsProps) {
  const [parcel, setParcel] = useState<ParcelResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    setParcel(null);
    setError(null);
    parcelApi
      .getParcel(parcelId, token)
      .then((loaded) => {
        if (current) {
          setParcel(loaded);
        }
      })
      .catch((loadError: unknown) => {
        if (current) {
          setError(errorMessage(loadError));
        }
      });
    return () => {
      current = false;
    };
  }, [parcelId, token]);

  return (
    <div className="parcel-details">
      <Button variant="secondary" className="parcel-details-back" onClick={onBack}>
        ← Back to my deliveries
      </Button>

      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      {!parcel && !error && <p className="deliveries-note">Loading the delivery…</p>}

      {parcel && (
        <>
          <div className="parcel-details-top">
            <div>
              <p className="parcel-details-label">Tracking number</p>
              <h3 className="parcel-details-number">{parcel.trackingNo}</h3>
            </div>
            <StatusBadge status={parcel.status} />
          </div>

          {parcel.status === "FAILED" && parcel.failureReason && (
            <p className="parcel-details-failed">
              <strong>Why the delivery failed:</strong> {parcel.failureReason}
            </p>
          )}

          <dl className="parcel-details-facts">
            <div>
              <dt>From</dt>
              <dd>{parcel.pickupAddress}</dd>
            </div>
            <div>
              <dt>To</dt>
              <dd>{parcel.dropoffAddress}</dd>
            </div>
            <div>
              <dt>Recipient</dt>
              <dd>
                {parcel.recipientName}, {parcel.recipientPhone}
              </dd>
            </div>
            <div>
              <dt>Parcel</dt>
              <dd>
                {SIZE_LABELS[parcel.size]}, {parcel.weightKg} kg
              </dd>
            </div>
            <div>
              <dt>Fee</dt>
              <dd>{formatMoney(parcel.feeAmount, parcel.feeCurrency)}</dd>
            </div>
            <div>
              <dt>Booked</dt>
              <dd>{formatDateTime(parcel.createdAt)}</dd>
            </div>
          </dl>

          <a className="parcel-details-track" href={trackingPath(parcel.trackingNo)}>
            Track live
          </a>

          <h4 className="parcel-details-history-title">History</h4>
          <ol className="parcel-details-history">
            {[...parcel.history].reverse().map((entry) => (
              <li key={`${entry.status}-${entry.createdAt}`}>
                <div className="parcel-details-history-top">
                  <strong>{STATUS_LABELS[entry.status]}</strong>
                  <time dateTime={entry.createdAt}>{formatDateTime(entry.createdAt)}</time>
                </div>
                {entry.changedByRole && (
                  <p className="parcel-details-by">By: {ROLE_LABELS[entry.changedByRole] ?? entry.changedByRole}</p>
                )}
                {entry.note && <p className="parcel-details-note">{entry.note}</p>}
                {entry.proofReference && <p className="parcel-details-by">Proof of delivery recorded</p>}
              </li>
            ))}
          </ol>
        </>
      )}
    </div>
  );
}
