package lk.swiftdrop.dispatch.workload;

import java.util.UUID;
import lk.swiftdrop.dispatch.courier.CourierStatus;

/** A courier and their active delivery count, as read from dispatch_db. */
public record CourierLoad(
        UUID courierId,
        UUID userId,
        CourierStatus status,
        long activeDeliveries) {
}
