package lk.swiftdrop.merchant.config;

import lk.swiftdrop.common.internal.InternalRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient parcelServiceRestClient(
            InternalRestClientFactory clients,
            @Value("${swiftdrop.services.parcel-url}") String parcelServiceUrl) {
        return clients.create("parcel-service", parcelServiceUrl);
    }
}
