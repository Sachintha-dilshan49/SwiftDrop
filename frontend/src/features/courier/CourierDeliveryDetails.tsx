import { FormEvent, useState } from "react";
import { CourierDelivery, DispatchApiError, dispatchApi } from "../../api/dispatchApi";
import type { ParcelStatus } from "../../api/parcelApi";
import { SIZE_LABELS } from "../../lib/parcelFormat";
import { Stops } from "./Stops";
import { ACTION_LABELS, COURIER_STATUS_LABELS, ROUTE, currentStatus, nextSteps, photoProblem } from "./courierFlow";

const SESSION_EXPIRED = "Your session has expired. Please sign out and sign in again.";

type CourierDeliveryDetailsProps = {
  delivery: CourierDelivery;
  token: string;
  onBack: () => void;
  onUpdated: (delivery: CourierDelivery) => void;
};

/** One delivery: trip progress and the next status step, after the Figma live tracking page. */
export function CourierDeliveryDetails({ delivery, token, onBack, onUpdated }: CourierDeliveryDetailsProps) {
  const [chosen, setChosen] = useState<ParcelStatus | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const status = currentStatus(delivery);
  const steps = nextSteps(delivery);

  return (
    <section className="courier" aria-labelledby="delivery-title">
      <button type="button" className="courier-back" onClick={onBack}>← Today&apos;s deliveries</button>
      <h2 id="delivery-title" className="courier-title">{delivery.trackingNo ?? "Delivery"}</h2>
      {delivery.size && (
        <p className="courier-subtitle">
          {SIZE_LABELS[delivery.size] ?? delivery.size}
          {delivery.weightKg != null && ` · ${delivery.weightKg} kg`}
        </p>
      )}

      {notice && <p className="courier-success" role="status">{notice}</p>}

      <div className="courier-panel">
        {status && (
          <span className={`courier-pill courier-pill-${status.toLowerCase()}`}>{COURIER_STATUS_LABELS[status]}</span>
        )}
        <Stops pickup={delivery.pickupAddress} dropoff={delivery.dropoffAddress} />
        {delivery.recipientName && <p className="courier-card-meta">Recipient: {delivery.recipientName}</p>}
      </div>

      <div className="courier-panel">
        <h3 className="courier-panel-title">Trip progress</h3>
        <TripProgress status={status} />
      </div>

      {steps.length > 0 && !chosen && (
        <div className="courier-actions">
          {steps.map((step) => (
            <button
              key={step}
              type="button"
              className={step === "FAILED" ? "courier-button-outline" : "courier-button"}
              onClick={() => {
                setNotice(null);
                setChosen(step);
              }}
            >
              {ACTION_LABELS[step]}
            </button>
          ))}
        </div>
      )}

      {chosen && (
        <StatusForm
          delivery={delivery}
          status={chosen}
          token={token}
          onCancel={() => setChosen(null)}
          onSaved={(saved) => {
            setChosen(null);
            setNotice(`Marked as ${COURIER_STATUS_LABELS[chosen].toLowerCase()}.`);
            onUpdated(saved);
          }}
        />
      )}
    </section>
  );
}

function TripProgress({ status }: { status: ParcelStatus | null }) {
  const reached = status === "FAILED" ? ROUTE.indexOf("OUT_FOR_DELIVERY") : status ? ROUTE.indexOf(status) : -1;
  const route: ParcelStatus[] = status === "FAILED" ? [...ROUTE.slice(0, -1), "FAILED"] : ROUTE;
  return (
    <ol className="courier-progress">
      {route.map((step, index) => {
        const state = step === "FAILED" ? "failed" : index < reached ? "done" : index === reached ? "current" : "todo";
        return (
          <li key={step} className={`courier-step courier-step-${state}`}>
            <span className="courier-step-mark" aria-hidden="true" />
            {COURIER_STATUS_LABELS[step]}
            {state === "current" && <span className="courier-visually-hidden"> (now)</span>}
          </li>
        );
      })}
    </ol>
  );
}

type StatusFormProps = {
  delivery: CourierDelivery;
  status: ParcelStatus;
  token: string;
  onCancel: () => void;
  onSaved: (delivery: CourierDelivery) => void;
};

/** Confirms one step. FAILED needs a reason; DELIVERED asks who received it; a proof photo is optional. */
function StatusForm({ delivery, status, token, onCancel, onSaved }: StatusFormProps) {
  const [note, setNote] = useState("");
  const [recipientName, setRecipientName] = useState(delivery.recipientName ?? "");
  const [failureReason, setFailureReason] = useState("");
  const [photo, setPhoto] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const failed = status === "FAILED";
  const finalStep = failed || status === "DELIVERED";

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (failed && !failureReason.trim()) {
      setError("Please say why the delivery failed.");
      return;
    }
    const problem = photo ? photoProblem(photo) : null;
    if (problem) {
      setError(problem);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      onSaved(await dispatchApi.updateDeliveryStatus(delivery.assignmentId, {
        status,
        note,
        recipientName: status === "DELIVERED" ? recipientName : undefined,
        failureReason: failed ? failureReason : undefined,
        photo: photo ?? undefined,
      }, token));
    } catch (saveError) {
      setError(saveError instanceof DispatchApiError
        ? (saveError.status === 401 ? SESSION_EXPIRED : saveError.message)
        : "Could not update the delivery. Please try again.");
      setSaving(false);
    }
  }

  return (
    <form className="courier-panel courier-form" onSubmit={(event) => void submit(event)} noValidate>
      <h3 className="courier-panel-title">{ACTION_LABELS[status]}</h3>

      {failed && (
        <label className="courier-field">
          Reason (required)
          <textarea
            value={failureReason}
            onChange={(event) => setFailureReason(event.target.value)}
            maxLength={500}
            rows={3}
            placeholder="For example: nobody at home, gate locked"
            required
          />
        </label>
      )}

      {status === "DELIVERED" && (
        <label className="courier-field">
          Received by
          <input value={recipientName} onChange={(event) => setRecipientName(event.target.value)} maxLength={150} />
        </label>
      )}

      <label className="courier-field">
        Note (optional)
        <textarea value={note} onChange={(event) => setNote(event.target.value)} maxLength={500} rows={2} />
      </label>

      {finalStep && (
        <label className="courier-field">
          Proof photo (optional)
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            capture="environment"
            onChange={(event) => setPhoto(event.target.files?.[0] ?? null)}
          />
          <small>JPEG, PNG or WebP, up to 5 MB.</small>
        </label>
      )}

      {error && <p className="alert" role="alert">{error}</p>}

      <div className="courier-form-actions">
        <button type="button" className="courier-button-outline" onClick={onCancel} disabled={saving}>Cancel</button>
        <button type="submit" className={failed ? "courier-button courier-button-danger" : "courier-button"}
          disabled={saving}>
          {saving ? "Saving…" : `Confirm: ${ACTION_LABELS[status].toLowerCase()}`}
        </button>
      </div>
    </form>
  );
}
