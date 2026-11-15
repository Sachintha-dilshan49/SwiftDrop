import { useEffect, useState } from "react";
import { CourierWorkload, DispatchApiError, UnassignedParcel, dispatchApi } from "../../api/dispatchApi";
import { SIZE_LABELS } from "../../lib/parcelFormat";
import { courierName, waitingTime } from "./dispatchFormat";

const SESSION_EXPIRED = "Your session has expired. Please sign out and sign in again.";

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof DispatchApiError) {
    return error.status === 401 ? SESSION_EXPIRED : error.message;
  }
  return fallback;
}

/** Parcels waiting for a courier, oldest first, each with an Assign button. */
export function UnassignedParcelsTab({ token }: { token: string }) {
  const [parcels, setParcels] = useState<UnassignedParcel[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [assigning, setAssigning] = useState<UnassignedParcel | null>(null);

  useEffect(() => {
    let current = true;
    dispatchApi
      .getUnassignedParcels(token)
      .then((rows) => current && setParcels(rows))
      .catch((loadError: unknown) => {
        if (current) {
          setError(errorMessage(loadError, "Could not load the parcels waiting for a courier."));
        }
      });
    return () => {
      current = false;
    };
  }, [token]);

  function assigned(parcel: UnassignedParcel, courier: CourierWorkload) {
    setParcels((rows) => rows?.filter((row) => row.parcelId !== parcel.parcelId) ?? null);
    setAssigning(null);
    setNotice(`${parcel.trackingNo} assigned to ${courierName(courier)}.`);
  }

  return (
    <section className="dispatch-card" aria-labelledby="unassigned-title">
      <div className="dispatch-card-heading">
        <h2 id="unassigned-title">Waiting for a courier</h2>
        {parcels && <span>{parcels.length} parcels</span>}
      </div>

      {notice && <p className="dispatch-success" role="status">{notice}</p>}
      {error && <p className="alert" role="alert">{error}</p>}
      {!parcels && !error && <p className="dispatch-note">Loading parcels…</p>}
      {parcels?.length === 0 && <p className="dispatch-note">Every parcel has a courier. Nice work.</p>}

      {parcels && parcels.length > 0 && (
        <table className="dispatch-table">
          <thead>
            <tr>
              <th scope="col">Parcel</th>
              <th scope="col">Route</th>
              <th scope="col">Size</th>
              <th scope="col">Waiting</th>
              <th scope="col"><span className="dispatch-visually-hidden">Action</span></th>
            </tr>
          </thead>
          <tbody>
            {parcels.map((parcel) => (
              <tr key={parcel.parcelId}>
                <td data-label="Parcel" className="dispatch-tracking">{parcel.trackingNo}</td>
                <td data-label="Route">
                  <span className="dispatch-route">
                    <span><i className="dispatch-dot dispatch-dot-pickup" aria-hidden="true" />{parcel.pickupAddress}</span>
                    <span><i className="dispatch-dot dispatch-dot-dropoff" aria-hidden="true" />{parcel.dropoffAddress}</span>
                  </span>
                </td>
                <td data-label="Size">
                  {SIZE_LABELS[parcel.size] ?? parcel.size} · {parcel.weightKg} kg
                </td>
                <td data-label="Waiting">{waitingTime(parcel.createdAt)}</td>
                <td className="dispatch-action">
                  <button
                    type="button"
                    className="dispatch-button"
                    onClick={() => {
                      setNotice(null);
                      setAssigning(parcel);
                    }}
                    aria-label={`Assign ${parcel.trackingNo}`}
                  >
                    Assign
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {assigning && (
        <AssignPanel
          parcel={assigning}
          token={token}
          onCancel={() => setAssigning(null)}
          onAssigned={(courier) => assigned(assigning, courier)}
        />
      )}
    </section>
  );
}

type AssignPanelProps = {
  parcel: UnassignedParcel;
  token: string;
  onCancel: () => void;
  onAssigned: (courier: CourierWorkload) => void;
};

/** Picks a courier for one parcel; offline couriers are left out and the least busy is chosen first. */
function AssignPanel({ parcel, token, onCancel, onAssigned }: AssignPanelProps) {
  const [couriers, setCouriers] = useState<CourierWorkload[] | null>(null);
  const [selected, setSelected] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let current = true;
    dispatchApi
      .getWorkload(token)
      .then((rows) => {
        if (current) {
          const available = rows.filter((courier) => courier.status !== "OFFLINE");
          setCouriers(available);
          setSelected(available[0]?.courierId ?? "");
        }
      })
      .catch((loadError: unknown) => {
        if (current) {
          setError(errorMessage(loadError, "Could not load the couriers."));
        }
      });
    return () => {
      current = false;
    };
  }, [token]);

  async function confirm() {
    const courier = couriers?.find((item) => item.courierId === selected);
    if (!courier) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await dispatchApi.assign(parcel.parcelId, courier.courierId, token);
      onAssigned(courier);
    } catch (assignError) {
      setError(errorMessage(assignError, "Could not assign the parcel. Please try again."));
      setSaving(false);
    }
  }

  return (
    <div className="dispatch-assign" role="dialog" aria-labelledby="assign-title">
      <h3 id="assign-title">Assign {parcel.trackingNo}</h3>
      <p className="dispatch-note">{parcel.pickupAddress} → {parcel.dropoffAddress}</p>

      {error && <p className="alert" role="alert">{error}</p>}
      {!couriers && !error && <p className="dispatch-note">Loading couriers…</p>}
      {couriers?.length === 0 && <p className="dispatch-note">No courier is available right now.</p>}

      {couriers && couriers.length > 0 && (
        <fieldset className="dispatch-couriers">
          <legend>Courier (least busy first)</legend>
          {couriers.map((courier) => (
            <label key={courier.courierId} className="dispatch-courier-option">
              <input
                type="radio"
                name="courier"
                value={courier.courierId}
                checked={selected === courier.courierId}
                onChange={() => setSelected(courier.courierId)}
              />
              <span className="dispatch-courier-name">{courierName(courier)}</span>
              <span className="dispatch-courier-load">
                {courier.activeDeliveries} active
              </span>
            </label>
          ))}
        </fieldset>
      )}

      <div className="dispatch-assign-actions">
        <button type="button" className="dispatch-button-outline" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
        <button
          type="button"
          className="dispatch-button"
          onClick={() => void confirm()}
          disabled={saving || !selected}
        >
          {saving ? "Assigning…" : "Assign courier"}
        </button>
      </div>
    </div>
  );
}
