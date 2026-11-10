package lk.swiftdrop.parcel.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 2 mapper for RabbitMQ event payloads (outbox rows and the AMQP message converter).
 * HTTP responses use Spring Boot's Jackson 3 mapper and are not affected.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return eventObjectMapper();
    }

    /**
     * Timestamps as ISO-8601 strings, as the {@code ParcelStatusChanged} contract requires
     * (docs/architecture.md §6). Unknown fields are ignored so older rows and newer publishers stay readable.
     */
    public static ObjectMapper eventObjectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
