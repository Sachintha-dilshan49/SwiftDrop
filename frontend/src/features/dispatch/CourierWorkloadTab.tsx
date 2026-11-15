import { useEffect, useState } from "react";
import { CourierWorkload, DispatchApiError, dispatchApi } from "../../api/dispatchApi";
import { COURIER_STATUS_LABELS, courierName } from "./dispatchFormat";

const SESSION_EXPIRED = "Your session has expired. Please sign out and sign in again.";

/** Every courier with their active deliveries, least busy first. */
export function CourierWorkloadTab({ token }: { token: string }) {
  const [couriers, setCouriers] = useState<CourierWorkload[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    dispatchApi
      .getWorkload(token)
      .then((rows) => current && setCouriers(rows))
      .catch((loadError: unknown) => {
        if (current) {
          setError(loadError instanceof DispatchApiError && loadError.status === 401
            ? SESSION_EXPIRED
            : "Could not load the courier workload. Please try again.");
        }
      });
    return () => {
      current = false;
    };
  }, [token]);

  return (
    <section className="dispatch-card" aria-labelledby="workload-title">
      <div className="dispatch-card-heading">
        <h2 id="workload-title">Couriers &amp; workload</h2>
        {couriers && <span>{couriers.length} couriers</span>}
      </div>

      {error && <p className="alert" role="alert">{error}</p>}
      {!couriers && !error && <p className="dispatch-note">Loading couriers…</p>}
      {couriers?.length === 0 && <p className="dispatch-note">No couriers yet.</p>}

      {couriers && couriers.length > 0 && (
        <table className="dispatch-table">
          <thead>
            <tr>
              <th scope="col">Courier</th>
              <th scope="col">Status</th>
              <th scope="col" className="dispatch-number">Active deliveries</th>
            </tr>
          </thead>
          <tbody>
            {couriers.map((courier) => (
              <tr key={courier.courierId}>
                <td data-label="Courier">
                  <span className="dispatch-courier">
                    <span className="dispatch-avatar" aria-hidden="true">
                      {courierName(courier).charAt(0).toUpperCase()}
                    </span>
                    {courierName(courier)}
                  </span>
                </td>
                <td data-label="Status">
                  <span className={`dispatch-pill dispatch-pill-${courier.status.toLowerCase()}`}>
                    {COURIER_STATUS_LABELS[courier.status]}
                  </span>
                </td>
                <td data-label="Active deliveries" className="dispatch-number">{courier.activeDeliveries}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
