package lk.swiftdrop.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("swiftdrop.google")
public record GoogleOAuthProperties(
        boolean enabled,
        String clientId,
        String clientSecret,
        String frontendSuccessUri) {
}
