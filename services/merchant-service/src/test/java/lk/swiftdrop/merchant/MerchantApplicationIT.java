package lk.swiftdrop.merchant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Starts the service on an empty PostgreSQL database with Flyway and Hibernate's schema validation on, as in
 * production. {@link MerchantApplicationTests} turns both off, so it cannot catch a missing migration run or an
 * entity that does not match its table.
 */
@AutoConfigureMockMvc
class MerchantApplicationIT extends MerchantIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void flywayCreatesEveryTableOnAnEmptyDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);

        assertThat(tables).contains("merchants", "merchant_api_keys", "merchant_webhook_endpoints",
                "merchant_orders", "processed_events", "flyway_schema_history");
    }

    @Test
    void startsWithSchemaValidationAndReportsHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
