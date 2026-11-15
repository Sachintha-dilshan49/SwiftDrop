import { useState } from "react";
import { Button } from "../../components/Button";
import { navigate } from "../../routes/router";
import { MyDeliveriesPage } from "../deliveries/MyDeliveriesPage";
import { BookingForm } from "./BookingForm";
import "./BookingForm.css";

type CustomerHomeProps = {
  token: string;
  /** Already a merchant: offer the merchant portal instead of the registration form. */
  isMerchant?: boolean;
};

/** The customer's dashboard screen: book a delivery, and the delivery list that refreshes after a booking. */
export function CustomerHome({ token, isMerchant = false }: CustomerHomeProps) {
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
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <Button className="customer-book-button" onClick={() => setBooking(true)}>
            Book a delivery
          </Button>
          <Button
            variant="secondary"
            className="customer-book-button"
            onClick={() => navigate(isMerchant ? "/merchant" : "/merchant/register")}
          >
            {isMerchant ? "Open merchant portal" : "Register as a merchant"}
          </Button>
        </div>
      )}
      <MyDeliveriesPage token={token} refreshKey={refreshKey} />
    </div>
  );
}
