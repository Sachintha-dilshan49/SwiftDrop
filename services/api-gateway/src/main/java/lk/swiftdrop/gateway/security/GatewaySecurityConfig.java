package lk.swiftdrop.gateway.security;

import java.util.ArrayList;
import java.util.List;
import lk.swiftdrop.common.web.RequestIds;
import lk.swiftdrop.gateway.config.GatewayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * The gateway only authenticates: a valid JWT for everything that is not public. Roles and
 * ownership are checked by the services, which verify the token again. The
 * {@code Authorization} header is forwarded unchanged.
 *
 * <p>Two chains:
 * <ol>
 *   <li>Public paths ({@code swiftdrop.gateway.public-paths}) and CORS preflight: permitted, with no
 *       resource server, so a stale token sent to {@code /auth/refresh} does not cause a 401.</li>
 *   <li>Everything else: JWT required, checked against auth-service's JWKS.</li>
 * </ol>
 * Both apply CORS inside Spring Security, so preflight is answered before authentication.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewaySecurityConfig {

    static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    static final List<String> ALLOWED_HEADERS = List.of(
            HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-API-Key", RequestIds.HEADER);

    @Bean
    @Order(1)
    SecurityWebFilterChain publicSecurityWebFilterChain(ServerHttpSecurity http, GatewayProperties properties,
            CorsConfigurationSource corsConfigurationSource) {
        return stateless(http, corsConfigurationSource)
                .securityMatcher(publicMatcher(properties))
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityWebFilterChain protectedSecurityWebFilterChain(ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource, ProblemDetailAuthenticationEntryPoint entryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        return stateless(http, corsConfigurationSource)
                .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewayProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.cors().allowedOrigins());
        cors.setAllowedMethods(ALLOWED_METHODS);
        cors.setAllowedHeaders(ALLOWED_HEADERS);
        cors.setExposedHeaders(List.of(RequestIds.HEADER));
        // The refresh token is an httpOnly cookie
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    static ServerWebExchangeMatcher publicMatcher(GatewayProperties properties) {
        List<ServerWebExchangeMatcher> matchers = new ArrayList<>();
        matchers.add(exchange -> CorsUtils.isPreFlightRequest(exchange.getRequest())
                ? ServerWebExchangeMatcher.MatchResult.match()
                : ServerWebExchangeMatcher.MatchResult.notMatch());
        for (GatewayProperties.PublicPath publicPath : properties.publicPaths()) {
            matchers.add(publicPath.method() == null || publicPath.method().isBlank()
                    ? ServerWebExchangeMatchers.pathMatchers(publicPath.path())
                    : ServerWebExchangeMatchers.pathMatchers(HttpMethod.valueOf(publicPath.method().trim()),
                            publicPath.path()));
        }
        return new OrServerWebExchangeMatcher(matchers);
    }

    private static ServerHttpSecurity stateless(ServerHttpSecurity http, CorsConfigurationSource cors) {
        return http
                .cors(spec -> spec.configurationSource(cors))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Services send their own security headers; the gateway copies them, so adding ours would duplicate them
                .headers(ServerHttpSecurity.HeaderSpec::disable)
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance());
    }
}
