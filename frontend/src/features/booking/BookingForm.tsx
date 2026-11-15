import { FormEvent, ReactNode, useState } from "react";
import { ParcelApiError, ParcelResponse, parcelApi } from "../../api/parcelApi";
import { Button } from "../../components/Button";
import { formatMoney } from "../../lib/parcelFormat";
import { trackingPath } from "../tracking/trackingNumber";
import { SIZE_OPTIONS, ZONES } from "./bookingOptions";
import {
  BookingErrors,
  BookingFormValues,
  EMPTY_BOOKING,
  toCreateRequest,
  validateBooking,
} from "./bookingValidation";
import { FareStep } from "./FareStep";
import "./BookingForm.css";

const FIELDS = new Set<string>(Object.keys(EMPTY_BOOKING));

/** Turns a failed booking into field messages and one message for the whole form. */
function describeFailure(error: unknown): { fieldErrors: BookingErrors; message: string } {
  if (!(error instanceof ParcelApiError)) {
    return { fieldErrors: {}, message: "Could not book the delivery. Please try again." };
  }
  if (error.status === 401) {
    return { fieldErrors: {}, message: "Your session has expired. Please sign out and sign in again." };
  }
  if (error.status === 503) {
    return { fieldErrors: {}, message: "We could not get a price right now. Please try again in a moment." };
  }
  if (error.status === 400) {
    const fieldErrors: BookingErrors = {};
    for (const { field, message } of error.fieldErrors) {
      if (FIELDS.has(field)) {
        fieldErrors[field as keyof BookingFormValues] = message;
      }
    }
    const message = Object.keys(fieldErrors).length > 0 ? "Please check the highlighted fields." : error.message;
    return { fieldErrors, message };
  }
  return { fieldErrors: {}, message: "Could not book the delivery. Please try again." };
}

function Field(props: { id: string; label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <div className="booking-field">
      <label htmlFor={props.id}>{props.label}</label>
      {props.children}
      {props.hint && !props.error && <p className="booking-hint">{props.hint}</p>}
      {props.error && (
        <p className="booking-error" id={`${props.id}-error`}>
          {props.error}
        </p>
      )}
    </div>
  );
}

type BookingFormProps = {
  token: string;
  /** Called after a successful booking, for example to refresh the delivery list. */
  onBooked: (parcel: ParcelResponse) => void;
  onClose: () => void;
};

/**
 * Customer booking (diagram seq-create-delivery) in two steps, as in Figma: the details, then the fare per
 * speed and Confirm & book. The fee comes from pricing-service, never from this form.
 */
