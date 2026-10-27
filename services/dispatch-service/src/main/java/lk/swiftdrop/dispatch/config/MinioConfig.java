package lk.swiftdrop.dispatch.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(@Value("${swiftdrop.storage.endpoint}") String endpoint,
            @Value("${swiftdrop.storage.access-key}") String accessKey,
            @Value("${swiftdrop.storage.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}
