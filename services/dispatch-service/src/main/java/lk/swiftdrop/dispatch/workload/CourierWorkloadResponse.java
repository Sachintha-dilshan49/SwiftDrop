package lk.swiftdrop.dispatch.workload;

import java.util.UUID;
import lk.swiftdrop.dispatch.courier.CourierStatus;

/**
 * One row of the dispatcher's workload table. {@code firstName} comes from auth-service and is null when it does not
 * know the user or cannot be reached; contact details are never included.
 */
public record CourierWorkloadResponse(
        UUID courierId,
        UUID userId,
        String firstName,
        CourierStatus status,
        long activeDeliveries) {

    static CourierWorkloadResponse from(CourierLoad load, String firstName) {
        return new CourierWorkloadResponse(load.courierId(), load.userId(), firstName, load.status(),
                load.activeDeliveries());
    }
}
