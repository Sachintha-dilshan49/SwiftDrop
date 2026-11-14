package lk.swiftdrop.ai.tools.dto;

import java.util.List;

/**
 * What {@code track_parcel} returns to the model: status and history only, already in customer
 * words (e.g. "Out for delivery", "14 Nov 2026, 11:45 am" in Sri Lanka time). Names, phone numbers,
 * addresses and courier notes are not copied from the parcel-service response.
 */
public record TrackingResult(
        String trackingNo,
        String status,
        String statusMeaning,
        String lastUpdated,
        List<HistoryEntry> history) {

    public record HistoryEntry(String status, String time) {
    }
}
