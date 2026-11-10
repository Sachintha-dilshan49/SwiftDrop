package lk.swiftdrop.pricing.quote.model;

/** Route types derived from the pickup and drop-off zones (docs/architecture.md §8); parcel-service stores it. */
public enum RouteType {
    SAME_ZONE,
    NEIGHBOURING,
    COLOMBO_SUBURBS,
    INTER_CITY
}
