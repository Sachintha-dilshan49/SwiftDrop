package lk.swiftdrop.ai.tools.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code swiftdrop.gateway.*}: where the tools send their requests. Tools call the public API
 * through api-gateway with the caller's JWT, never other services directly.
 *
 * @param baseUrl        gateway URL, e.g. {@code http://api-gateway:8080}
 * @param connectTimeout connect timeout (architecture NFR: 2 s)
 * @param readTimeout    read timeout (architecture NFR: 5 s)
 */
@Validated
@ConfigurationProperties("swiftdrop.gateway")
public record GatewayProperties(
        @NotBlank String baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
