package lk.swiftdrop.pricing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The context without a database: no Flyway, no schema validation, no database health check. The URL points
 * at a closed port so the test never touches a developer's local database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/unused",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
class PricingApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsWithPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /** springdoc.paths-to-exclude keeps the service-to-service quote out of the public API docs. */
    @Test
    void publicApiDocsListPublicEndpointsButNotTheInternalQuote() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/pricing/estimate']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pricing/zones']").exists())
                .andExpect(jsonPath("$.paths['/internal/pricing/quote']").doesNotExist());
    }
}
