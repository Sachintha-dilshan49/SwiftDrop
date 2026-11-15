package lk.swiftdrop.dispatch.unassigned;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.parcel.ParcelLookupClient;
import lk.swiftdrop.dispatch.parcel.WaitingParcel;
import org.junit.jupiter.api.Test;

class UnassignedParcelServiceTest {

    private static final String TOKEN = "dispatcher-token";

    private final ParcelLookupClient parcelLookupClient = mock(ParcelLookupClient.class);
    private final AssignmentRepository assignmentRepository = mock(AssignmentRepository.class);
    private final UnassignedParcelService service =
            new UnassignedParcelService(parcelLookupClient, assignmentRepository);

    @Test
    void leavesOutParcelsThatAlreadyHaveAnActiveAssignment() {
        WaitingParcel waiting = parcel("SD-0001");
        WaitingParcel assigned = parcel("SD-0002");
        WaitingParcel alsoWaiting = parcel("SD-0003");
        List<UUID> ids = List.of(waiting.id(), assigned.id(), alsoWaiting.id());
        given(parcelLookupClient.oldestCreatedParcels(TOKEN)).willReturn(List.of(waiting, assigned, alsoWaiting));
        given(assignmentRepository.findParcelIdsWithStatus(ids, AssignmentStatus.ACTIVE))
                .willReturn(Set.of(assigned.id()));

        List<UnassignedParcelResponse> result = service.getUnassignedParcels(TOKEN);

        assertThat(result).extracting(UnassignedParcelResponse::trackingNo).containsExactly("SD-0001", "SD-0003");
        assertThat(result.get(0).parcelId()).isEqualTo(waiting.id());
        assertThat(result.get(0).pickupAddress()).isEqualTo("Colombo 03");
    }

    @Test
    void noCreatedParcelsMeansNothingToLookUp() {
        given(parcelLookupClient.oldestCreatedParcels(TOKEN)).willReturn(List.of());

        assertThat(service.getUnassignedParcels(TOKEN)).isEmpty();
        verifyNoInteractions(assignmentRepository);
    }

    private static WaitingParcel parcel(String trackingNo) {
        return new WaitingParcel(UUID.randomUUID(), trackingNo, "Colombo 03", "Kandy", "SMALL",
                new BigDecimal("1.50"), Instant.now());
    }
}
