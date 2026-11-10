package lk.swiftdrop.notification.client;

import lk.swiftdrop.common.error.SwiftDropException;
import lk.swiftdrop.notification.client.dto.RecipientContactResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for parcel-service internal endpoint.
 *
 * GET /internal/parcels/{id}/contacts → RecipientContactResponse
 *   recipientName, recipientPhone, recipientEmail (null for customer bookings)
 *   404 if parcel not found
 *   401 if X-Internal-Token is wrong
 *
 * Called by NotificationService to get the physical recipient's contact details,
 * which differ from the customer who booked the parcel (architecture §6.3):
 *   - Customer booking: SMS recipient from here; email customer from auth-service.
 *   - Merchant order:   email + SMS recipient from here.
 *
 * NOTE: GET /internal/parcels/{id}/contacts is not yet on dev.
 * Role 3 will tag when it is merged. Until then, calls return Optional.empty()
 * (NotFoundException mapped by InternalRestClientFactory), which NotificationService
 * treats as "no recipient contact available" — customer booking email still goes
 * through auth-service as before.
 */
@Component
public class ParcelServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ParcelServiceClient.class);
    private final RestClient restClient;

    public ParcelServiceClient(@Qualifier("parcelServiceClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetches the recipient's contact details for the given parcel.
     * Returns empty if parcel not found (404) or parcel-service is unavailable.
     */
    public Optional<RecipientContactResponse> getRecipientContacts(UUID parcelId) {
        try {
            RecipientContactResponse response = restClient.get()
                    .uri("/internal/parcels/{id}/contacts", parcelId)
                    .retrieve()
                    .body(RecipientContactResponse.class);
            return Optional.ofNullable(response);
        } catch (SwiftDropException e) {
            log.warn("Failed to fetch recipient contacts for parcel {}: {}", parcelId, e.getMessage());
            return Optional.empty();
        }
    }
}

