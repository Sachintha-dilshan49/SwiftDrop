package lk.swiftdrop.parcel.tracking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TrackingSseProperties.class)
public class TrackingConfig {
}
