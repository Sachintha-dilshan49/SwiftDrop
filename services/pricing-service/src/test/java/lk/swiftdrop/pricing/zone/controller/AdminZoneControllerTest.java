package lk.swiftdrop.pricing.zone.controller;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.pricing.support.TestJwts;
import lk.swiftdrop.pricing.zone.dto.AdminZoneResponse;
import lk.swiftdrop.pricing.zone.dto.CreateZoneRequest;
import lk.swiftdrop.pricing.zone.dto.UpdateZoneRequest;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;
import lk.swiftdrop.pricing.zone.service.ZoneAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminZoneController.class)
class AdminZoneControllerTest {

    private static final String ZONES = "/api/v1/admin/zones";
    private static final String ADMIN = TestJwts.bearer("ADMIN");
    private static final String NEW_ZONE = """
            { "code": "MATARA", "name": "Matara", "group": "INTER_CITY", "districts": ["Matara"] }
            """;
    private static final String UPDATE = """
            { "name": "Galle Fort", "group": "INTER_CITY", "active": false, "districts": ["Galle", "Unawatuna"] }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZoneAdminService zoneAdminService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUpTokens() {
        TestJwts.acceptRoleTokens(jwtDecoder);
    }

    @Test
    void adminListsAllZonesIncludingInactiveOnes() throws Exception {
        given(zoneAdminService.allZones()).willReturn(List.of(zone("KANDY", true), zone("GALLE", false)));

        mockMvc.perform(get(ZONES).header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].code").value("GALLE"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void adminGetsOneZone() throws Exception {
        given(zoneAdminService.zone(id)).willReturn(zone("KANDY", true));

        mockMvc.perform(get(ZONES + "/" + id).header("Authorization", ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.districts[0]").value("KANDY district"));
    }

    @Test
    void unknownZoneIsNotFound() throws Exception {
        given(zoneAdminService.zone(id)).willThrow(new NotFoundException("Zone " + id + " was not found."));

        mockMvc.perform(get(ZONES + "/" + id).header("Authorization", ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCreatesAZoneWith201AndLocation() throws Exception {
        given(zoneAdminService.create(any(CreateZoneRequest.class))).willReturn(zone("MATARA", true));

        mockMvc.perform(post(ZONES).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ZONE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", ZONES + "/" + id))
                .andExpect(jsonPath("$.code").value("MATARA"));

        verify(zoneAdminService).create(new CreateZoneRequest("MATARA", "Matara", ZoneGroup.INTER_CITY, null,
                List.of("Matara")));
    }

    @Test
    void createWithInvalidFieldsReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ZONES).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "matara", "name": " ", "districts": [] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/validation-error"))
                .andExpect(jsonPath("$.errors[*].field", hasItems("code", "name", "group", "districts")));

        verifyNoInteractions(zoneAdminService);
    }

    @Test
    void duplicateCodeIsAConflict() throws Exception {
        given(zoneAdminService.create(any(CreateZoneRequest.class)))
                .willThrow(new ConflictException("Zone code MATARA already exists."));

        mockMvc.perform(post(ZONES).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ZONE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Zone code MATARA already exists."));
    }

    @Test
    void adminUpdatesAZone() throws Exception {
        given(zoneAdminService.update(eq(id), any(UpdateZoneRequest.class))).willReturn(zone("GALLE", false));

        mockMvc.perform(put(ZONES + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(zoneAdminService).update(id, new UpdateZoneRequest("Galle Fort", ZoneGroup.INTER_CITY, false,
                List.of("Galle", "Unawatuna")));
    }

    @Test
    void updateWithoutActiveFlagIsBadRequest() throws Exception {
        mockMvc.perform(put(ZONES + "/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Galle", "group": "INTER_CITY", "districts": ["Galle"] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("active"));
    }

    @Test
    void deleteDeactivatesTheZoneWith204() throws Exception {
        mockMvc.perform(delete(ZONES + "/" + id).header("Authorization", ADMIN))
                .andExpect(status().isNoContent());

        verify(zoneAdminService).deactivate(id);
    }

    @Test
    void deleteOfAnUnknownZoneIsNotFound() throws Exception {
        willThrow(new NotFoundException("Zone " + id + " was not found.")).given(zoneAdminService).deactivate(id);

        mockMvc.perform(delete(ZONES + "/" + id).header("Authorization", ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(ZONES)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ZONES).contentType(MediaType.APPLICATION_JSON).content(NEW_ZONE))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(zoneAdminService);
    }

    @ParameterizedTest(name = "{0} is forbidden")
    @ValueSource(strings = {"CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER"})
    void nonAdminIsForbidden(String role) throws Exception {
        mockMvc.perform(get(ZONES).header("Authorization", TestJwts.bearer(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/forbidden"));
        mockMvc.perform(post(ZONES).header("Authorization", TestJwts.bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_ZONE))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(ZONES + "/" + id).header("Authorization", TestJwts.bearer(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(zoneAdminService);
    }

    private AdminZoneResponse zone(String code, boolean active) {
        return new AdminZoneResponse(id, code, code, ZoneGroup.INTER_CITY, active, List.of(code + " district"),
                Instant.parse("2026-11-01T00:00:00Z"), null);
    }
}
