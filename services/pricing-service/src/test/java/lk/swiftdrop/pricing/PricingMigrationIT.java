package lk.swiftdrop.pricing;

import lk.swiftdrop.pricing.support.SharedPostgres;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * The migrations against a fresh database: each test gets a new, empty database in the shared test
 * container (never a developer's local database) and applies every migration to it.
 */
class PricingMigrationIT {

    private static final PostgreSQLContainer POSTGRES = SharedPostgres.POSTGRES;

    /** frontend/src/features/booking/bookingOptions.ts lists exactly these codes. */
    private static final List<String> FRONTEND_ZONE_CODES = List.of(
            "COLOMBO_CENTRAL", "COLOMBO_SOUTH", "COLOMBO_NORTH",
            "KOTTE_RAJAGIRIYA", "NUGEGODA_MAHARAGAMA", "DEHIWALA_MT_LAVINIA",
            "KANDY", "GALLE", "NEGOMBO");

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateEmptyDatabase() {
        String database = "pricing_it_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(dataSource(POSTGRES.getJdbcUrl())).execute("CREATE DATABASE " + database);
        DriverManagerDataSource dataSource =
                dataSource(POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database));
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("Seeds the nine zones with the codes the booking form uses, each in its group and with districts")
    void seedsZonesMatchingTheFrontend() {
        Map<String, String> groups = jdbc.queryForList("SELECT code, zone_group FROM zones WHERE active").stream()
                .collect(Collectors.toMap(row -> (String) row.get("code"), row -> (String) row.get("zone_group")));

        assertThat(groups.keySet()).containsExactlyInAnyOrderElementsOf(FRONTEND_ZONE_CODES);
        assertThat(groups).contains(
                entry("COLOMBO_CENTRAL", "COLOMBO"), entry("COLOMBO_SOUTH", "COLOMBO"), entry("COLOMBO_NORTH", "COLOMBO"),
                entry("KOTTE_RAJAGIRIYA", "SUBURBS"), entry("NUGEGODA_MAHARAGAMA", "SUBURBS"),
                entry("DEHIWALA_MT_LAVINIA", "SUBURBS"),
                entry("KANDY", "INTER_CITY"), entry("GALLE", "INTER_CITY"), entry("NEGOMBO", "INTER_CITY"));

        Integer zonesWithoutDistricts = jdbc.queryForObject("""
                SELECT count(*) FROM zones z
                WHERE NOT EXISTS (SELECT 1 FROM zone_districts d WHERE d.zone_id = z.id)
                """, Integer.class);
        assertThat(zonesWithoutDistricts).isZero();
        assertThat(jdbc.queryForList("""
                SELECT d.district_name FROM zone_districts d JOIN zones z ON z.id = d.zone_id
                WHERE z.code = 'COLOMBO_SOUTH' ORDER BY d.district_name
                """, String.class)).containsExactly("Colombo 04", "Colombo 05", "Colombo 06");
    }

    @Test
    @DisplayName("Seeds every amount from architecture §8, with a maximum weight on each size")
    void seedsEveryPricingRule() {
        Map<String, BigDecimal> amounts = jdbc.queryForList(
                        "SELECT rule_type, rule_key, amount FROM pricing_rules WHERE active").stream()
                .collect(Collectors.toMap(row -> row.get("rule_type") + ":" + row.get("rule_key"),
                        row -> (BigDecimal) row.get("amount")));

        assertThat(amounts).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                entry("ROUTE_BASE:INTER_CITY", new BigDecimal("650.00")),
                entry("ROUTE_BASE:SAME_ZONE", new BigDecimal("300.00")),
                entry("ROUTE_BASE:NEIGHBOURING", new BigDecimal("350.00")),
                entry("ROUTE_BASE:COLOMBO_SUBURBS", new BigDecimal("400.00")),
                entry("EXTRA_KG:PER_KG", new BigDecimal("40.00")),
                entry("SIZE:SMALL", new BigDecimal("0.00")),
                entry("SIZE:MEDIUM", new BigDecimal("100.00")),
                entry("SIZE:LARGE", new BigDecimal("250.00")),
                entry("SIZE:EXTRA_LARGE", new BigDecimal("500.00")),
                entry("SPEED:EXPRESS", new BigDecimal("100.00")),
                entry("SPEED:STANDARD", new BigDecimal("0.00")),
                entry("SPEED:NEXT_DAY", new BigDecimal("0.00"))));

        Map<String, BigDecimal> maxWeights = jdbc.queryForList(
                        "SELECT rule_key, max_weight_kg FROM pricing_rules WHERE rule_type = 'SIZE'").stream()
                .collect(Collectors.toMap(row -> (String) row.get("rule_key"),
                        row -> (BigDecimal) row.get("max_weight_kg")));
        assertThat(maxWeights).containsExactlyInAnyOrderEntriesOf(Map.of(
                "SMALL", new BigDecimal("5.00"), "MEDIUM", new BigDecimal("10.00"),
                "LARGE", new BigDecimal("20.00"), "EXTRA_LARGE", new BigDecimal("40.00")));
    }

    @Test
    @DisplayName("Seeds the DROP100 promo code worth 100 LKR without an end date")
    void seedsDrop100() {
        Map<String, Object> promo = jdbc.queryForMap(
                "SELECT discount_amount, currency, valid_to, active FROM promo_codes WHERE code = 'DROP100'");

        assertThat((BigDecimal) promo.get("discount_amount")).isEqualByComparingTo("100.00");
        assertThat(promo).containsEntry("currency", "LKR").containsEntry("active", true);
        assertThat(promo.get("valid_to")).isNull();
    }

    @Test
    @DisplayName("Constraints reject unknown groups and rule keys, negative amounts and sizes without a maximum weight")
    void rejectsInvalidRows() {
        assertRejected(() -> jdbc.update(
                "INSERT INTO zones (code, name, zone_group) VALUES ('JAFFNA', 'Jaffna', 'NORTHERN')"));
        assertRejected(() -> jdbc.update(
                "INSERT INTO zones (code, name, zone_group) VALUES ('KANDY', 'Kandy again', 'INTER_CITY')"));
        assertRejected(() -> jdbc.update(
                "INSERT INTO zones (code, name, zone_group) VALUES ('kandy_lower', 'Lower case', 'INTER_CITY')"));
        assertRejected(() -> jdbc.update(
                "INSERT INTO pricing_rules (rule_type, rule_key, amount) VALUES ('SPEED', 'OVERNIGHT', 10)"));
        assertRejected(() -> jdbc.update(
                "UPDATE pricing_rules SET amount = -1 WHERE rule_type = 'SPEED' AND rule_key = 'EXPRESS'"));
        assertRejected(() -> jdbc.update(
                "UPDATE pricing_rules SET max_weight_kg = NULL WHERE rule_type = 'SIZE' AND rule_key = 'SMALL'"));
        assertRejected(() -> jdbc.update(
                "UPDATE pricing_rules SET max_weight_kg = 5 WHERE rule_type = 'SPEED' AND rule_key = 'EXPRESS'"));
        assertRejected(() -> jdbc.update("""
                INSERT INTO promo_codes (code, discount_amount, valid_from, valid_to)
                VALUES ('LATE', 50, now(), now() - interval '1 day')
                """));
        assertRejected(() -> jdbc.update(
                "INSERT INTO promo_codes (code, discount_amount, valid_from) VALUES ('FREE', 0, now())"));
        assertRejected(() -> jdbc.update(
                "INSERT INTO promo_codes (code, discount_amount, valid_from) VALUES ('drop50', 50, now())"));
    }

    @Test
    @DisplayName("Deleting a zone also deletes its districts")
    void deletingAZoneDeletesItsDistricts() {
        jdbc.update("DELETE FROM zones WHERE code = 'KANDY'");

        Integer orphans = jdbc.queryForObject("""
                SELECT count(*) FROM zone_districts d WHERE NOT EXISTS (SELECT 1 FROM zones z WHERE z.id = d.zone_id)
                """, Integer.class);
        assertThat(orphans).isZero();
    }

    private static void assertRejected(Runnable statement) {
        assertThatThrownBy(statement::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static DriverManagerDataSource dataSource(String url) {
        return new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
