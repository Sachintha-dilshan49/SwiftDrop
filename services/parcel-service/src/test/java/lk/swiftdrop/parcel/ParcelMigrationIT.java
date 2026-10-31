package lk.swiftdrop.parcel;

import lk.swiftdrop.parcel.support.SharedContainers;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V5 and V6 against a database that already has data. Each test gets a new, empty database in
 * the shared test container (never a developer's local database).
 */
class ParcelMigrationIT {

    private static final PostgreSQLContainer POSTGRES = SharedContainers.POSTGRES;

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void createEmptyDatabase() {
        String database = "migration_it_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(dataSource(POSTGRES.getJdbcUrl())).execute("CREATE DATABASE " + database);
        dataSource = dataSource(POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database));
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("V5 sorts existing outbox rows so no event can be published after a newer sent event")
    void backfillsRetryStateWithoutBreakingPerParcelOrder() {
        migrateTo("4");

        // A failed event with nothing after it: the newest state was never delivered, so it is retried
        UUID onlyFailed = UUID.randomUUID();
        UUID onlyFailedEvent = insertOutbox(onlyFailed, "FAILED", "now() - interval '5 minutes'");

        // A failed event followed by a sent one: publishing it now would roll the status back
        UUID failedThenSent = UUID.randomUUID();
        UUID supersededFailed = insertOutbox(failedThenSent, "FAILED", "now() - interval '5 minutes'");
        UUID laterSent = insertOutbox(failedThenSent, "SENT", "now() - interval '4 minutes'");

        // The previous relay kept going past failures, so a still-pending event can be older than a sent one
        UUID pendingSentPending = UUID.randomUUID();
        UUID supersededPending = insertOutbox(pendingSentPending, "PENDING", "now() - interval '5 minutes'");
        insertOutbox(pendingSentPending, "SENT", "now() - interval '4 minutes'");
        UUID newestPending = insertOutbox(pendingSentPending, "PENDING", "now() - interval '3 minutes'");

        // A failed event followed only by pending events: all are published, oldest first
        UUID failedThenPending = UUID.randomUUID();
        UUID firstFailed = insertOutbox(failedThenPending, "FAILED", "now() - interval '5 minutes'");
        UUID secondPending = insertOutbox(failedThenPending, "PENDING", "now() - interval '4 minutes'");

        // failed, sent, failed: only the event after the last sent one is retried
        UUID failedSentFailed = UUID.randomUUID();
        UUID oldFailed = insertOutbox(failedSentFailed, "FAILED", "now() - interval '5 minutes'");
        insertOutbox(failedSentFailed, "SENT", "now() - interval '4 minutes'");
        UUID newFailed = insertOutbox(failedSentFailed, "FAILED", "now() - interval '3 minutes'");

        migrateTo("5");

        assertThat(statusOf(onlyFailedEvent)).isEqualTo("PENDING");

        assertThat(statusOf(supersededFailed)).isEqualTo("PARKED");
        assertThat(lastErrorOf(supersededFailed)).startsWith("Superseded before V5");
        assertThat(statusOf(laterSent)).isEqualTo("SENT");

        assertThat(statusOf(supersededPending)).isEqualTo("PARKED");
        assertThat(statusOf(newestPending)).isEqualTo("PENDING");

        assertThat(statusOf(firstFailed)).isEqualTo("PENDING");
        assertThat(statusOf(secondPending)).isEqualTo("PENDING");
        assertThat(seqOf(firstFailed)).isLessThan(seqOf(secondPending));

        assertThat(statusOf(oldFailed)).isEqualTo("PARKED");
        assertThat(statusOf(newFailed)).isEqualTo("PENDING");

        // No unsent event is left behind a sent event of its parcel
        Integer outOfOrder = jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events o
                WHERE o.status = 'PENDING'
                  AND EXISTS (SELECT 1 FROM outbox_events later
                              WHERE later.aggregate_id = o.aggregate_id
                                AND later.status = 'SENT' AND later.seq > o.seq)
                """, Integer.class);
        assertThat(outOfOrder).isZero();
    }

    @Test
    @DisplayName("V5 backfills aggregate_id and seq from the stored events in creation order")
    void backfillsOrderingColumns() {
        migrateTo("4");
        UUID parcelId = UUID.randomUUID();
        UUID oldest = insertOutbox(parcelId, "SENT", "now() - interval '2 minutes'");
        UUID middle = insertOutbox(parcelId, "SENT", "now() - interval '1 minute'");
        UUID newest = insertOutbox(parcelId, "PENDING", "now()");

        migrateTo("5");

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, aggregate_id, next_attempt_at FROM outbox_events ORDER BY seq");
        assertThat(rows).extracting(row -> row.get("id")).containsExactly(oldest, middle, newest);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("aggregate_id")).isEqualTo(parcelId);
            assertThat(row.get("next_attempt_at")).isNotNull();
        });

        // New rows continue the sequence after the backfilled ones
        UUID added = insertOutbox(parcelId, "PENDING", "now()");
        assertThat(seqOf(added)).isGreaterThan(seqOf(newest));
    }

    @Test
    @DisplayName("V5 constraints reject invalid parcels and outbox rows")
    void rejectsInvalidRows() {
        migrateTo("5");
        UUID parcelId = UUID.randomUUID();
        insertParcel(parcelId, "SD-7K3Q-9P2M", "CREATED", null);

        assertRejected(() -> insertParcel(UUID.randomUUID(), "SD-2222-3333", "LOST", null));
        assertRejected(() -> insertParcel(UUID.randomUUID(), "SD-IOIO-0101", "CREATED", null));
        assertRejected(() -> insertParcel(UUID.randomUUID(), "SD-2222-4444", "FAILED", null));
        assertRejected(() -> jdbc.update("""
                INSERT INTO parcels (id, tracking_no, pickup_address, dropoff_address, recipient_name,
                                     recipient_phone, size, weight_kg, fee_amount)
                VALUES (?, 'SD-2222-5555', 'A', 'B', 'C', '0771234567', 'SMALL', 1.00, 350.00)
                """, UUID.randomUUID()));
        assertRejected(() -> jdbc.update("UPDATE parcels SET fee_amount = -1 WHERE id = ?", parcelId));
        assertRejected(() -> jdbc.update("""
                INSERT INTO parcel_status_history (id, parcel_id, status) VALUES (?, ?, 'LOST')
                """, UUID.randomUUID(), parcelId));
        assertRejected(() -> insertOutbox(parcelId, "FAILED", "now()"));

        // A failed parcel with a reason is fine
        insertParcel(UUID.randomUUID(), "SD-2222-6666", "FAILED", "Recipient unavailable");
    }

    @Test
    @DisplayName("V6 gives existing parcels a route and a fee breakdown that adds up to their fee")
    void backfillsRouteAndFeeBreakdown() {
        migrateTo("5");
        UUID parcelId = UUID.randomUUID();
        insertParcel(parcelId, "SD-7K3Q-9P2M", "CREATED", null);

        migrateTo("6");

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT pickup_zone, dropoff_zone, speed, route_type, base_fee, weight_fee, size_fee,
                       express_fee, discount, fee_amount
                FROM parcels WHERE id = ?
                """, parcelId);
        assertThat(row)
                .containsEntry("pickup_zone", "UNKNOWN")
                .containsEntry("dropoff_zone", "UNKNOWN")
                .containsEntry("speed", "STANDARD")
                .containsEntry("route_type", "SAME_ZONE");
        assertThat((BigDecimal) row.get("base_fee")).isEqualByComparingTo((BigDecimal) row.get("fee_amount"));
        assertThat(List.of("weight_fee", "size_fee", "express_fee", "discount"))
                .allSatisfy(column -> assertThat((BigDecimal) row.get(column)).isZero());
    }

    @Test
    @DisplayName("V6 accepts EXTRA_LARGE and rejects unknown sizes, speeds and routes, and merchant orders without an email")
    void rejectsInvalidQuotedParcels() {
        migrateTo("6");
        insertQuotedParcel("SD-2222-3333", "EXTRA_LARGE", "EXPRESS", "INTER_CITY", null, null);
        insertQuotedParcel("SD-2222-4444", "SMALL", "NEXT_DAY", "SAME_ZONE", UUID.randomUUID(), "shop@example.com");

        assertRejected(() -> insertQuotedParcel("SD-2222-5555", "HUGE", "STANDARD", "SAME_ZONE", null, null));
        assertRejected(() -> insertQuotedParcel("SD-2222-6666", "SMALL", "TOMORROW", "SAME_ZONE", null, null));
        assertRejected(() -> insertQuotedParcel("SD-2222-7777", "SMALL", "STANDARD", "OVERSEAS", null, null));
        assertRejected(() -> insertQuotedParcel("SD-2222-8888", "SMALL", "STANDARD", "SAME_ZONE",
                UUID.randomUUID(), null));
        // A parcel without a route and a fee breakdown
        assertRejected(() -> insertParcel(UUID.randomUUID(), "SD-2222-9999", "CREATED", null));
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private void insertParcel(UUID id, String trackingNo, String status, String failureReason) {
        jdbc.update("""
                INSERT INTO parcels (id, tracking_no, customer_id, pickup_address, dropoff_address, recipient_name,
                                     recipient_phone, size, weight_kg, fee_amount, status, failure_reason)
                VALUES (?, ?, ?, 'Pickup', 'Dropoff', 'Recipient', '0771234567', 'SMALL', 1.00, 350.00, ?, ?)
                """, id, trackingNo, UUID.randomUUID(), status, failureReason);
    }

    /** Inserts a parcel in the V6 shape: a route, a speed and a fee breakdown that adds up to fee_amount. */
    private void insertQuotedParcel(String trackingNo, String size, String speed, String routeType,
                                    UUID merchantId, String recipientEmail) {
        jdbc.update("""
                INSERT INTO parcels (id, tracking_no, customer_id, merchant_id, pickup_address, pickup_zone,
                                     dropoff_address, dropoff_zone, recipient_name, recipient_phone, recipient_email,
                                     size, weight_kg, speed, route_type, base_fee, weight_fee, size_fee,
                                     express_fee, discount, fee_amount)
                VALUES (?, ?, ?, ?, 'Pickup', 'COLOMBO_SOUTH', 'Dropoff', 'COLOMBO_CENTRAL', 'Recipient',
                        '0771234567', ?, ?, 1.00, ?, ?, 350.00, 0, 0, 0, 0, 350.00)
                """, UUID.randomUUID(), trackingNo, merchantId == null ? UUID.randomUUID() : null, merchantId,
                recipientEmail, size, speed, routeType);
    }

    /** Inserts an outbox row in the shape of the current schema (before or after V5). */
    private UUID insertOutbox(UUID parcelId, String status, String createdAtSql) {
        UUID id = UUID.randomUUID();
        String payload = "{\"parcelId\":\"" + parcelId + "\",\"type\":\"ParcelStatusChanged\"}";
        boolean v5 = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'outbox_events' AND column_name = 'aggregate_id'
                """, Integer.class) > 0;
        if (v5) {
            jdbc.update("INSERT INTO outbox_events (id, event_id, aggregate_id, event_type, routing_key, payload, status, created_at) "
                    + "VALUES (?, ?, ?, 'ParcelStatusChanged', 'parcel.status.created', ?, ?, " + createdAtSql + ")",
                    id, UUID.randomUUID(), parcelId, payload, status);
        } else {
            jdbc.update("INSERT INTO outbox_events (id, event_id, event_type, routing_key, payload, status, created_at) "
                    + "VALUES (?, ?, 'ParcelStatusChanged', 'parcel.status.created', ?, ?, " + createdAtSql + ")",
                    id, UUID.randomUUID(), payload, status);
        }
        return id;
    }

    private String statusOf(UUID outboxId) {
        return jdbc.queryForObject("SELECT status FROM outbox_events WHERE id = ?", String.class, outboxId);
    }

    private String lastErrorOf(UUID outboxId) {
        return jdbc.queryForObject("SELECT last_error FROM outbox_events WHERE id = ?", String.class, outboxId);
    }

    private long seqOf(UUID outboxId) {
        return jdbc.queryForObject("SELECT seq FROM outbox_events WHERE id = ?", Long.class, outboxId);
    }

    private static void assertRejected(Runnable statement) {
        assertThatThrownBy(statement::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static DriverManagerDataSource dataSource(String url) {
        return new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
