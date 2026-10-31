package lk.swiftdrop.notification.config;

import lk.swiftdrop.common.internal.InternalRestClientFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Client for auth-service internal endpoint (GET /internal/users/{id}/contact).
     * Uses InternalRestClientFactory (ADR-010): attaches X-Internal-Token + X-Request-Id,
     * applies 2s/5s timeouts, and maps 404 → NotFoundException, 5xx → ServiceUnavailableException.
     */
    @Bean
    public RestClient authServiceRestClient(
            InternalRestClientFactory clients,
            @Value("${swiftdrop.services.auth-url}") String baseUrl) {
        return clients.create("auth-service", baseUrl);
    }

    /**
     * Client for merchant-service internal endpoint (GET /internal/merchants/{id}/webhook).
     * Uses InternalRestClientFactory (ADR-010): attaches X-Internal-Token + X-Request-Id,
     * applies 2s/5s timeouts, and maps 404 → NotFoundException, 5xx → ServiceUnavailableException.
     */
    @Bean
    @Qualifier("merchantServiceClient")
    public RestClient merchantServiceRestClient(
            InternalRestClientFactory clients,
            @Value("${swiftdrop.services.merchant-url}") String baseUrl) {
        return clients.create("merchant-service", baseUrl);
    }

    /**
     * Outbound webhook client — NOT an internal call. No X-Internal-Token.
     * Longer timeouts to accommodate slower merchant endpoints.
     */
    @Bean
    public RestClient webhookClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
