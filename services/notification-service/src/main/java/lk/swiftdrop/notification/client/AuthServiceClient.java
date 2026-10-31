package lk.swiftdrop.notification.client;

import lk.swiftdrop.common.error.SwiftDropException;
import lk.swiftdrop.notification.client.dto.UserContactResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    private final RestClient restClient;

    public AuthServiceClient(@Qualifier("authServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetches contact details for a user from auth-service.
     * Returns empty if the user is not found (404) or auth-service is unavailable.
     * The InternalRestClientFactory maps 404 → NotFoundException and I/O failures → ServiceUnavailableException.
     */
    public Optional<UserContactResponse> getUserContact(UUID userId) {
        try {
            UserContactResponse response = restClient.get()
                    .uri("/internal/users/{id}/contact", userId)
                    .retrieve()
                    .body(UserContactResponse.class);
            return Optional.ofNullable(response);
        } catch (SwiftDropException e) {
            log.error("Failed to fetch contact for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }
}
