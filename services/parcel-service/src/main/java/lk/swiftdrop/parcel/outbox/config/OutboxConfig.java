package lk.swiftdrop.parcel.outbox.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
