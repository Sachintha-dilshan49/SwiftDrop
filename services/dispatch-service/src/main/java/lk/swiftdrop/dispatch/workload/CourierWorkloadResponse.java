package lk.swiftdrop.dispatch.workload;

import java.util.UUID;
import lk.swiftdrop.dispatch.courier.CourierStatus;

/** One row of the dispatcher's workload table. IDs only: names and phone numbers live in auth-service. */
public record CourierWorkloadResponse(
        UUID courierId,
        UUID userId,
        CourierStatus status,
        long activeDeliveries) {
}
