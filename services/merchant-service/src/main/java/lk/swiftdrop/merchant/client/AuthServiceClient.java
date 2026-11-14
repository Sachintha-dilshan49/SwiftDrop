package lk.swiftdrop.merchant.client;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestClient authServiceRestClient;

    public AuthServiceClient(@Qualifier("authServiceRestClient") RestClient authServiceRestClient) {
        this.authServiceRestClient = authServiceRestClient;
    }

    public void grantRole(UUID userId, String role) {
        try {
            log.info("Calling auth-service to grant role {} to user {}", role, userId);
            authServiceRestClient.post()
                    .uri("/internal/users/{userId}/roles", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GrantRoleRequest(role))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully granted role {} to user {}", role, userId);
        } catch (Exception e) {
            log.error("Failed to grant role {} to user {}: {}", role, userId, e.getMessage());
            throw e;
        }
    }
}

