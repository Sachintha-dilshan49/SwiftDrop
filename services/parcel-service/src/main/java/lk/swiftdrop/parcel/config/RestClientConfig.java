package lk.swiftdrop.parcel.config;

import lk.swiftdrop.common.internal.InternalRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /** Direct service-to-service calls with the internal token and the shared timeouts (ADR-010). */
    @Bean
    public RestClient pricingServiceRestClient(
            InternalRestClientFactory clients,
            @Value("${swiftdrop.services.pricing-url:http://localhost:8082}") String baseUrl) {
        return clients.create("pricing-service", baseUrl);
    }
}
