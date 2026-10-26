package lk.swiftdrop.dispatch.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** Couriers work in Sri Lanka, so "today" starts at midnight Colombo time, not UTC. */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Colombo");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
