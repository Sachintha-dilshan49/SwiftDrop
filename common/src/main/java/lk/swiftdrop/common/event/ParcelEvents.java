package lk.swiftdrop.common.event;

import java.util.Locale;

/** RabbitMQ names for parcel events (docs/architecture.md §6). */
public final class ParcelEvents {

    /** Durable topic exchange that parcel-service publishes to. */
    public static final String EXCHANGE = "parcel.events";

    public static final String ROUTING_KEY_PREFIX = "parcel.status.";

    /** Binding pattern for consumers that want every status change. */
    public static final String ALL_STATUS_CHANGES = ROUTING_KEY_PREFIX + "#";

    private ParcelEvents() {
    }

    /** {@code OUT_FOR_DELIVERY} becomes {@code parcel.status.out_for_delivery}. */
    public static String routingKey(ParcelStatus status) {
        return ROUTING_KEY_PREFIX + status.name().toLowerCase(Locale.ROOT);
    }
}
