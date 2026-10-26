package lk.swiftdrop.dispatch.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import lk.swiftdrop.dispatch.courier.CourierStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final UUID PARCEL_ID = UUID.randomUUID();
    private static final UUID DISPATCHER_ID = UUID.randomUUID();

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CourierRepository courierRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void assignsParcelToAvailableCourierAndRecordsWhoAssignedIt() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(assignmentRepository.saveAndFlush(any(Assignment.class))).willAnswer(invocation -> invocation.getArgument(0));

        AssignmentResponse response = assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID);

        assertThat(response.parcelId()).isEqualTo(PARCEL_ID);
        assertThat(response.courierId()).isEqualTo(courier.getId());
        assertThat(response.assignedByUserId()).isEqualTo(DISPATCHER_ID);
        assertThat(response.status()).isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void busyCourierCanStillTakeMoreDeliveries() {
        Courier courier = courier(CourierStatus.BUSY);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(assignmentRepository.saveAndFlush(any(Assignment.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID).status())
                .isEqualTo(AssignmentStatus.ASSIGNED);
    }

    @Test
    void rejectsUnknownCourierWithNotFound() {
        UUID unknown = UUID.randomUUID();
        given(courierRepository.findById(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, unknown, DISPATCHER_ID))
                .isInstanceOf(NotFoundException.class);
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOfflineCourierWithConflict() {
        Courier courier = courier(CourierStatus.OFFLINE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("offline");
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsParcelThatAlreadyHasAnActiveAssignment() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(assignmentRepository.existsByParcelIdAndStatusIn(PARCEL_ID, AssignmentService.ACTIVE)).willReturn(true);

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active assignment");
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void concurrentAssignmentCaughtByUniqueIndexBecomesConflict() {
        Courier courier = courier(CourierStatus.AVAILABLE);
        given(courierRepository.findById(courier.getId())).willReturn(Optional.of(courier));
        given(assignmentRepository.saveAndFlush(any(Assignment.class)))
                .willThrow(new DataIntegrityViolationException("ux_assignments_active_parcel"));

        assertThatThrownBy(() -> assignmentService.assign(PARCEL_ID, courier.getId(), DISPATCHER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active assignment");
    }

    private static Courier courier(CourierStatus status) {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        courier.setStatus(status);
        return courier;
    }
}
