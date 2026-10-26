package lk.swiftdrop.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient authServiceRestClient(
            @Value("${swiftdrop.services.auth-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(serviceTimeoutFactory())
                .build();
    }

    @Bean
    public RestClient merchantServiceRestClient(
            @Value("${swiftdrop.services.merchant-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(serviceTimeoutFactory())
                .build();
    }

    @Bean
    public RestClient webhookClient() {
        return RestClient.builder()
                .requestFactory(webhookTimeoutFactory())
                .build();
    }

    private SimpleClientHttpRequestFactory serviceTimeoutFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    private SimpleClientHttpRequestFactory webhookTimeoutFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
