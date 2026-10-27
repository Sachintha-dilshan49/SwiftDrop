package lk.swiftdrop.dispatch.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.dispatch.assignment.Assignment;
import lk.swiftdrop.dispatch.assignment.AssignmentRepository;
import lk.swiftdrop.dispatch.assignment.AssignmentStatus;
import lk.swiftdrop.dispatch.config.ClockConfig;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.parcel.ParcelServiceClient;
import lk.swiftdrop.dispatch.parcel.ParcelStatusUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourierDeliveryServiceTest {

    // 08:30 on 26 Oct in Colombo, which is still 25 Oct in UTC
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T03:00:00Z"), ClockConfig.BUSINESS_ZONE);

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private ParcelServiceClient parcelServiceClient;

    private CourierDeliveryService service;

    private static final String TOKEN = "access-token";

    private final Courier me = courier();
    private final Courier other = courier();

    @BeforeEach
    void setUp() {
        service = new CourierDeliveryService(assignmentRepository, courierRepository, parcelServiceClient, CLOCK);
    }

    @Test
    void todaysListStartsAtMidnightColomboTime() {
        Assignment assignment = assignment(me);
        given(courierRepository.findByUserId(me.getUserId())).willReturn(Optional.of(me));
        given(assignmentRepository.findDayList(me.getId(), AssignmentStatus.ACTIVE,
                Instant.parse("2026-10-25T18:30:00Z"))).willReturn(List.of(assignment));

        List<CourierDeliveryResponse> deliveries = service.getTodaysDeliveries(me.getUserId());

        assertThat(deliveries).extracting(CourierDeliveryResponse::assignmentId).containsExactly(assignment.getId());
    }

    @Test
    void courierCanViewOwnDelivery() {
        Assignment assignment = assignment(me);
        given(courierRepository.findByUserId(me.getUserId())).willReturn(Optional.of(me));
        given(assignmentRepository.findById(assignment.getId())).willReturn(Optional.of(assignment));

        assertThat(service.getDelivery(me.getUserId(), assignment.getId()).parcelId())
                .isEqualTo(assignment.getParcelId());
    }

    @Test
    void courierCannotViewAnotherCouriersDelivery() {
        Assignment othersAssignment = assignment(other);
        given(courierRepository.findByUserId(me.getUserId())).willReturn(Optional.of(me));
        given(assignmentRepository.findById(othersAssignment.getId())).willReturn(Optional.of(othersAssignment));

        assertThatThrownBy(() -> service.getDelivery(me.getUserId(), othersAssignment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void unknownDeliveryIsNotFound() {
        UUID unknown = UUID.randomUUID();
        given(courierRepository.findByUserId(me.getUserId())).willReturn(Optional.of(me));
        given(assignmentRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDelivery(me.getUserId(), unknown))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void userWithoutCourierProfileIsNotFound() {
        UUID stranger = UUID.randomUUID();
        given(courierRepository.findByUserId(stranger)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTodaysDeliveries(stranger))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No courier profile");
    }

    @Test
    void deliveredParcelCompletesTheAssignment() {
        Assignment assignment = ownAssignment();
        given(assignmentRepository.save(assignment)).willReturn(assignment);

        CourierDeliveryResponse response = service.updateStatus(me.getUserId(), TOKEN, assignment.getId(),
                new UpdateDeliveryStatusRequest(ParcelStatus.DELIVERED, "Left at the door", "Nimali", null));

        assertThat(response.status()).isEqualTo(AssignmentStatus.COMPLETED);
        verify(parcelServiceClient).updateStatus(assignment.getParcelId(), new ParcelStatusUpdate(
                ParcelStatus.DELIVERED, "Left at the door", "Nimali", null, me.getId(), null), TOKEN);
    }

    @Test
    void pickedUpParcelPutsTheAssignmentInProgress() {
        Assignment assignment = ownAssignment();
        given(assignmentRepository.save(assignment)).willReturn(assignment);

        CourierDeliveryResponse response = service.updateStatus(me.getUserId(), TOKEN, assignment.getId(),
                new UpdateDeliveryStatusRequest(ParcelStatus.PICKED_UP, null, null, null));

        assertThat(response.status()).isEqualTo(AssignmentStatus.IN_PROGRESS);
    }

    @Test
    void failedDeliveryWithReasonFailsTheAssignment() {
        Assignment assignment = ownAssignment();
        given(assignmentRepository.save(assignment)).willReturn(assignment);

        CourierDeliveryResponse response = service.updateStatus(me.getUserId(), TOKEN, assignment.getId(),
                new UpdateDeliveryStatusRequest(ParcelStatus.FAILED, null, null, "Nobody at home"));

        assertThat(response.status()).isEqualTo(AssignmentStatus.FAILED);
    }

    @Test
    void failedStatusWithoutReasonIsRejected() {
        UUID assignmentId = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateStatus(me.getUserId(), TOKEN, assignmentId,
                new UpdateDeliveryStatusRequest(ParcelStatus.FAILED, "note", null, "  ")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("needs a reason");
        verifyNoInteractions(parcelServiceClient, assignmentRepository);
    }

    @Test
    void courierCannotUpdateAnotherCouriersDelivery() {
        Assignment othersAssignment = assignment(other);
        given(courierRepository.findByUserId(me.getUserId())).willReturn(Optional.of(me));
        given(assignmentRepository.findById(othersAssignment.getId())).willReturn(Optional.of(othersAssignment));

        assertThatThrownBy(() -> service.updateStatus(me.getUserId(), TOKEN, othersAssignment.getId(),
                new UpdateDeliveryStatusRequest(ParcelStatus.PICKED_UP, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(parcelServiceClient);
    }

    @Test
    void closedDeliveryCannotBeUpdatedAgain() {
        Assignment assignment = ownAssignment();
        assignment.setStatus(AssignmentStatus.COMPLETED);

        assertThatThrownBy(() -> service.updateStatus(me.getUserId(), TOKEN, assignment.getId(),
                new UpdateDeliveryStatusRequest(ParcelStatus.DELIVERED, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already COMPLETED");
        verifyNoInteractions(parcelServiceClient);
    }

    @Test
    void assignmentIsUnchangedWhenParcelServiceRejectsTheMove() {
        Assignment assignment = ownAssignment();
        willThrow(new ConflictException("invalid-status-transition", "Invalid", "no"))
                .given(parcelServiceClient).updateStatus(eq(assignment.getParcelId()), any(), eq(TOKEN));

        assertThatThrownBy(() -> service.updateStatus(me.getUserId(), TOKEN, assignment.getId(),
                new UpdateDeliveryStatusRequest(ParcelStatus.DELIVERED, null, null, null)))
                .isInstanceOf(ConflictException.class);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        verify(assignmentRepository, never()).save(any());
    }

    private Assignment ownAssignment() {
        Assignment assignment = assignment(me);
        given(courierRepository.findByUserId(me.getUserId())).willReturn(Optional.of(me));
        given(assignmentRepository.findById(assignment.getId())).willReturn(Optional.of(assignment));
        return assignment;
    }

    private static Courier courier() {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        return courier;
    }

    private static Assignment assignment(Courier courier) {
        Assignment assignment = new Assignment();
        assignment.setParcelId(UUID.randomUUID());
        assignment.setCourierId(courier.getId());
        assignment.setAssignedByUserId(UUID.randomUUID());
        return assignment;
    }
}
