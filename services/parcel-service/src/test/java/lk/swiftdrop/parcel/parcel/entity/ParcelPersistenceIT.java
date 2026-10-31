package lk.swiftdrop.parcel.parcel.entity;

import jakarta.persistence.EntityManager;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.support.AbstractContainersIT;
import lk.swiftdrop.parcel.support.ParcelFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The parcel entities against the real schema (V1–V5): mapping, defaults, history and constraints. */
class ParcelPersistenceIT extends AbstractContainersIT {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("A parcel and its first history entry are stored and read back with defaults filled in")
    void storesParcelWithHistory() {
        UUID parcelId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID courierId = UUID.randomUUID();

        inTransaction(() -> {
            Parcel parcel = new Parcel(parcelId, trackingNo(), customerId, null, "No. 12, Galle Road",
                    "No. 45, Peradeniya Road", "Kamal Silva", "+94719876543", ParcelSize.MEDIUM,
                    new BigDecimal("3.20"), new BigDecimal("820.00"), null);
            ParcelFixtures.withRouteAndFee(parcel);
            parcel.setCourierId(courierId);
            parcel.addStatusHistory(new ParcelStatusHistory(null, parcel, ParcelStatus.CREATED, customerId,
                    "CUSTOMER", "Delivery booking created", null));
            entityManager.persist(parcel);
        });

        inTransaction(() -> {
            Parcel stored = entityManager.find(Parcel.class, parcelId);
            assertThat(stored.getTrackingNo()).matches("SD-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}");
            assertThat(stored.getCustomerId()).isEqualTo(customerId);
            assertThat(stored.getMerchantId()).isNull();
            assertThat(stored.getCourierId()).isEqualTo(courierId);
            assertThat(stored.getPickupAddress()).isEqualTo("No. 12, Galle Road");
            assertThat(stored.getDropoffAddress()).isEqualTo("No. 45, Peradeniya Road");
            assertThat(stored.getRecipientName()).isEqualTo("Kamal Silva");
            assertThat(stored.getRecipientPhone()).isEqualTo("+94719876543");
            assertThat(stored.getSize()).isEqualTo(ParcelSize.MEDIUM);
            assertThat(stored.getWeightKg()).isEqualByComparingTo("3.20");
            assertThat(stored.getFeeAmount()).isEqualByComparingTo("820.00");
            assertThat(stored.getFeeCurrency()).isEqualTo("LKR");
            assertThat(stored.getStatus()).isEqualTo(ParcelStatus.CREATED);
            assertThat(stored.getFailureReason()).isNull();
            assertThat(stored.getCreatedAt()).isNotNull();
            assertThat(stored.getUpdatedAt()).isNull();

            assertThat(stored.getStatusHistory()).hasSize(1);
            ParcelStatusHistory history = stored.getStatusHistory().getFirst();
            assertThat(history.getId()).isNotNull();
            assertThat(history.getParcel().getId()).isEqualTo(parcelId);
            assertThat(history.getStatus()).isEqualTo(ParcelStatus.CREATED);
            assertThat(history.getChangedBy()).isEqualTo(customerId);
            assertThat(history.getChangedByRole()).isEqualTo("CUSTOMER");
            assertThat(history.getNote()).isEqualTo("Delivery booking created");
            assertThat(history.getProofReference()).isNull();
            assertThat(history.getCreatedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("A status change sets updated_at and history is read back oldest first")
    void statusChangeKeepsHistoryInOrder() {
        UUID parcelId = persistCreatedParcel();

        inTransaction(() -> {
            Parcel parcel = entityManager.find(Parcel.class, parcelId);
            parcel.setStatus(ParcelStatus.PICKED_UP);
            parcel.addStatusHistory(new ParcelStatusHistory(UUID.randomUUID(), parcel, ParcelStatus.PICKED_UP,
                    UUID.randomUUID(), "COURIER", "Picked up", "delivery-proofs/pickup.jpg"));
        });

        inTransaction(() -> {
            Parcel stored = entityManager.find(Parcel.class, parcelId);
            assertThat(stored.getStatus()).isEqualTo(ParcelStatus.PICKED_UP);
            assertThat(stored.getUpdatedAt()).isNotNull().isAfterOrEqualTo(stored.getCreatedAt());
            assertThat(stored.getStatusHistory())
                    .extracting(ParcelStatusHistory::getStatus)
                    .containsExactly(ParcelStatus.CREATED, ParcelStatus.PICKED_UP);
            assertThat(stored.getStatusHistory().getLast().getProofReference()).isEqualTo("delivery-proofs/pickup.jpg");
        });
    }

    @Test
    @DisplayName("The database refuses a FAILED parcel without a failure reason")
    void failedParcelNeedsReason() {
        UUID parcelId = persistCreatedParcel();

        assertThatThrownBy(() -> inTransaction(() -> {
            Parcel parcel = entityManager.find(Parcel.class, parcelId);
            parcel.setStatus(ParcelStatus.FAILED);
        })).rootCause().hasMessageContaining("chk_parcels_failure_reason");

        inTransaction(() -> {
            Parcel parcel = entityManager.find(Parcel.class, parcelId);
            parcel.setStatus(ParcelStatus.FAILED);
            parcel.setFailureReason("Recipient unavailable");
        });
        inTransaction(() -> assertThat(entityManager.find(Parcel.class, parcelId).getFailureReason())
                .isEqualTo("Recipient unavailable"));
    }

    private UUID persistCreatedParcel() {
        UUID parcelId = UUID.randomUUID();
        inTransaction(() -> {
            Parcel parcel = new Parcel(parcelId, trackingNo(), UUID.randomUUID(), null, "Pickup", "Dropoff",
                    "Recipient", "0771234567", ParcelSize.SMALL, BigDecimal.ONE, new BigDecimal("350.00"), "LKR");
            ParcelFixtures.withRouteAndFee(parcel);
            parcel.addStatusHistory(new ParcelStatusHistory(UUID.randomUUID(), parcel, ParcelStatus.CREATED,
                    parcel.getCustomerId(), "CUSTOMER", "Created", null));
            entityManager.persist(parcel);
        });
        return parcelId;
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private static String trackingNo() {
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder trackingNo = new StringBuilder("SD-");
        for (int i = 0; i < 9; i++) {
            trackingNo.append(i == 4 ? '-' : alphabet.charAt((int) (Math.random() * alphabet.length())));
        }
        return trackingNo.toString();
    }
}
