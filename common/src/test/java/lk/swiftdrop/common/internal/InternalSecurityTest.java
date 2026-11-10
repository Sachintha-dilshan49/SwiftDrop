package lk.swiftdrop.common.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// No token is configured here, so InternalTokenTestContextCustomizerFactory supplies TEST_TOKEN
@WebMvcTest(controllers = InternalTestController.class)
@ExtendWith(OutputCaptureExtension.class)
class InternalSecurityTest {

    private static final String TOKEN = InternalTokenTestContextCustomizerFactory.TEST_TOKEN;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void jwts() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            if ("courier-token".equals(invocation.getArgument(0))) {
                return Jwt.withTokenValue("courier-token").header("alg", "RS256").subject("courier-1")
                        .claim(SwiftDropSecurityAutoConfiguration.ROLES_CLAIM, List.of("COURIER")).build();
            }
            throw new BadJwtException("Signed JWT rejected");
        });
    }

    @Test
    void internalCallWithoutTokenGets401Problem() throws Exception {
        mockMvc.perform(get("/internal/whoami").header(RequestIds.HEADER, "req-int-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"))
                .andExpect(jsonPath("$.detail").value(InternalTokenRequiredException.DETAIL))
                .andExpect(jsonPath("$.instance").value("/internal/whoami"))
                .andExpect(jsonPath("$.requestId").value("req-int-1"));
    }

    @Test
    void internalCallWithWrongTokenGets401AndTokenIsNotLogged(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/internal/whoami").header(InternalTokenAuthenticationFilter.HEADER, "guessed-token-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://swiftdrop.lk/problems/unauthorized"));

        assertThat(output.getOut()).contains("invalid X-Internal-Token").doesNotContain("guessed-token-123");
    }

    @Test
    void internalCallWithCorrectTokenIsAuthenticatedAsInternalService() throws Exception {
        mockMvc.perform(get("/internal/whoami").header(InternalTokenAuthenticationFilter.HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem(InternalTokenAuthenticationFilter.ROLE)));
    }

    @Test
    void userJwtAloneDoesNotOpenInternalEndpoints() throws Exception {
        mockMvc.perform(get("/internal/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer courier-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(InternalTokenRequiredException.DETAIL));
    }

    @Test
    void normalRoutesStillRequireAJwt() throws Exception {
        mockMvc.perform(get("/api/v1/things"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.startsWith("Bearer")));

        mockMvc.perform(get("/api/v1/things").header(HttpHeaders.AUTHORIZATION, "Bearer courier-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("ROLE_COURIER")));
    }

    @Test
    void internalTokenDoesNotOpenNormalRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/things").header(InternalTokenAuthenticationFilter.HEADER, TOKEN))
                .andExpect(status().isUnauthorized());
    }
}
