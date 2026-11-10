package lk.swiftdrop.parcel.support;

import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.RouteType;

import java.math.BigDecimal;

/** Fills the route and fee columns (V6) for tests that build a parcel by hand instead of through a quote. */
public final class ParcelFixtures {

    private ParcelFixtures() {
    }

    /** Colombo South to Colombo Central at standard speed, the whole fee as the base fee. */
    public static Parcel withRouteAndFee(Parcel parcel) {
        parcel.setPickupZone("COLOMBO_SOUTH");
        parcel.setDropoffZone("COLOMBO_CENTRAL");
        parcel.setSpeed(DeliverySpeed.STANDARD);
        if (parcel.getMerchantId() != null) {
            parcel.setRecipientEmail("recipient@example.com");
        }
        parcel.applyFee(RouteType.NEIGHBOURING, parcel.getFeeAmount(), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, parcel.getFeeAmount(), parcel.getFeeCurrency());
        return parcel;
    }
}
