package lk.swiftdrop.ai.tools.dto;

import java.util.List;

/**
 * What {@code track_parcel} returns to the model: status and history only. Names, phone numbers,
 * addresses and courier notes are not copied from the parcel-service response.
 */
public record TrackingResult(
        String trackingNo,
        String status,
        String lastUpdatedAt,
        List<HistoryEntry> history) {

    public record HistoryEntry(String status, String at) {
    }
}
