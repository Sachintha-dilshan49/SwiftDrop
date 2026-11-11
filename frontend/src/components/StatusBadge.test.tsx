import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ParcelStatus } from "../api/parcelApi";
import { STATUS_LABELS } from "../lib/parcelFormat";
import { StatusBadge } from "./StatusBadge";

describe("StatusBadge", () => {
  it.each([
    ["CREATED", "sd-status-created"],
    ["PICKED_UP", "sd-status-picked-up"],
    ["IN_TRANSIT", "sd-status-in-transit"],
    ["OUT_FOR_DELIVERY", "sd-status-out-for-delivery"],
    ["DELIVERED", "sd-status-delivered"],
    ["FAILED", "sd-status-failed"],
  ] as [ParcelStatus, string][])("shows %s with its label and colour class", (status, colourClass) => {
    render(<StatusBadge status={status} />);

    const badge = screen.getByText(STATUS_LABELS[status]);
    expect(badge).toHaveClass("sd-status-badge", colourClass);
  });

  it("keeps extra class names", () => {
    render(<StatusBadge status="DELIVERED" className="tracking-badge" />);

    expect(screen.getByText(STATUS_LABELS.DELIVERED)).toHaveClass("sd-status-badge", "tracking-badge");
  });
});
