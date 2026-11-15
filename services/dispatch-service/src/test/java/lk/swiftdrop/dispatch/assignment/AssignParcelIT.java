package lk.swiftdrop.dispatch.assignment;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import lk.swiftdrop.dispatch.courier.Courier;
import lk.swiftdrop.dispatch.courier.CourierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Assigning against real PostgreSQL and a fake parcel-service, then reading it back as the courier. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AssignParcelIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @RegisterExtension
    static WireMockExtension parcelService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.services.parcel-url", parcelService::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Test
    void savesAssignmentAndRejectsSecondActiveAssignmentForSameParcel() throws Exception {
        Courier courier = courier();
        UUID parcelId = UUID.randomUUID();
        stubParcel(parcelId, "CREATED");

        mockMvc.perform(assign(parcelId, courier.getId())).andExpect(status().isCreated());
        mockMvc.perform(assign(parcelId, courier.getId())).andExpect(status().isConflict());

        assertThat(assignmentRepository.existsByParcelIdAndStatusIn(parcelId, AssignmentStatus.ACTIVE)).isTrue();
        assertThat(assignmentRepository.findAll()).filteredOn(a -> a.getParcelId().equals(parcelId)).hasSize(1);
    }

    @Test
    void courierSeesTheParcelDetailsCopiedAtAssignment() throws Exception {
        Courier courier = courier();
        UUID parcelId = UUID.randomUUID();
        stubParcel(parcelId, "CREATED");

        mockMvc.perform(assign(parcelId, courier.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNo").value("SD-0001"));

        mockMvc.perform(get("/api/v1/courier/deliveries/today")
                        .with(jwt().jwt(token -> token.subject(courier.getUserId().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_COURIER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNo").value("SD-0001"))
                .andExpect(jsonPath("$[0].parcelStatus").value("CREATED"))
                .andExpect(jsonPath("$[0].pickupAddress").value("Liberty Plaza"))
                .andExpect(jsonPath("$[0].dropoffAddress").value("Rajagiriya Flyover"))
                .andExpect(jsonPath("$[0].recipientName").value("Nimal"))
                .andExpect(jsonPath("$[0].weightKg").value(1.2));
    }

    @Test
    void parcelAlreadyOnItsWayCannotBeAssigned() throws Exception {
        Courier courier = courier();
        UUID parcelId = UUID.randomUUID();
        stubParcel(parcelId, "IN_TRANSIT");

        mockMvc.perform(assign(parcelId, courier.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/parcel-not-waiting"));
    }

    private Courier courier() {
        Courier courier = new Courier();
        courier.setUserId(UUID.randomUUID());
        return courierRepository.save(courier);
    }

    private static void stubParcel(UUID parcelId, String status) {
        parcelService.stubFor(WireMock.get(urlPathEqualTo("/api/v1/parcels/" + parcelId)).willReturn(okJson("""
                {"id":"%s","trackingNo":"SD-0001","status":"%s","pickupAddress":"Liberty Plaza",
                 "dropoffAddress":"Rajagiriya Flyover","recipientName":"Nimal","recipientPhone":"0711234567",
                 "size":"SMALL","weightKg":1.20,"feeAmount":550.00,"feeCurrency":"LKR","history":[]}
                """.formatted(parcelId, status))));
    }

    private static MockHttpServletRequestBuilder assign(UUID parcelId, UUID courierId) {
        return post("/api/v1/dispatch/assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parcelId\":\"" + parcelId + "\",\"courierId\":\"" + courierId + "\"}")
                .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER")));
    }
}
