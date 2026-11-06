package lk.swiftdrop.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lk.swiftdrop.common.autoconfigure.SwiftDropSecurityAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.HeaderBearerTokenResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SecuredTestController.class, properties = "swiftdrop.security.public-paths=/test/public/**")
class CustomBearerTokenResolverTest {

    static final String CUSTOM_HEADER = "X-Test-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BearerTokenResolver bearerTokenResolver;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void tokens() {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            if (!"courier-token".equals(invocation.getArgument(0))) {
                throw new BadJwtException("Signed JWT rejected");
            }
            return Jwt.withTokenValue("courier-token")
                    .header("alg", "RS256")
                    .subject("6a1f0f7e-1111-4c2e-9a71-0d6f2b9c1e44")
                    .claim(SwiftDropSecurityAutoConfiguration.ROLES_CLAIM, List.of("COURIER"))
                    .build();
        });
    }

    @Test
    void serviceDefinedResolverReplacesTheDefault() {
        assertThat(bearerTokenResolver).isInstanceOf(HeaderBearerTokenResolver.class);
    }

    @Test
    void filterChainUsesTheServiceDefinedResolver() throws Exception {
        mockMvc.perform(get("/test/me").header(CUSTOM_HEADER, "courier-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/test/me").header(HttpHeaders.AUTHORIZATION, "Bearer courier-token"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomResolverConfig {

        @Bean
        BearerTokenResolver customBearerTokenResolver() {
            return new HeaderBearerTokenResolver(CUSTOM_HEADER);
        }
    }
}
