import { useState } from "react";
import { MyDeliveriesPage } from "../deliveries/MyDeliveriesPage";
import { BookingForm } from "./BookingForm";
import "./BookingForm.css";

type CustomerHomeProps = {
  token: string;
};

/** The customer's dashboard screen: book a delivery, and the delivery list that refreshes after a booking. */
export function CustomerHome({ token }: CustomerHomeProps) {
  const [booking, setBooking] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="customer-home">
      {booking ? (
        <BookingForm
          token={token}
          onBooked={() => setRefreshKey((current) => current + 1)}
          onClose={() => setBooking(false)}
        />
      ) : (
        <button type="button" className="customer-book-button" onClick={() => setBooking(true)}>
          Book a delivery
        </button>
      )}
      <MyDeliveriesPage token={token} refreshKey={refreshKey} />
    </div>
  );
}
