import { useEffect, useState } from "react";
import { CourierDelivery, DispatchApiError, dispatchApi } from "../../api/dispatchApi";
import { CourierDeliveryDetails } from "./CourierDeliveryDetails";
import { Stops } from "./Stops";
import { COURIER_STATUS_LABELS, currentStatus, isOpen } from "./courierFlow";
import "./Courier.css";

type Filter = "todo" | "done";

const SESSION_EXPIRED = "Your session has expired. Please sign out and sign in again.";

type CourierHomeProps = {
  /** The signed-in courier's access token. */
  token: string;
};

/** The courier's phone view: today's deliveries, one delivery's details and its status update. */
export function CourierHome({ token }: CourierHomeProps) {
  const [deliveries, setDeliveries] = useState<CourierDelivery[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>("todo");
  const [openId, setOpenId] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    dispatchApi
      .getTodaysDeliveries(token)
      .then((rows) => current && setDeliveries(rows))
      .catch((loadError: unknown) => {
        if (current) {
          setError(loadError instanceof DispatchApiError && loadError.status === 401
            ? SESSION_EXPIRED
            : "Could not load your deliveries. Please try again.");
        }
      });
    return () => {
      current = false;
    };
  }, [token]);

  function updated(delivery: CourierDelivery) {
    setDeliveries((rows) => rows?.map((row) => (row.assignmentId === delivery.assignmentId ? delivery : row)) ?? null);
  }

  const open = deliveries?.find((delivery) => delivery.assignmentId === openId);
  if (open) {
    return (
      <CourierDeliveryDetails
        delivery={open}
        token={token}
        onBack={() => setOpenId(null)}
        onUpdated={updated}
      />
    );
  }

  const todo = deliveries?.filter(isOpen) ?? [];
  const done = deliveries?.filter((delivery) => !isOpen(delivery)) ?? [];
  const shown = filter === "todo" ? todo : done;

  return (
    <section className="courier" aria-labelledby="courier-title">
      <h2 id="courier-title" className="courier-title">Today&apos;s deliveries</h2>

      <div className="courier-tabs" role="tablist" aria-label="Deliveries">
        <button type="button" role="tab" aria-selected={filter === "todo"} className="courier-tab"
          onClick={() => setFilter("todo")}>
          To do <span>{todo.length}</span>
        </button>
        <button type="button" role="tab" aria-selected={filter === "done"} className="courier-tab"
          onClick={() => setFilter("done")}>
          Done <span>{done.length}</span>
        </button>
      </div>

      {error && <p className="alert" role="alert">{error}</p>}
      {!deliveries && !error && <p className="courier-note">Loading your deliveries…</p>}
      {deliveries && shown.length === 0 && (
        <p className="courier-note">
          {filter === "todo" ? "Nothing left to deliver today." : "No finished deliveries yet today."}
        </p>
      )}

      <ul className="courier-list">
        {shown.map((delivery) => {
          const status = currentStatus(delivery);
          return (
            <li key={delivery.assignmentId}>
              <button
                type="button"
                className="courier-card"
                onClick={() => setOpenId(delivery.assignmentId)}
                aria-label={`Open ${delivery.trackingNo ?? "delivery"}`}
              >
                <span className="courier-card-top">
                  <strong className="courier-tracking">{delivery.trackingNo ?? "Parcel"}</strong>
                  {status && (
                    <span className={`courier-pill courier-pill-${status.toLowerCase()}`}>
                      {COURIER_STATUS_LABELS[status]}
                    </span>
                  )}
                </span>
                <Stops pickup={delivery.pickupAddress} dropoff={delivery.dropoffAddress} />
                {delivery.recipientName && (
                  <span className="courier-card-meta">For {delivery.recipientName}</span>
                )}
              </button>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
