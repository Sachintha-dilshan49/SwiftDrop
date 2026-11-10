import { useState } from "react";
import { CourierWorkloadTab } from "./CourierWorkloadTab";
import { UnassignedParcelsTab } from "./UnassignedParcelsTab";
import "./Dispatch.css";

type Tab = "unassigned" | "workload" | "reports";

const TABS: { id: Tab; label: string }[] = [
  { id: "unassigned", label: "Unassigned" },
  { id: "workload", label: "Couriers & workload" },
  { id: "reports", label: "Reports" },
];

type DispatchHomeProps = {
  /** The signed-in dispatcher's access token. */
  token: string;
};

/** The dispatcher's screens: parcels waiting for a courier, courier workload and reports. */
export function DispatchHome({ token }: DispatchHomeProps) {
  const [tab, setTab] = useState<Tab>("unassigned");

  return (
    <div className="dispatch">
      <div className="dispatch-tabs" role="tablist" aria-label="Dispatch">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            id={`dispatch-tab-${item.id}`}
            aria-selected={tab === item.id}
            aria-controls={`dispatch-panel-${item.id}`}
            className="dispatch-tab"
            onClick={() => setTab(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`dispatch-panel-${tab}`} aria-labelledby={`dispatch-tab-${tab}`}>
        {tab === "unassigned" && <UnassignedParcelsTab token={token} />}
        {tab === "workload" && <CourierWorkloadTab token={token} />}
        {tab === "reports" && (
          <section className="dispatch-card dispatch-empty">
            <h2>Reports</h2>
            <p>Daily delivery reports are coming soon.</p>
          </section>
        )}
      </div>
    </div>
  );
}
