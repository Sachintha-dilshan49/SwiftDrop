package lk.swiftdrop.auth.identity;

import io.minio.MinioClient;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "swiftdrop.identity-storage.enabled", havingValue = "true")
public class IdentityStorageConfiguration {
    @Bean
    MinioClient identityMinioClient(
            @Value("${swiftdrop.identity-storage.endpoint}") String endpoint,
            @Value("${swiftdrop.identity-storage.access-key}") String accessKey,
            @Value("${swiftdrop.identity-storage.secret-key}") String secretKey,
            @Value("${swiftdrop.identity-storage.bucket}") String bucket) {
        URI uri = URI.create(endpoint);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("Identity storage requires an HTTPS endpoint without embedded credentials.");
        }
        if (accessKey.isBlank() || secretKey.isBlank() || bucket.isBlank() || bucket.equals("delivery-proofs")) {
            throw new IllegalStateException("Identity storage requires credentials and a separate private bucket.");
        }
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}
