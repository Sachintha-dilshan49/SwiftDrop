package lk.swiftdrop.pricing;

import lk.swiftdrop.pricing.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** The application starts against PostgreSQL: Flyway applies every migration and Hibernate validates the schema. */
class PricingApplicationIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesFlywayMigrationsOnStartup() {
        String latest = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);

        assertThat(latest).isEqualTo("5");
    }
}
