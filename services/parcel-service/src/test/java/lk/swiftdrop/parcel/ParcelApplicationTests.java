package lk.swiftdrop.parcel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "management.health.db.enabled=false",
        "management.health.rabbit.enabled=false"
})
@AutoConfigureMockMvc
class ParcelApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsWithPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /** springdoc.paths-to-exclude keeps service-to-service endpoints out of the public API docs. */
    @Test
    void publicApiDocsListPublicEndpointsButNotInternalOnes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/parcels']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/parcels/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/parcels/{id}/receipt']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/track/{trackingNo}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/track/{trackingNo}/stream']").exists())
                .andExpect(jsonPath("$.paths['/internal/parcels']").doesNotExist())
                .andExpect(jsonPath("$.paths['/internal/parcels/{id}/status']").doesNotExist())
                .andExpect(jsonPath("$.paths['/internal/parcels/{id}/contacts']").doesNotExist());
    }
}
