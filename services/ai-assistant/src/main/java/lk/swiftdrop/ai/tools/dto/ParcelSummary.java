package lk.swiftdrop.ai.tools.dto;

/**
 * One of the caller's parcels, as returned by {@code my_parcels}, in customer words. No recipient or
 * address data.
 */
public record ParcelSummary(String trackingNo, String status, String bookedOn) {
}
