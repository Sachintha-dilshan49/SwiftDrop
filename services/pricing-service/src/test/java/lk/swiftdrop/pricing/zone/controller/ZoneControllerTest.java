package lk.swiftdrop.pricing.zone.controller;

import lk.swiftdrop.pricing.support.TestJwts;
import lk.swiftdrop.pricing.zone.dto.ZoneResponse;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;
import lk.swiftdrop.pricing.zone.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ZoneController.class)
class ZoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZoneService zoneService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        TestJwts.acceptRoleTokens(jwtDecoder);
        given(zoneService.activeZones()).willReturn(List.of(
                new ZoneResponse("COLOMBO_SOUTH", "Colombo South", ZoneGroup.COLOMBO,
                        List.of("Colombo 04", "Colombo 05", "Colombo 06")),
                new ZoneResponse("KANDY", "Kandy", ZoneGroup.INTER_CITY, List.of("Kandy"))));
    }

    @Test
    @DisplayName("A guest without a token gets the active zones with code, name, group and districts")
    void guestGetsTheActiveZonesWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("COLOMBO_SOUTH"))
                .andExpect(jsonPath("$[0].name").value("Colombo South"))
                .andExpect(jsonPath("$[0].group").value("COLOMBO"))
                .andExpect(jsonPath("$[0].districts[2]").value("Colombo 06"))
                .andExpect(jsonPath("$[1].group").value("INTER_CITY"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void staleTokenIsIgnored() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/zones").header("Authorization", "Bearer expired-or-broken"))
                .andExpect(status().isOk());
    }

    @Test
    void signedInUserGetsTheZonesToo() throws Exception {
        mockMvc.perform(get("/api/v1/pricing/zones").header("Authorization", TestJwts.bearer("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
