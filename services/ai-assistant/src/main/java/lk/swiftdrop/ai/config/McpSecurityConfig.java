package lk.swiftdrop.ai.config;

import lk.swiftdrop.common.autoconfigure.SwiftDropSecurityAutoConfiguration;
import lk.swiftdrop.common.security.ProblemDetailAccessDeniedHandler;
import lk.swiftdrop.common.security.ProblemDetailAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@code /mcp} is a router function, so {@code @PreAuthorize} cannot guard it. This chain requires
 * the same SwiftDrop roles as the chat endpoints; everything else falls through to the common chain.
 */
@Configuration(proxyBeanMethods = false)
public class McpSecurityConfig {

    @Bean
    @Order(SwiftDropSecurityAutoConfiguration.FILTER_CHAIN_ORDER - 1)
    SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter,
            ProblemDetailAuthenticationEntryPoint entryPoint, ProblemDetailAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http.securityMatcher("/mcp", "/mcp/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().hasAnyRole("CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER", "ADMIN"))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
