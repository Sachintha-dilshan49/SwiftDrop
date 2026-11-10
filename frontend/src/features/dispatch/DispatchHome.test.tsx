import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CourierWorkload, DispatchApiError, UnassignedParcel, dispatchApi } from "../../api/dispatchApi";
import { DispatchHome } from "./DispatchHome";

vi.mock("../../api/dispatchApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/dispatchApi")>();
  return {
    ...actual,
    dispatchApi: { getUnassignedParcels: vi.fn(), getWorkload: vi.fn(), assign: vi.fn() },
  };
});

const getUnassignedParcels = vi.mocked(dispatchApi.getUnassignedParcels);
const getWorkload = vi.mocked(dispatchApi.getWorkload);
const assign = vi.mocked(dispatchApi.assign);

function parcel(trackingNo: string, overrides: Partial<UnassignedParcel> = {}): UnassignedParcel {
  return {
    parcelId: `id-${trackingNo}`,
    trackingNo,
    pickupAddress: "Liberty Plaza",
    dropoffAddress: "Rajagiriya Flyover",
    size: "SMALL",
    weightKg: 1.2,
    createdAt: new Date(Date.now() - 3 * 60 * 60_000).toISOString(),
    ...overrides,
  };
}

function courier(firstName: string | null, overrides: Partial<CourierWorkload> = {}): CourierWorkload {
  return {
    courierId: `courier-${firstName ?? "unnamed"}-0000`,
    userId: `user-${firstName}`,
    firstName,
    status: "AVAILABLE",
    activeDeliveries: 0,
    ...overrides,
  };
}

describe("DispatchHome", () => {
  beforeEach(() => {
    getUnassignedParcels.mockReset();
    getWorkload.mockReset();
    assign.mockReset();
  });

  it("lists parcels waiting for a courier with route, size and waiting time", async () => {
    getUnassignedParcels.mockResolvedValue([parcel("SD-0001"), parcel("SD-0002")]);

    render(<DispatchHome token="token-1" />);

    const row = (await screen.findByText("SD-0001")).closest("tr") as HTMLElement;
    expect(within(row).getByText("Liberty Plaza")).toBeInTheDocument();
    expect(within(row).getByText("Rajagiriya Flyover")).toBeInTheDocument();
    expect(within(row).getByText("Small · 1.2 kg")).toBeInTheDocument();
    expect(within(row).getByText("3 h")).toBeInTheDocument();
    expect(screen.getByText("2 parcels")).toBeInTheDocument();
    expect(getUnassignedParcels).toHaveBeenCalledWith("token-1");
  });

  it("assigns a parcel to the least busy courier that is not offline", async () => {
    getUnassignedParcels.mockResolvedValue([parcel("SD-0001")]);
    getWorkload.mockResolvedValue([
      courier("Nadeesha", { status: "OFFLINE" }),
      courier("Kasun", { activeDeliveries: 1 }),
      courier("Tharindu", { status: "BUSY", activeDeliveries: 4 }),
    ]);
    assign.mockResolvedValue({
      id: "a-1", parcelId: "id-SD-0001", trackingNo: "SD-0001", courierId: "courier-Kasun-0000",
      assignedByUserId: "d-1", status: "ASSIGNED", assignedAt: "2026-11-10T10:00:00Z",
    });
    render(<DispatchHome token="token-1" />);

    fireEvent.click(await screen.findByRole("button", { name: "Assign SD-0001" }));

    const dialog = await screen.findByRole("dialog", { name: "Assign SD-0001" });
    expect(await within(dialog).findByLabelText(/Kasun/)).toBeChecked();
    expect(within(dialog).queryByText("Nadeesha")).not.toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole("button", { name: "Assign courier" }));

    expect(await screen.findByRole("status")).toHaveTextContent("SD-0001 assigned to Kasun.");
    expect(assign).toHaveBeenCalledWith("id-SD-0001", "courier-Kasun-0000", "token-1");
    expect(screen.queryByText("SD-0001", { selector: "td" })).not.toBeInTheDocument();
  });

  it("keeps the parcel and shows dispatch-service's reason when the assignment is refused", async () => {
    getUnassignedParcels.mockResolvedValue([parcel("SD-0001")]);
    getWorkload.mockResolvedValue([courier("Kasun")]);
    assign.mockRejectedValue(new DispatchApiError(409, "Parcel SD-0001 already has an active assignment."));
    render(<DispatchHome token="token-1" />);

    fireEvent.click(await screen.findByRole("button", { name: "Assign SD-0001" }));
    const dialog = await screen.findByRole("dialog");
    await within(dialog).findByLabelText(/Kasun/);
    fireEvent.click(within(dialog).getByRole("button", { name: "Assign courier" }));

    expect(await within(dialog).findByRole("alert")).toHaveTextContent("already has an active assignment");
    expect(screen.getByText("SD-0001", { selector: "td" })).toBeInTheDocument();
  });

  it("says so when every parcel has a courier", async () => {
    getUnassignedParcels.mockResolvedValue([]);

    render(<DispatchHome token="token-1" />);

    expect(await screen.findByText("Every parcel has a courier. Nice work.")).toBeInTheDocument();
  });

  it("asks to sign in again when the session has expired", async () => {
    getUnassignedParcels.mockRejectedValue(new DispatchApiError(401, "Unauthorized"));

    render(<DispatchHome token="token-1" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Your session has expired");
  });

  it("shows each courier's name, status and active deliveries on the workload tab", async () => {
    getUnassignedParcels.mockResolvedValue([]);
    getWorkload.mockResolvedValue([
      courier("Kasun", { activeDeliveries: 0 }),
      courier(null, { courierId: "abcdef12-3456", status: "BUSY", activeDeliveries: 3 }),
    ]);
    render(<DispatchHome token="token-1" />);

    fireEvent.click(screen.getByRole("tab", { name: "Couriers & workload" }));

    const kasun = (await screen.findByText("Kasun")).closest("tr") as HTMLElement;
    expect(within(kasun).getByText("Available")).toBeInTheDocument();
    const unnamed = screen.getByText("Courier abcdef12").closest("tr") as HTMLElement;
    expect(within(unnamed).getByText("Busy")).toBeInTheDocument();
    expect(within(unnamed).getByText("3")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Couriers & workload" })).toHaveAttribute("aria-selected", "true");
  });
});
