package lk.swiftdrop.dispatch.parcel;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls parcel-service, the only service allowed to change a parcel's status. */
@Component
public class ParcelServiceClient {

    private final RestClient restClient;

    public ParcelServiceClient(@Qualifier("parcelServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Asks parcel-service to move the parcel, authenticated with the internal token (see {@code RestClientConfig}).
     * parcel-service's 404 and 409 arrive as {@code NotFoundException} and {@code ConflictException} with its own
     * problem type and detail; 5xx, timeouts and refused connections as {@code ServiceUnavailableException} (503).
     */
    public void updateStatus(UUID parcelId, ParcelStatusUpdate update) {
        restClient.patch()
                .uri("/internal/parcels/{id}/status", parcelId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .retrieve()
                .toBodilessEntity();
    }
}
