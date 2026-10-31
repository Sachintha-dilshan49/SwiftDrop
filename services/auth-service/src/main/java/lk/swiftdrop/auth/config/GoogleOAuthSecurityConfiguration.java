package lk.swiftdrop.auth.config;

import lk.swiftdrop.auth.service.GoogleOAuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
@ConditionalOnProperty(name = "swiftdrop.google.enabled", havingValue = "true")
public class GoogleOAuthSecurityConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(GoogleOAuthProperties properties) {
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalStateException(
                    "AUTH_GOOGLE_CLIENT_ID and AUTH_GOOGLE_CLIENT_SECRET are required when AUTH_GOOGLE_ENABLED=true.");
        }
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    @Order(1)
    SecurityFilterChain googleOAuthFilterChain(
            HttpSecurity http,
            GoogleOAuthService googleOAuthService,
            GoogleOAuthProperties properties) throws Exception {
        AuthenticationSuccessHandler successHandler = (request, response, authentication) -> {
            if (!(authentication.getPrincipal() instanceof OAuth2User googleUser)) {
                throw new IllegalStateException("Google login did not provide an OAuth2 user profile.");
            }
            String code = googleOAuthService.createLoginCode(googleUser);
            response.sendRedirect(org.springframework.web.util.UriComponentsBuilder
                    .fromUriString(properties.frontendSuccessUri())
                    .queryParam("code", code)
                    .build()
                    .encode()
                    .toUriString());
        };
        SimpleUrlAuthenticationFailureHandler failureHandler =
                new SimpleUrlAuthenticationFailureHandler(
                        properties.frontendSuccessUri() + "?error=google_login_failed");

        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2Login(login -> login
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));
        return http.build();
    }
}
