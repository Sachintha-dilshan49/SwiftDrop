import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ParcelApiError, ParcelResponse, parcelApi } from "../../api/parcelApi";
import { BookingForm } from "./BookingForm";

vi.mock("../../api/parcelApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/parcelApi")>();
  return { ...actual, parcelApi: { createParcel: vi.fn() } };
});

const createParcel = vi.mocked(parcelApi.createParcel);

const created = {
  id: "p-1",
  trackingNo: "SD-7K3Q-9P2M",
  feeAmount: 750,
  feeCurrency: "LKR",
} as ParcelResponse;

function fillValidBooking() {
  const type = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
  type("Pickup address", "123 Main St, Colombo");
  type("Pickup zone", "COLOMBO_SOUTH");
  type("Drop-off address", "456 High St, Kandy");
  type("Drop-off zone", "KANDY");
  type("Recipient name", "Nimal Perera");
  type("Recipient phone", "077 123 4567");
  type("Size", "MEDIUM");
  type("Weight (kg)", "2.5");
  fireEvent.click(screen.getByLabelText(/Express/));
}

function renderForm() {
  const onBooked = vi.fn();
  const onClose = vi.fn();
  render(<BookingForm token="token-1" onBooked={onBooked} onClose={onClose} />);
  return { onBooked, onClose };
}

describe("BookingForm", () => {
  beforeEach(() => {
    createParcel.mockReset();
  });

  it("shows what is missing and does not call the server", async () => {
    renderForm();

    fireEvent.click(screen.getByRole("button", { name: "Book delivery" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Please check the highlighted fields.");
    expect(screen.getByText("Pickup address is required")).toBeInTheDocument();
    expect(screen.getByText("Choose a parcel size")).toBeInTheDocument();
    expect(screen.getByLabelText("Pickup address")).toHaveAttribute("aria-invalid", "true");
    expect(createParcel).not.toHaveBeenCalled();
  });

  it("books a valid delivery and shows the tracking number and fee", async () => {
    createParcel.mockResolvedValue(created);
    const { onBooked } = renderForm();

    fillValidBooking();
    fireEvent.click(screen.getByRole("button", { name: "Book delivery" }));

    expect(await screen.findByRole("heading", { name: "Delivery booked" })).toBeInTheDocument();
    expect(screen.getByText("SD-7K3Q-9P2M")).toBeInTheDocument();
    expect(screen.getByText("Fee: LKR 750.00")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Track this parcel" })).toHaveAttribute("href", "/track/SD-7K3Q-9P2M");
    expect(createParcel).toHaveBeenCalledWith({
      pickupAddress: "123 Main St, Colombo",
      pickupZone: "COLOMBO_SOUTH",
      dropoffAddress: "456 High St, Kandy",
      dropoffZone: "KANDY",
      recipientName: "Nimal Perera",
      recipientPhone: "0771234567",
      size: "MEDIUM",
      weightKg: 2.5,
      speed: "EXPRESS",
    }, "token-1");
    expect(onBooked).toHaveBeenCalledWith(created);
  });

  it("shows the server's field errors next to the fields", async () => {
    createParcel.mockRejectedValue(new ParcelApiError(400, "One or more fields are invalid.", [
      { field: "pickupZone", message: "Pickup zone must be a zone code such as COLOMBO_SOUTH" },
    ]));
    renderForm();

    fillValidBooking();
    fireEvent.click(screen.getByRole("button", { name: "Book delivery" }));

    expect(await screen.findByText("Pickup zone must be a zone code such as COLOMBO_SOUTH")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Please check the highlighted fields.");
  });

  it("explains when no price can be calculated right now", async () => {
    createParcel.mockRejectedValue(new ParcelApiError(503, "Service temporarily unavailable"));
    renderForm();

    fillValidBooking();
    fireEvent.click(screen.getByRole("button", { name: "Book delivery" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "We could not get a price right now. Please try again in a moment.");
  });

  it("starts a new empty form with Book another", async () => {
    createParcel.mockResolvedValue(created);
    renderForm();
    fillValidBooking();
    fireEvent.click(screen.getByRole("button", { name: "Book delivery" }));

    fireEvent.click(await screen.findByRole("button", { name: "Book another" }));

    await waitFor(() => expect(screen.getByLabelText("Pickup address")).toHaveValue(""));
  });

  it("closes with Cancel", () => {
    const { onClose } = renderForm();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onClose).toHaveBeenCalled();
  });
});
