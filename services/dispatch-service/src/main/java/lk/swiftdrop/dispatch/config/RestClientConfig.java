package lk.swiftdrop.dispatch.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    // JDK HttpClient, not SimpleClientHttpRequestFactory: HttpURLConnection cannot send PATCH.
    // HTTP/1.1 because the default h2c upgrade breaks requests with a body on plain-HTTP servers.
    @Bean
    public RestClient parcelServiceRestClient(@Value("${swiftdrop.services.parcel-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
