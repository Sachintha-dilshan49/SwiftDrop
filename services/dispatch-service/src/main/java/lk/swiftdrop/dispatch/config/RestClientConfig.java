package lk.swiftdrop.dispatch.config;

import java.net.http.HttpClient;
import lk.swiftdrop.common.internal.InternalRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    /**
     * {@code /internal/**} calls. The common factory adds X-Internal-Token (ADR-010) and maps 404/409/5xx; its
     * request factory is replaced because HttpURLConnection cannot send PATCH.
     */
    @Bean
    public RestClient parcelServiceRestClient(InternalRestClientFactory clients,
                                              @Value("${swiftdrop.services.parcel-url}") String baseUrl) {
        return clients.builder("parcel-service", baseUrl).requestFactory(requestFactory()).build();
    }

    /** auth-service {@code /internal/**} calls (courier names); GET only, so the factory's default client is fine. */
    @Bean
    public RestClient authServiceRestClient(InternalRestClientFactory clients,
                                            @Value("${swiftdrop.services.auth-url}") String baseUrl) {
        return clients.create("auth-service", baseUrl);
    }

    /**
     * Public {@code /api/v1/**} calls made with the signed-in user's own JWT, so parcel-service applies its
     * usual role checks. Never carries the internal token.
     */
    @Bean
    public RestClient parcelServiceUserRestClient(@Value("${swiftdrop.services.parcel-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory()).build();
    }

    // HTTP/1.1 because the default h2c upgrade breaks requests with a body on plain-HTTP servers.
    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(InternalRestClientFactory.CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(InternalRestClientFactory.READ_TIMEOUT);
        return factory;
    }
}
