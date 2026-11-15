import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CourierDelivery, DispatchApiError, dispatchApi } from "../../api/dispatchApi";
import { CourierHome } from "./CourierHome";

vi.mock("../../api/dispatchApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/dispatchApi")>();
  return {
    ...actual,
    dispatchApi: { getTodaysDeliveries: vi.fn(), updateDeliveryStatus: vi.fn() },
  };
});

const getTodaysDeliveries = vi.mocked(dispatchApi.getTodaysDeliveries);
const updateDeliveryStatus = vi.mocked(dispatchApi.updateDeliveryStatus);

function delivery(trackingNo: string, overrides: Partial<CourierDelivery> = {}): CourierDelivery {
  return {
    assignmentId: `a-${trackingNo}`,
    parcelId: `p-${trackingNo}`,
    status: "ASSIGNED",
    parcelStatus: "CREATED",
    assignedAt: "2026-11-11T04:00:00Z",
    trackingNo,
    pickupAddress: "Liberty Plaza",
    dropoffAddress: "Rajagiriya Flyover",
    recipientName: "Nimal",
    size: "SMALL",
    weightKg: 1.2,
    ...overrides,
  };
}

async function openDelivery(trackingNo: string) {
  fireEvent.click(await screen.findByRole("button", { name: `Open ${trackingNo}` }));
}

describe("CourierHome", () => {
  beforeEach(() => {
    getTodaysDeliveries.mockReset();
    updateDeliveryStatus.mockReset();
  });

  it("lists today's open deliveries with stops, recipient and status, and finished ones under Done", async () => {
    getTodaysDeliveries.mockResolvedValue([
      delivery("SD-0001"),
      delivery("SD-0002", { status: "COMPLETED", parcelStatus: "DELIVERED" }),
    ]);

    render(<CourierHome token="token-1" />);

    const card = await screen.findByRole("button", { name: "Open SD-0001" });
    expect(within(card).getByText("Liberty Plaza")).toBeInTheDocument();
    expect(within(card).getByText("Rajagiriya Flyover")).toBeInTheDocument();
    expect(within(card).getByText("For Nimal")).toBeInTheDocument();
    expect(within(card).getByText("To pick up")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Open SD-0002" })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /Done/ }));

    expect(within(screen.getByRole("button", { name: "Open SD-0002" })).getByText("Delivered")).toBeInTheDocument();
    expect(getTodaysDeliveries).toHaveBeenCalledWith("token-1");
  });

  it("offers only the next step and moves the parcel on", async () => {
    getTodaysDeliveries.mockResolvedValue([delivery("SD-0001")]);
    updateDeliveryStatus.mockResolvedValue(delivery("SD-0001", { status: "IN_PROGRESS", parcelStatus: "PICKED_UP" }));
    render(<CourierHome token="token-1" />);
    await openDelivery("SD-0001");

    expect(screen.queryByRole("button", { name: "Delivered" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Picked up" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirm: picked up" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Marked as picked up.");
    expect(updateDeliveryStatus).toHaveBeenCalledWith(
      "a-SD-0001",
      { status: "PICKED_UP", note: "", recipientName: undefined, failureReason: undefined, photo: undefined },
      "token-1",
    );
    expect(screen.getByRole("button", { name: "On the way" })).toBeInTheDocument();
  });

  it("needs a reason before a failed delivery is sent", async () => {
    getTodaysDeliveries.mockResolvedValue([
      delivery("SD-0001", { status: "IN_PROGRESS", parcelStatus: "OUT_FOR_DELIVERY" }),
    ]);
    updateDeliveryStatus.mockResolvedValue(delivery("SD-0001", { status: "FAILED", parcelStatus: "FAILED" }));
    render(<CourierHome token="token-1" />);
    await openDelivery("SD-0001");

    fireEvent.click(screen.getByRole("button", { name: "Delivery failed" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirm: delivery failed" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Please say why the delivery failed.");
    expect(updateDeliveryStatus).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText("Reason (required)"), { target: { value: "Gate locked" } });
    fireEvent.click(screen.getByRole("button", { name: "Confirm: delivery failed" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Marked as failed.");
    expect(updateDeliveryStatus.mock.calls[0][1]).toMatchObject({ status: "FAILED", failureReason: "Gate locked" });
  });

  it("sends who received it and the proof photo when delivered", async () => {
    getTodaysDeliveries.mockResolvedValue([
      delivery("SD-0001", { status: "IN_PROGRESS", parcelStatus: "OUT_FOR_DELIVERY" }),
    ]);
    updateDeliveryStatus.mockResolvedValue(delivery("SD-0001", { status: "COMPLETED", parcelStatus: "DELIVERED" }));
    render(<CourierHome token="token-1" />);
    await openDelivery("SD-0001");
    fireEvent.click(screen.getByRole("button", { name: "Delivered" }));

    expect(screen.getByLabelText("Received by")).toHaveValue("Nimal");
    const photo = new File(["jpeg"], "door.jpg", { type: "image/jpeg" });
    fireEvent.change(screen.getByLabelText(/Proof photo/), { target: { files: [photo] } });
    fireEvent.click(screen.getByRole("button", { name: "Confirm: delivered" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Marked as delivered.");
    expect(updateDeliveryStatus.mock.calls[0][1]).toMatchObject({ status: "DELIVERED", recipientName: "Nimal", photo });
    expect(screen.queryByRole("button", { name: "Delivered" })).not.toBeInTheDocument();
  });

  it("does not send a photo that is not an image", async () => {
    getTodaysDeliveries.mockResolvedValue([
      delivery("SD-0001", { status: "IN_PROGRESS", parcelStatus: "OUT_FOR_DELIVERY" }),
    ]);
    render(<CourierHome token="token-1" />);
    await openDelivery("SD-0001");
    fireEvent.click(screen.getByRole("button", { name: "Delivered" }));

    fireEvent.change(screen.getByLabelText(/Proof photo/), {
      target: { files: [new File(["%PDF"], "doc.pdf", { type: "application/pdf" })] },
    });
    fireEvent.click(screen.getByRole("button", { name: "Confirm: delivered" }));

    expect(screen.getByRole("alert")).toHaveTextContent("JPEG, PNG or WebP");
    expect(updateDeliveryStatus).not.toHaveBeenCalled();
  });

  it("shows parcel-service's reason when the step is refused", async () => {
    getTodaysDeliveries.mockResolvedValue([delivery("SD-0001")]);
    updateDeliveryStatus.mockRejectedValue(
      new DispatchApiError(409, "Parcel cannot move from IN_TRANSIT to PICKED_UP."));
    render(<CourierHome token="token-1" />);
    await openDelivery("SD-0001");

    fireEvent.click(screen.getByRole("button", { name: "Picked up" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirm: picked up" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("cannot move from IN_TRANSIT to PICKED_UP");
  });

  it("goes back to the list", async () => {
    getTodaysDeliveries.mockResolvedValue([delivery("SD-0001")]);
    render(<CourierHome token="token-1" />);
    await openDelivery("SD-0001");

    expect(screen.getByRole("heading", { name: "SD-0001" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "← Today's deliveries" }));

    expect(screen.getByRole("heading", { name: "Today's deliveries" })).toBeInTheDocument();
  });

  it("says when nothing is left to deliver", async () => {
    getTodaysDeliveries.mockResolvedValue([]);

    render(<CourierHome token="token-1" />);

    expect(await screen.findByText("Nothing left to deliver today.")).toBeInTheDocument();
  });
});
