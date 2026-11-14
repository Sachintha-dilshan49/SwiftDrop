package lk.swiftdrop.parcel.parcel.controller;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.InternalTokenTestContextCustomizerFactory;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.entity.ParcelStatusHistory;
import lk.swiftdrop.parcel.parcel.repository.ParcelRepository;
import lk.swiftdrop.parcel.parcel.repository.ParcelStatusHistoryRepository;
import lk.swiftdrop.parcel.support.AbstractContainersIT;
import lk.swiftdrop.parcel.support.ParcelFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract with dispatch-service: in {@code PATCH /internal/parcels/{id}/status}, {@code courierId} is the courier's
 * auth user ID (the JWT subject), never dispatch's own {@code couriers.id}. parcel-service stores {@code courierId}
 * at the first courier update and checks every later one against {@code changedBy}, so the two must be the same ID.
 * The dispatch side is checked in dispatch-service's {@code CourierStatusUpdateIT}.
 *
 * <p>In every test the courier's user ID and dispatch's courier row ID are different values, as in production.
 */
@AutoConfigureMockMvc
class CourierIdContractIT extends AbstractContainersIT {

    private static final String TOKEN_HEADER = InternalTokenAuthenticationFilter.HEADER;
    private static final String TOKEN = InternalTokenTestContextCustomizerFactory.TEST_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private ParcelStatusHistoryRepository historyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** The courier's auth-service user ID: the JWT subject dispatch sends as both courierId and changedBy. */
    private final UUID courierUserId = UUID.randomUUID();

    /** dispatch-service's couriers.id for the same courier. parcel-service must never be given this ID. */
    private final UUID dispatchCourierRowId = UUID.randomUUID();

    private UUID parcelId;

    @BeforeEach
    void createWaitingParcel() {
        assertThat(courierUserId).isNotEqualTo(dispatchCourierRowId);
        parcelId = UUID.randomUUID();
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Parcel parcel = new Parcel(parcelId, "SD-" + randomBlock() + "-" + randomBlock(), UUID.randomUUID(),
                    null, "Pickup", "Dropoff", "Recipient", "0771234567", ParcelSize.SMALL, BigDecimal.ONE,
                    new BigDecimal("350.00"), "LKR");
            ParcelFixtures.withRouteAndFee(parcel);
            parcel.addStatusHistory(new ParcelStatusHistory(UUID.randomUUID(), parcel, ParcelStatus.CREATED,
                    parcel.getCustomerId(), "CUSTOMER", "Created", null));
            parcelRepository.save(parcel);
        });
    }

    @Test
    @DisplayName("A courier identified by their user ID takes the parcel from pickup to DELIVERED")
    void courierWhoseCourierIdIsTheirUserIdCanDeliverTheParcel() throws Exception {
        courierUpdate(ParcelStatus.PICKED_UP, courierUserId, courierUserId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierUserId.toString()));
        courierUpdate(ParcelStatus.IN_TRANSIT, courierUserId, courierUserId).andExpect(status().isOk());
        courierUpdate(ParcelStatus.OUT_FOR_DELIVERY, courierUserId, courierUserId).andExpect(status().isOk());
        courierUpdate(ParcelStatus.DELIVERED, courierUserId, courierUserId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow();
        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.DELIVERED);
        assertThat(parcel.getCourierId()).isEqualTo(courierUserId).isNotEqualTo(dispatchCourierRowId);
        assertThat(historyRepository.findByParcelIdOrderByCreatedAtAsc(parcelId))
                .filteredOn(row -> row.getStatus() != ParcelStatus.CREATED)
                .extracting(ParcelStatusHistory::getChangedBy)
                .containsOnly(courierUserId);
    }

    @Test
    @DisplayName("If dispatch's couriers.id is sent as courierId, the courier is refused after pickup (the old bug)")
    void dispatchCourierRowIdAsCourierIdLocksTheCourierOutAfterPickup() throws Exception {
        // The first courier update has nothing to check against yet, so the wrong ID is stored...
        courierUpdate(ParcelStatus.PICKED_UP, dispatchCourierRowId, courierUserId).andExpect(status().isOk());

        // ...and every later update by the same courier no longer matches it
        courierUpdate(ParcelStatus.IN_TRANSIT, dispatchCourierRowId, courierUserId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/forbidden"));

        assertThat(parcelRepository.findById(parcelId).orElseThrow().getStatus()).isEqualTo(ParcelStatus.PICKED_UP);
    }

    @Test
    @DisplayName("Another courier cannot move a parcel that a courier has picked up")
    void anotherCourierCannotUpdateAParcelPickedUpBySomeoneElse() throws Exception {
        courierUpdate(ParcelStatus.PICKED_UP, courierUserId, courierUserId).andExpect(status().isOk());

        UUID otherCourierUserId = UUID.randomUUID();
        courierUpdate(ParcelStatus.IN_TRANSIT, otherCourierUserId, otherCourierUserId)
                .andExpect(status().isForbidden());

        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow();
        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.PICKED_UP);
        assertThat(parcel.getCourierId()).isEqualTo(courierUserId);
    }

    /** The same JSON body dispatch-service's {@code ParcelStatusUpdate.byCourier} sends. */
    private ResultActions courierUpdate(ParcelStatus newStatus, UUID courierId, UUID changedBy) throws Exception {
        String body = """
                {"status":"%s","courierId":"%s","note":null,"proofReference":null,"failureReason":null,\
                "changedBy":"%s","changedByRole":"COURIER"}""".formatted(newStatus, courierId, changedBy);
        return mockMvc.perform(patch("/internal/parcels/{id}/status", parcelId)
                .header(TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String randomBlock() {
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder block = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            block.append(alphabet.charAt((int) (Math.random() * alphabet.length())));
        }
        return block.toString();
    }
}
