import { useEffect, useState } from "react";
import type { DeliverySpeed } from "../../api/parcelApi";
import { PricingApiError, PricingEstimate, RouteType, pricingApi } from "../../api/pricingApi";
import { Button } from "../../components/Button";
import { SIZE_LABELS, formatMoney } from "../../lib/parcelFormat";
import { SPEED_OPTIONS } from "./bookingOptions";
import { BookingFormValues, toCreateRequest } from "./bookingValidation";
import "./FareStep.css";

const ROUTE_LABELS: Record<RouteType, string> = {
  SAME_ZONE: "same zone",
  NEIGHBOURING: "neighbouring zone",
  COLOMBO_SUBURBS: "Colombo suburbs",
  INTER_CITY: "inter-city",
};

type Estimates = Partial<Record<DeliverySpeed, PricingEstimate>>;

function loadErrorMessage(error: unknown): string {
  // A 400 explains what cannot be priced, e.g. a parcel over the weight limit or an unknown promo code.
  if (error instanceof PricingApiError && error.status === 400) {
    return error.message;
  }
  return "We could not get a price right now. Please try again in a moment.";
}

type FareStepProps = {
  values: BookingFormValues;
  submitting: boolean;
  /** A booking failure that has no field to point at, e.g. pricing was down when booking. */
  error: string | null;
  onSpeedChange: (speed: DeliverySpeed) => void;
  onBack: () => void;
  onConfirm: () => void;
};

/** Second booking step (Figma "Fare & confirmation"): a price per speed, the fare breakdown and Confirm & book. */
export function FareStep({ values, submitting, error, onSpeedChange, onBack, onConfirm }: FareStepProps) {
  const [estimates, setEstimates] = useState<Estimates>({});
  const [loadError, setLoadError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const loading = Object.keys(estimates).length === 0 && loadError === null;

  // One estimate per speed, so each speed card shows its own price.
  const request = toCreateRequest(values);
  const { pickupZone, dropoffZone, weightKg, size, promoCode } = request;
  useEffect(() => {
    let current = true;
    setEstimates({});
    setLoadError(null);
    Promise.allSettled(
      SPEED_OPTIONS.map((option) =>
        pricingApi.getEstimate({ pickupZone, dropoffZone, weightKg, size, promoCode, speed: option.speed }),
      ),
    ).then((results) => {
      if (!current) return;
      const found: Estimates = {};
      results.forEach((result, index) => {
        if (result.status === "fulfilled") found[SPEED_OPTIONS[index].speed] = result.value;
      });
      const firstFailure = results.find((result) => result.status === "rejected");
      if (Object.keys(found).length === 0 && firstFailure) {
        setLoadError(loadErrorMessage(firstFailure.reason));
      } else {
        setEstimates(found);
      }
    });
    return () => {
      current = false;
    };
  }, [pickupZone, dropoffZone, weightKg, size, promoCode, attempt]);

  const selected = estimates[values.speed];
  const sizeName = SIZE_LABELS[request.size];

  return (
    <div className="fare">
      <button type="button" className="fare-back" onClick={onBack}>
        ← Edit details
      </button>
      <p className="fare-summary">
        {request.pickupAddress} → {request.dropoffAddress} · {sizeName}, {request.weightKg} kg
      </p>

      {loading && <p className="fare-note">Getting prices…</p>}
      {loadError && (
        <div className="fare-problem">
          <p className="alert" role="alert">
            {loadError}
          </p>
          <Button variant="secondary" onClick={() => setAttempt((count) => count + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !loadError && (
        <>
          <fieldset className="fare-card">
            <legend>Delivery speed</legend>
            {SPEED_OPTIONS.map((option) => {
              const estimate = estimates[option.speed];
              return (
                <label
                  key={option.speed}
                  className={`fare-speed${values.speed === option.speed ? " is-selected" : ""}`}
                >
                  <input
                    type="radio"
                    name="fare-speed"
                    value={option.speed}
                    checked={values.speed === option.speed}
                    onChange={() => onSpeedChange(option.speed)}
                    disabled={!estimate}
                  />
                  <span className="fare-speed-text">
                    <strong>{option.name}</strong>
                    <span>{option.promise}</span>
                  </span>
                  <strong className="fare-speed-price">
                    {estimate ? formatMoney(estimate.total, estimate.currency) : "Not available"}
                  </strong>
                </label>
              );
            })}
          </fieldset>

          {selected && (
            <section className="fare-card" aria-labelledby="fare-breakdown-title">
              <h3 id="fare-breakdown-title">Fare breakdown</h3>
              <dl className="fare-lines">
                <div>
                  <dt>Base fee · {ROUTE_LABELS[selected.routeType]}</dt>
                  <dd>{formatMoney(selected.baseFee, selected.currency)}</dd>
                </div>
                <div>
                  <dt>Weight · {request.weightKg} kg</dt>
                  <dd>{formatMoney(selected.weightFee, selected.currency)}</dd>
                </div>
                {selected.sizeFee > 0 && (
                  <div>
                    <dt>Size · {sizeName}</dt>
                    <dd>{formatMoney(selected.sizeFee, selected.currency)}</dd>
                  </div>
                )}
                {selected.expressFee > 0 && (
                  <div>
                    <dt>Express delivery</dt>
                    <dd>{formatMoney(selected.expressFee, selected.currency)}</dd>
                  </div>
                )}
                {selected.discount > 0 && (
                  <div className="fare-discount">
                    <dt>Promo {request.promoCode}</dt>
                    <dd>− {formatMoney(selected.discount, selected.currency)}</dd>
                  </div>
                )}
                <div className="fare-total">
                  <dt>Total</dt>
                  <dd>{formatMoney(selected.total, selected.currency)}</dd>
                </div>
              </dl>

              {error && (
                <p className="alert" role="alert">
                  {error}
                </p>
              )}
              <Button className="fare-confirm" onClick={onConfirm} disabled={submitting}>
                {submitting ? "Booking…" : `Confirm & book · ${formatMoney(selected.total, selected.currency)}`}
              </Button>
              <p className="fare-note">This is the expected fee; the fee is fixed when you book.</p>
            </section>
          )}
        </>
      )}
    </div>
  );
}
