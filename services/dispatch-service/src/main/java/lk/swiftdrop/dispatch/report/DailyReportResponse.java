package lk.swiftdrop.dispatch.report;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * {@code GET /api/v1/reports/daily}: deliveries per Sri Lanka day, totals, and each courier's share.
 *
 * @param days     every day from {@code from} to {@code to}, oldest first, including days with nothing
 * @param couriers couriers with any activity in the range, most deliveries first
 */
public record DailyReportResponse(LocalDate from, LocalDate to, List<Day> days, Counts totals, List<CourierCounts> couriers) {

    public record Day(LocalDate date, long assigned, long delivered, long failed) {
    }

    public record Counts(long assigned, long delivered, long failed) {
    }

    /** @param firstName {@code null} when auth-service could not be asked */
    public record CourierCounts(UUID courierId, String firstName, long assigned, long delivered, long failed) {
    }
}
