import { useEffect, useState } from "react";
import { DailyReport, DispatchApiError, dispatchApi } from "../../api/dispatchApi";
import { courierName, rangeStart, sriLankaDate, successRate } from "./dispatchFormat";

const SESSION_EXPIRED = "Your session has expired. Please sign out and sign in again.";
const RANGES = [7, 30] as const;
type Range = (typeof RANGES)[number];

function dayLabel(date: string, long = false): string {
  const [year, month, day] = date.split("-").map(Number);
  return new Intl.DateTimeFormat("en-GB", long
    ? { weekday: "short", day: "numeric", month: "short" }
    : { day: "numeric", month: "short" }).format(new Date(Date.UTC(year, month - 1, day)));
}

/** Deliveries per day for the last 7 or 30 days, with totals and each courier's share. */
export function ReportsTab({ token }: { token: string }) {
  const [range, setRange] = useState<Range>(7);
  const [report, setReport] = useState<DailyReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    setReport(null);
    setError(null);
    dispatchApi
      .getDailyReport(token, { from: rangeStart(range), to: sriLankaDate(new Date()) })
      .then((loaded) => current && setReport(loaded))
      .catch((loadError: unknown) => {
        if (current) {
          setError(loadError instanceof DispatchApiError && loadError.status === 401
            ? SESSION_EXPIRED
            : "Could not load the delivery report. Please try again.");
        }
      });
    return () => {
      current = false;
    };
  }, [token, range]);

  const busiest = report ? Math.max(1, ...report.days.map((day) => day.delivered + day.failed)) : 1;

  return (
    <section className="dispatch-card" aria-labelledby="reports-title">
      <div className="dispatch-card-heading">
        <h2 id="reports-title">Delivery report</h2>
        <div className="dispatch-range" role="group" aria-label="Report period">
          {RANGES.map((days) => (
            <button key={days} type="button" className="dispatch-range-option" aria-pressed={range === days}
              onClick={() => setRange(days)}>
              Last {days} days
            </button>
          ))}
        </div>
      </div>

      {error && <p className="alert" role="alert">{error}</p>}
      {!report && !error && <p className="dispatch-note">Loading the report…</p>}

      {report && (
        <>
          <div className="dispatch-stats">
            <Stat label="Assigned" value={report.totals.assigned} />
            <Stat label="Delivered" value={report.totals.delivered} />
            <Stat label="Failed" value={report.totals.failed} />
            <Stat label="Success rate" value={successRate(report.totals.delivered, report.totals.failed)} />
          </div>

          <h3 className="dispatch-subheading">Finished deliveries per day</h3>
          <div className="dispatch-legend" aria-hidden="true">
            <span><i className="dispatch-swatch dispatch-swatch-delivered" />Delivered</span>
            <span><i className="dispatch-swatch dispatch-swatch-failed" />Failed</span>
          </div>
          <div className={`dispatch-chart dispatch-chart-${report.days.length > 7 ? "dense" : "wide"}`} aria-hidden="true">
            {report.days.map((day) => (
              <div key={day.date} className="dispatch-chart-day"
                title={`${dayLabel(day.date, true)}: ${day.delivered} delivered, ${day.failed} failed`}>
                <div className="dispatch-chart-bar">
                  <span className="dispatch-chart-failed" style={{ height: `${(day.failed / busiest) * 100}%` }} />
                  <span className="dispatch-chart-delivered" style={{ height: `${(day.delivered / busiest) * 100}%` }} />
                </div>
                <span className="dispatch-chart-label">{dayLabel(day.date)}</span>
              </div>
            ))}
          </div>

          <details className="dispatch-details">
            <summary>Daily numbers</summary>
            <table className="dispatch-table">
              <caption className="dispatch-visually-hidden">Deliveries per day</caption>
              <thead>
                <tr>
                  <th scope="col">Day</th>
                  <th scope="col" className="dispatch-number">Assigned</th>
                  <th scope="col" className="dispatch-number">Delivered</th>
                  <th scope="col" className="dispatch-number">Failed</th>
                </tr>
              </thead>
              <tbody>
                {[...report.days].reverse().map((day) => (
                  <tr key={day.date}>
                    <th scope="row" data-label="Day">{dayLabel(day.date, true)}</th>
                    <td data-label="Assigned" className="dispatch-number">{day.assigned}</td>
                    <td data-label="Delivered" className="dispatch-number">{day.delivered}</td>
                    <td data-label="Failed" className="dispatch-number">{day.failed}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </details>

          <h3 className="dispatch-subheading">Couriers</h3>
          {report.couriers.length === 0 ? (
            <p className="dispatch-note">No deliveries in this period.</p>
          ) : (
            <table className="dispatch-table">
              <thead>
                <tr>
                  <th scope="col">Courier</th>
                  <th scope="col" className="dispatch-number">Assigned</th>
                  <th scope="col" className="dispatch-number">Delivered</th>
                  <th scope="col" className="dispatch-number">Failed</th>
                  <th scope="col" className="dispatch-number">Success rate</th>
                </tr>
              </thead>
              <tbody>
                {report.couriers.map((courier) => (
                  <tr key={courier.courierId}>
                    <td data-label="Courier">
                      <span className="dispatch-courier">
                        <span className="dispatch-avatar" aria-hidden="true">
                          {courierName(courier).charAt(0).toUpperCase()}
                        </span>
                        {courierName(courier)}
                      </span>
                    </td>
                    <td data-label="Assigned" className="dispatch-number">{courier.assigned}</td>
                    <td data-label="Delivered" className="dispatch-number">{courier.delivered}</td>
                    <td data-label="Failed" className="dispatch-number">{courier.failed}</td>
                    <td data-label="Success rate" className="dispatch-number">
                      {successRate(courier.delivered, courier.failed)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  );
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="dispatch-stat">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
