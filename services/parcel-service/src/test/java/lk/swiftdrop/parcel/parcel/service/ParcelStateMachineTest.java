package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelStateMachineTest {

    private ParcelStateMachine stateMachine;
    private Parcel parcel;
    private final UUID courierId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stateMachine = new ParcelStateMachine();
        parcel = new Parcel(
                UUID.randomUUID(),
                "SD-7K3Q-9P2M",
                UUID.randomUUID(),
                null,
                "123 Main St, Colombo",
                "456 High St, Kandy",
                "Nimal Perera",
                "+94771234567",
                ParcelSize.MEDIUM,
                new BigDecimal("2.50"),
                new BigDecimal("750.00"),
                "LKR"
        );
        parcel.setCourierId(courierId);
    }

    @Test
    @DisplayName("Allows valid full lifecycle CREATED -> PICKED_UP -> IN_TRANSIT -> OUT_FOR_DELIVERY -> DELIVERED")
    void allowsValidDeliveryLifecycle() {
        parcel.setStatus(ParcelStatus.CREATED);
        assertThatCode(() -> stateMachine.validateTransition(parcel, ParcelStatus.PICKED_UP, null))
                .doesNotThrowAnyException();

        parcel.setStatus(ParcelStatus.PICKED_UP);
        assertThatCode(() -> stateMachine.validateTransition(parcel, ParcelStatus.IN_TRANSIT, null))
                .doesNotThrowAnyException();

        parcel.setStatus(ParcelStatus.IN_TRANSIT);
        assertThatCode(() -> stateMachine.validateTransition(parcel, ParcelStatus.OUT_FOR_DELIVERY, null))
                .doesNotThrowAnyException();

        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        assertThatCode(() -> stateMachine.validateTransition(parcel, ParcelStatus.DELIVERED, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Allows OUT_FOR_DELIVERY -> FAILED when failure reason is provided")
    void allowsFailedTransitionWithReason() {
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        assertThatCode(() -> stateMachine.validateTransition(parcel, ParcelStatus.FAILED, "Recipient unavailable"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects OUT_FOR_DELIVERY -> FAILED with 400 when failure reason is missing or blank")
    void rejectsFailedTransitionWithoutReason() {
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);

        assertThatThrownBy(() -> stateMachine.validateTransition(parcel, ParcelStatus.FAILED, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("A failure reason is required")
                .satisfies(e -> assertThat(((ValidationException) e).getErrors())
                        .extracting("field").containsExactly("failureReason"));

        assertThatThrownBy(() -> stateMachine.validateTransition(parcel, ParcelStatus.FAILED, "   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("A failure reason is required");
    }

    @Test
    @DisplayName("Rejects a failure reason with 400 when the target status is not FAILED")
    void rejectsFailureReasonForOtherStatuses() {
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);

        assertThatThrownBy(() -> stateMachine.validateTransition(parcel, ParcelStatus.DELIVERED, "Recipient unavailable"))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getErrors())
                        .extracting("field").containsExactly("failureReason"));
    }

    @Test
    @DisplayName("An invalid transition is reported as 409 before the failure reason is checked")
    void invalidTransitionTakesPrecedenceOverMissingReason() {
        parcel.setStatus(ParcelStatus.CREATED);

        assertThatThrownBy(() -> stateMachine.validateTransition(parcel, ParcelStatus.FAILED, null))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest(name = "Caller role \"{0}\" is forbidden")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void callerWithoutRoleIsForbidden(String role) {
        assertThatThrownBy(() -> stateMachine.validateAuthorization(parcel, courierId, role))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("caller role is required");
    }

    @ParameterizedTest(name = "Rejects invalid transition {0} -> {1}")
    @CsvSource({
            "CREATED, DELIVERED",
            "CREATED, IN_TRANSIT",
            "CREATED, OUT_FOR_DELIVERY",
            "CREATED, FAILED",
            "PICKED_UP, CREATED",
            "PICKED_UP, DELIVERED",
            "PICKED_UP, FAILED",
            "IN_TRANSIT, CREATED",
            "IN_TRANSIT, PICKED_UP",
            "IN_TRANSIT, DELIVERED",
            "IN_TRANSIT, FAILED",
            "DELIVERED, CREATED",
            "DELIVERED, IN_TRANSIT",
            "DELIVERED, FAILED",
            "FAILED, CREATED",
            "FAILED, DELIVERED"
    })
    void rejectsInvalidTransitions(ParcelStatus from, ParcelStatus to) {
        parcel.setStatus(from);
        assertThatThrownBy(() -> stateMachine.validateTransition(parcel, to, "Some reason"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Parcel cannot move from " + from + " to " + to);
    }

    @Test
    @DisplayName("Assigned courier is authorized to update parcel")
    void assignedCourierIsAuthorized() {
        assertThatCode(() -> stateMachine.validateAuthorization(parcel, courierId, "ROLE_COURIER"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Unassigned courier is forbidden from updating another courier's parcel")
    void unassignedCourierIsForbidden() {
        UUID anotherCourier = UUID.randomUUID();
        assertThatThrownBy(() -> stateMachine.validateAuthorization(parcel, anotherCourier, "ROLE_COURIER"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Courier is not assigned");
    }

    @Test
    @DisplayName("A courier without a caller ID is forbidden, so the assignment check cannot be skipped")
    void courierWithoutCallerIdIsForbidden() {
        assertThatThrownBy(() -> stateMachine.validateAuthorization(parcel, null, "ROLE_COURIER"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("must be identified");
    }

    @Test
    @DisplayName("Dispatcher and Admin are always authorized to update status")
    void dispatcherAndAdminAreAlwaysAuthorized() {
        assertThatCode(() -> stateMachine.validateAuthorization(parcel, UUID.randomUUID(), "ROLE_DISPATCHER"))
                .doesNotThrowAnyException();

        assertThatCode(() -> stateMachine.validateAuthorization(parcel, UUID.randomUUID(), "ROLE_ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Customer is forbidden from updating parcel status")
    void customerIsForbiddenFromUpdatingStatus() {
        assertThatThrownBy(() -> stateMachine.validateAuthorization(parcel, parcel.getCustomerId(), "ROLE_CUSTOMER"))
                .isInstanceOf(ForbiddenException.class);
    }
}
