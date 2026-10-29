package lk.swiftdrop.parcel;

import lk.swiftdrop.parcel.support.AbstractContainersIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ParcelApplicationIT extends AbstractContainersIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesFlywayMigrationsOnStartup() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        String latest = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);

        assertThat(applied).isNotNull().isGreaterThanOrEqualTo(5);
        assertThat(latest).isEqualTo("5");
    }
}
