package lk.swiftdrop.common.event;

/**
 * Parcel lifecycle (docs/architecture.md §7). Only parcel-service decides which transitions are
 * valid; this enum only names the states.
 */
public enum ParcelStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED
}