export function BookingForm({ token, onBooked, onClose }: BookingFormProps) {
  const [values, setValues] = useState<BookingFormValues>(EMPTY_BOOKING);
  const [errors, setErrors] = useState<BookingErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [booked, setBooked] = useState<ParcelResponse | null>(null);
  const [step, setStep] = useState<"details" | "fare">("details");

  function update<K extends keyof BookingFormValues>(field: K, value: BookingFormValues[K]) {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  /** Props that link an input to its error message for screen readers. */
  function errorProps(field: keyof BookingFormValues) {
    return errors[field]
      ? { "aria-invalid": true, "aria-describedby": `${field}-error` }
      : { "aria-invalid": false };
  }

  /** Step 1: check the details, then show the fare. */
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validateBooking(values);
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) {
      setFormError("Please check the highlighted fields.");
      return;
    }
    setStep("fare");
  }

  /** Step 2: book. A field the server rejects is shown next to that field in step 1. */
  async function confirmBooking() {
    setSubmitting(true);
    setFormError(null);
    try {
      const parcel = await parcelApi.createParcel(toCreateRequest(values), token);
      setBooked(parcel);
      onBooked(parcel);
    } catch (bookingError) {
      const failure = describeFailure(bookingError);
      setErrors(failure.fieldErrors);
      setFormError(failure.message);
      if (Object.keys(failure.fieldErrors).length > 0) {
        setStep("details");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (booked) {
    return (
      <section className="booking" aria-labelledby="booking-done-title">
        <div className="booking-done" role="status">
          <h2 id="booking-done-title">Delivery booked</h2>
          <p>
            Tracking number <strong className="booking-tracking">{booked.trackingNo}</strong>
          </p>
          <p>Fee: {formatMoney(booked.feeAmount, booked.feeCurrency)}</p>
          <div className="booking-actions">
            <a className="booking-primary" href={trackingPath(booked.trackingNo)}>
              Track this parcel
            </a>
            <Button
              variant="secondary"
              onClick={() => {
                setValues(EMPTY_BOOKING);
                setBooked(null);
                setStep("details");
              }}
            >
              Book another
            </Button>
            <Button variant="secondary" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      </section>
    );
  }

  const heading = (
    <div className="booking-heading">
      <h2 id="booking-title">{step === "fare" ? "Fare and confirmation" : "Book a delivery"}</h2>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
    </div>
  );

  if (step === "fare") {
    return (
      <section className="booking" aria-labelledby="booking-title">
        {heading}
        <FareStep
          values={values}
          submitting={submitting}
          error={formError}
          onSpeedChange={(speed) => update("speed", speed)}
          onBack={() => {
            setFormError(null);
            setStep("details");
          }}
          onConfirm={() => void confirmBooking()}
        />
      </section>
    );
  }

  return (
    <section className="booking" aria-labelledby="booking-title">
      {heading}

      <form onSubmit={handleSubmit} noValidate>
        <fieldset className="booking-group">
          <legend>Pickup</legend>
          <Field id="pickupAddress" label="Pickup address" error={errors.pickupAddress}>
            <input
              id="pickupAddress"
              value={values.pickupAddress}
              onChange={(event) => update("pickupAddress", event.target.value)}
              autoComplete="street-address"
              {...errorProps("pickupAddress")}
            />
          </Field>
          <Field id="pickupZone" label="Pickup zone" error={errors.pickupZone}>
            <select
              id="pickupZone"
              value={values.pickupZone}
              onChange={(event) => update("pickupZone", event.target.value)}
              {...errorProps("pickupZone")}
            >
              <option value="">Choose a zone</option>
              {ZONES.map((zone) => (
                <option key={zone.code} value={zone.code}>
                  {zone.name}
                </option>
              ))}
            </select>
          </Field>
        </fieldset>

        <fieldset className="booking-group">
          <legend>Drop-off</legend>
          <Field id="dropoffAddress" label="Drop-off address" error={errors.dropoffAddress}>
            <input
              id="dropoffAddress"
              value={values.dropoffAddress}
              onChange={(event) => update("dropoffAddress", event.target.value)}
              {...errorProps("dropoffAddress")}
            />
          </Field>
          <Field id="dropoffZone" label="Drop-off zone" error={errors.dropoffZone}>
            <select
              id="dropoffZone"
              value={values.dropoffZone}
              onChange={(event) => update("dropoffZone", event.target.value)}
              {...errorProps("dropoffZone")}
            >
              <option value="">Choose a zone</option>
              {ZONES.map((zone) => (
                <option key={zone.code} value={zone.code}>
                  {zone.name}
                </option>
              ))}
            </select>
          </Field>
        </fieldset>

        <fieldset className="booking-group">
          <legend>Recipient</legend>
          <Field id="recipientName" label="Recipient name" error={errors.recipientName}>
            <input
              id="recipientName"
              value={values.recipientName}
              onChange={(event) => update("recipientName", event.target.value)}
              {...errorProps("recipientName")}
            />
          </Field>
          <Field id="recipientPhone" label="Recipient phone" error={errors.recipientPhone} hint="For example 0771234567">
            <input
              id="recipientPhone"
              type="tel"
              inputMode="tel"
              value={values.recipientPhone}
              onChange={(event) => update("recipientPhone", event.target.value)}
              {...errorProps("recipientPhone")}
            />
          </Field>
        </fieldset>

        <fieldset className="booking-group">
          <legend>Parcel</legend>
          <Field id="size" label="Size" error={errors.size}>
            <select
              id="size"
              value={values.size}
              onChange={(event) => update("size", event.target.value as BookingFormValues["size"])}
              {...errorProps("size")}
            >
              <option value="">Choose a size</option>
              {SIZE_OPTIONS.map((option) => (
                <option key={option.size} value={option.size}>
                  {option.name} (up to {option.maxWeightKg} kg)
                </option>
              ))}
            </select>
          </Field>
          <Field id="weightKg" label="Weight (kg)" error={errors.weightKg}>
            <input
              id="weightKg"
              inputMode="decimal"
              value={values.weightKg}
              onChange={(event) => update("weightKg", event.target.value)}
              {...errorProps("weightKg")}
            />
          </Field>
        </fieldset>


        <Field id="promoCode" label="Promo code (optional)" error={errors.promoCode}>
          <input
            id="promoCode"
            value={values.promoCode}
            onChange={(event) => update("promoCode", event.target.value)}
            autoComplete="off"
            {...errorProps("promoCode")}
          />
        </Field>

        {formError && (
          <p className="alert" role="alert">
            {formError}
          </p>
        )}

        <p className="booking-hint">Next you choose the delivery speed and see the fare.</p>
        <Button type="submit" className="booking-submit">
          See the fare
        </Button>
      </form>
    </section>
  );
}
