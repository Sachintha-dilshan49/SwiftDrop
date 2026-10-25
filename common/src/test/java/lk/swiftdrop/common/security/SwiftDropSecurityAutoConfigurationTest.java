package lk.swiftdrop.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lk.swiftdrop.common.autoconfigure.SwiftDropSecurityAutoConfiguration;
import lk.swiftdrop.common.web.RequestIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecuredTestController.class, properties = "swiftdrop.security.public-paths=/test/public/**")
class SwiftDropSecurityAutoConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void tokens() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> switch (invocation.<String>getArgument(0)) {
            case "admin-token" -> jwt("admin-token", "ADMIN");
            case "courier-token" -> jwt("courier-token", "COURIER");
            default -> throw new BadJwtException("Signed JWT rejected");
        });
    }

    @Test
    void rejectsRequestWithoutTokenWith401Problem() throws Exception {
        mockMvc.perform(get("/test/me").header(RequestIds.HEADER, "req-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"))
                .andExpect(jsonPath("$.instance").value("/test/me"))
                .andExpect(jsonPath("$.requestId").value("req-401"));
    }

    @Test
    void rejectsInvalidTokenWithoutLeakingTheReason() throws Exception {
        String body = mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(SecurityExceptionHandler.UNAUTHORIZED_DETAIL))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("Signed JWT rejected");
    }

    @Test
    void acceptsValidToken() throws Exception {
        mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer courier-token"))
                .andExpect(status().isOk());
    }

    @Test
    void courierCannotCallAdminEndpoint() throws Exception {
        mockMvc.perform(get("/test/admin").header(HttpHeaders.AUTHORIZATION, "Bearer courier-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/forbidden"))
                .andExpect(jsonPath("$.detail").value(SecurityExceptionHandler.FORBIDDEN_DETAIL))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void adminCanCallAdminEndpoint() throws Exception {
        mockMvc.perform(get("/test/admin").header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void configuredPublicPathsNeedNoToken() throws Exception {
        mockMvc.perform(get("/test/public/info"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCallerDeniedByPreAuthorizeOnPublicPathGets401() throws Exception {
        mockMvc.perform(get("/test/public/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));
    }

    @Test
    void healthAndOpenApiPathsAreAlwaysPublic() throws Exception {
        // No handler in this slice, so 404 (not 401) proves the request got past security
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isNotFound());
    }

    @Test
    void mapsRolesClaimToRoleAuthorities() {
        List<String> authorities = jwtAuthenticationConverter.convert(jwt("t", "COURIER", "DISPATCHER"))
                .getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        // Spring Security 7 also adds the FACTOR_BEARER authentication-factor authority
        assertThat(authorities).filteredOn(authority -> authority.startsWith("ROLE_"))
                .containsExactlyInAnyOrder("ROLE_COURIER", "ROLE_DISPATCHER");
    }

    private static Jwt jwt(String token, String... roles) {
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("6a1f0f7e-1111-4c2e-9a71-0d6f2b9c1e44")
                .claim(SwiftDropSecurityAutoConfiguration.ROLES_CLAIM, List.of(roles))
                .build();
    }
}
