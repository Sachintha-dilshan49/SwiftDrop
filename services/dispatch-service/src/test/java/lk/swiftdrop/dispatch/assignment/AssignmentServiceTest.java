package lk.swiftdrop.dispatch.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import lk.swiftdrop.dispatch.parcel.AssignableParcel;
import lk.swiftdrop.dispatch.parcel.ParcelLookupClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final UUID PARCEL_ID = UUID.randomUUID();
    private static final UUID DISPATCHER_ID = UUID.randomUUID();
    private static final String TOKEN = "dispatcher-token";

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private ParcelLookupClient parcelLookupClient;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void assignsParcelToAvailableCourierAndRecordsWhoAssignedIt() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(parcelLookupClient.getParcel(PARCEL_ID, TOKEN)).willReturn(parcel(ParcelStatus.CREATED));
        given(assignmentRepository.saveAndFlush(any(Assignment.class))).willAnswer(invocation -> invocation.getArgument(0));

        AssignmentResponse response = assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN);

        assertThat(response.parcelId()).isEqualTo(PARCEL_ID);
        assertThat(response.trackingNo()).isEqualTo("SD-0001");
        assertThat(response.courierId()).isEqualTo(courier.getId());
        assertThat(response.assignedByUserId()).isEqualTo(DISPATCHER_ID);
        assertThat(response.status()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void copiesWhatTheCourierNeedsOntoTheAssignment() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(parcelLookupClient.getParcel(PARCEL_ID, TOKEN)).willReturn(parcel(ParcelStatus.CREATED));
        given(assignmentRepository.saveAndFlush(any(Assignment.class))).willAnswer(invocation -> invocation.getArgument(0));

        assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN);

        ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getParcelDetails()).isEqualTo(new ParcelDetails(
                "SD-0001", "Liberty Plaza", "Rajagiriya Flyover", "Nimal", "SMALL", new BigDecimal("1.20")));
    }

    @Test
    void busyCourierCanStillTakeMoreDeliveries() {
        Courier courier = courier(CourierStatus.BUSY);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(parcelLookupClient.getParcel(PARCEL_ID, TOKEN)).willReturn(parcel(ParcelStatus.CREATED));
        given(assignmentRepository.saveAndFlush(any(Assignment.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN).status())
                .isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void rejectsUnknownCourierWithNotFound() {
        UUID unknown = UUID.randomUUID();
        given(courierRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, unknown, DISPATCHER_ID, TOKEN))
                .isInstanceOf(NotFoundException.class);
        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(parcelLookupClient);
    }

    @Test
    void rejectsOfflineCourierWithConflict() {
        Courier courier = courier(CourierStatus.OFFLINE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("offline");
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsParcelThatAlreadyHasAnActiveAssignment() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(assignmentRepository.existsByParcelIdAndStatusIn(PARCEL_ID, AssignmentStatus.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active assignment");
        verify(assignmentRepository, never()).saveAndFlush(any());
        verifyNoInteractions(parcelLookupClient);
    }

    @Test
    void rejectsParcelThatIsNoLongerWaitingForACourier() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(parcelLookupClient.getParcel(PARCEL_ID, TOKEN)).willReturn(parcel(ParcelStatus.PICKED_UP));

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only CREATED parcels can be assigned");
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownParcelIsNotFound() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(parcelLookupClient.getParcel(PARCEL_ID, TOKEN))
                .willThrow(new NotFoundException("parcel-not-found", "Parcel not found", "Parcel does not exist."));

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN))
                .isInstanceOf(NotFoundException.class);
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentAssignmentCaughtByUniqueIndexBecomesConflict() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(parcelLookupClient.getParcel(PARCEL_ID, TOKEN)).willReturn(parcel(ParcelStatus.CREATED));
        given(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .willThrow(new DataIntegrityViolationException("ux_assignments_active_parcel"));

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID, TOKEN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active assignment");
    }

    private static Courier courier(CourierStatus status) {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier.setStatus(status);
        return courier;
    }

    private static AssignableParcel parcel(ParcelStatus status) {
        return new AssignableParcel(PARCEL_ID, "SD-0001", status, "Liberty Plaza", "Rajagiriya Flyover", "Nimal",
                "SMALL", new BigDecimal("1.20"));
    }
}
