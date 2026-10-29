package lk.swiftdrop.common.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code swiftdrop.internal.*} settings, bound in each service's application.yml as
 * {@code swiftdrop.internal.token: ${INTERNAL_API_TOKEN:}} (ADR-010).
 *
 * @param token shared secret for {@code /internal/**} calls; same value in every service
 */
@ConfigurationProperties("swiftdrop.internal")
public record SwiftDropInternalProperties(String token) {

    static final String MISSING_TOKEN_MESSAGE = "swiftdrop.internal.token is not set. Set the INTERNAL_API_TOKEN "
            + "environment variable to the shared service-to-service token (see deploy/compose/.env.example).";

    /** The token, or an {@link IllegalStateException} that stops startup when it is missing. */
    public String requiredToken() {
        // A placeholder without a default is bound literally, e.g. "${INTERNAL_API_TOKEN}"
        if (token == null || token.isBlank() || token.startsWith("${")) {
            throw new IllegalStateException(MISSING_TOKEN_MESSAGE);
        }
        return token;
    }

    @Override
    public String toString() {
        return "SwiftDropInternalProperties[token=****]";
    }
}
