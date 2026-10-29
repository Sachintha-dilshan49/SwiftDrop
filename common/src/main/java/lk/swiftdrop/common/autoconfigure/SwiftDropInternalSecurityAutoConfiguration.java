package lk.swiftdrop.common.autoconfigure;

import lk.swiftdrop.common.internal.InternalTokenAuthenticationEntryPoint;
import lk.swiftdrop.common.internal.InternalTokenAuthenticationFilter;
import lk.swiftdrop.common.internal.SwiftDropInternalProperties;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Service-to-service authentication for servlet services (ADR-010).
 *
 * <ul>
 *   <li>{@code /internal/**} has its own filter chain, ordered before the JWT chain: it needs
 *       {@code X-Internal-Token} = {@code swiftdrop.internal.token} and grants {@code ROLE_INTERNAL_SERVICE}.
 *       A user JWT does not open it, and the internal token does not open any other path.</li>
 *   <li>Startup fails when the token is not set.</li>
 * </ul>
 */
@AutoConfiguration(before = {ServletWebSecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class,
        OAuth2ResourceServerWebSecurityAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SecurityFilterChain.class, DispatcherServlet.class, RestClient.class})
@EnableConfigurationProperties(SwiftDropInternalProperties.class)
public class SwiftDropInternalSecurityAutoConfiguration {

    public static final int FILTER_CHAIN_ORDER = SwiftDropSecurityAutoConfiguration.FILTER_CHAIN_ORDER - 10;
    public static final String INTERNAL_PATHS = "/internal/**";

    @Bean
    @Order(FILTER_CHAIN_ORDER)
    @ConditionalOnMissingBean(name = "swiftDropInternalFilterChain")
    public SecurityFilterChain swiftDropInternalFilterChain(HttpSecurity http, SwiftDropInternalProperties properties,
            @Lazy @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        InternalTokenAuthenticationEntryPoint entryPoint = new InternalTokenAuthenticationEntryPoint(resolver);
        // Not a bean on purpose: Boot would register a Filter bean for every request
        InternalTokenAuthenticationFilter filter = new InternalTokenAuthenticationFilter(properties.requiredToken());

        http.securityMatcher(INTERNAL_PATHS)
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(filter, AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().hasRole("INTERNAL_SERVICE"))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));
        return http.build();
    }
}
