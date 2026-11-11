import type { ParcelStatus } from "../api/parcelApi";
import { STATUS_LABELS } from "../lib/parcelFormat";
import "./StatusBadge.css";

type StatusBadgeProps = {
  status: ParcelStatus;
  className?: string;
};

/** Parcel status chip; colours follow CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED / FAILED. */
export function StatusBadge({ status, className }: StatusBadgeProps) {
  const modifier = status.toLowerCase().replace(/_/g, "-");
  const classes = ["sd-status-badge", `sd-status-${modifier}`, className].filter(Boolean).join(" ");
  return <span className={classes}>{STATUS_LABELS[status]}</span>;
}
