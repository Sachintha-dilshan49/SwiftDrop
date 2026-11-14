package lk.swiftdrop.ai.tools.client;

import java.net.URI;
import java.util.function.Function;
import lk.swiftdrop.ai.tools.ToolFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

/**
 * Calls SwiftDrop's public API through api-gateway as the caller: the caller's bearer token is
 * forwarded, so the owning services apply their normal role and ownership checks.
 *
 * <p>Failures become a {@link ToolFailureException} with a short message the model can relay.
 * Response bodies of errors are never passed on, because they are not meant for the model.
 */
@Component
public class GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayClient.class);

    static final String NOT_FOUND = "Nothing was found for that request.";
    static final String NO_ACCESS = "The user does not have access to that information.";
    static final String INVALID = "SwiftDrop rejected the request as invalid. Check the values and try again.";
    static final String UNAVAILABLE = "That SwiftDrop service is not available right now. Try again later.";

    private final RestClient restClient;

    public GatewayClient(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    /**
     * GET a JSON resource from the gateway.
     *
     * @param uri         builds the path and query, e.g. {@code b -> b.path("/api/v1/track/{no}").build(no)}
     * @param bearerToken caller's JWT without the {@code Bearer } prefix, or {@code null} for public endpoints
     */
    public JsonNode get(Function<UriBuilder, URI> uri, String bearerToken) {
        try {
            return restClient.get()
                    .uri(uri)
                    .headers(headers -> {
                        if (bearerToken != null) {
                            headers.setBearerAuth(bearerToken);
                        }
                    })
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw new ToolFailureException(messageFor(ex.getStatusCode()));
        } catch (ResourceAccessException ex) {
            log.warn("Gateway call failed: {}", ex.getClass().getSimpleName());
            throw new ToolFailureException(UNAVAILABLE);
        }
    }

    static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> NOT_FOUND;
            case 401, 403 -> NO_ACCESS;
            case 400, 422 -> INVALID;
            default -> UNAVAILABLE;
        };
    }
}
