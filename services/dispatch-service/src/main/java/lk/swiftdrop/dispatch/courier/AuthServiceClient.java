package lk.swiftdrop.dispatch.courier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Reads a courier's display name from auth-service's internal API (X-Internal-Token, see RestClientConfig). */
@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestClient restClient;

    public AuthServiceClient(@Qualifier("authServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * The user's first name, or empty if auth-service does not know the user. Throws
     * {@code ServiceUnavailableException} if auth-service cannot be reached.
     */
    public Optional<String> firstName(UUID userId) {
        try {
            UserName user = restClient.get()
                    .uri("/internal/users/{id}/contact", userId)
                    .retrieve()
                    .body(UserName.class);
            return Optional.ofNullable(user).map(UserName::firstName).filter(name -> !name.isBlank());
        } catch (NotFoundException e) {
            log.warn("auth-service has no user {} for a courier", userId);
            return Optional.empty();
        }
    }

    /** Only the first name is read; the email and phone in the reply are ignored. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserName(String firstName) {
    }
}
