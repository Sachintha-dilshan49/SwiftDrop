package lk.swiftdrop.common.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import lk.swiftdrop.common.security.ProblemDetailAccessDeniedHandler;
import lk.swiftdrop.common.security.ProblemDetailAuthenticationEntryPoint;
import lk.swiftdrop.common.security.SecurityExceptionHandler;
import lk.swiftdrop.common.security.SwiftDropSecurityProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Stateless JWT resource-server security shared by all servlet services.
 *
 * <ul>
 *   <li>Tokens are verified with the JWKS at {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}
 *       (set from {@code JWT_JWK_SET_URI} in each service's application.yml).</li>
 *   <li>The {@code roles} claim becomes {@code ROLE_*} authorities, so {@code @PreAuthorize("hasRole('COURIER')")}
 *       works; method security is enabled.</li>
 *   <li>Health, OpenAPI and {@code swiftdrop.security.public-paths} are public; everything else needs a token.</li>
 *   <li>401/403 are ProblemDetail bodies.</li>
 * </ul>
 *
 * <p>A service that needs different rules for some paths (API keys, OAuth2 login) adds its own
 * {@link SecurityFilterChain} with a {@code securityMatcher} and an {@code @Order} lower than
 * {@link #FILTER_CHAIN_ORDER}; this chain then handles everything else.
 */
@AutoConfiguration(before = {ServletWebSecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, JwtAuthenticationConverter.class, DispatcherServlet.class})
@EnableConfigurationProperties(SwiftDropSecurityProperties.class)
public class SwiftDropSecurityAutoConfiguration {

    public static final int FILTER_CHAIN_ORDER = Ordered.LOWEST_PRECEDENCE - 5;
    public static final String ROLES_CLAIM = "roles";

    static final List<String> ALWAYS_PUBLIC = List.of(
            "/actuator/health", "/actuator/health/**", "/v3/api-docs", "/v3/api-docs/**", "/error");

    @Bean
    @Order(FILTER_CHAIN_ORDER)
    @ConditionalOnMissingBean(name = "swiftDropSecurityFilterChain")
    public SecurityFilterChain swiftDropSecurityFilterChain(HttpSecurity http, SwiftDropSecurityProperties properties,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ProblemDetailAuthenticationEntryPoint entryPoint, ProblemDetailAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        List<String> publicPaths = new ArrayList<>(ALWAYS_PUBLIC);
        publicPaths.addAll(properties.publicPaths());

        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(publicPaths.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLES_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    // @Lazy: the MVC resolver is created after the security filter chain
    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint(
            @Lazy @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new ProblemDetailAuthenticationEntryPoint(resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler(
            @Lazy @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new ProblemDetailAccessDeniedHandler(resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler() {
        return new SecurityExceptionHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
