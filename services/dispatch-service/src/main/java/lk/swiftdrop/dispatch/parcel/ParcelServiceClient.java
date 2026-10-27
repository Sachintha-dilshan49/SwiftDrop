package lk.swiftdrop.dispatch.parcel;

import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls parcel-service, the only service allowed to change a parcel's status. */
@Component
public class ParcelServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ParcelServiceClient.class);

    private final RestClient restClient;

    public ParcelServiceClient(@Qualifier("parcelServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Asks parcel-service to move the parcel. The caller's access token is forwarded so parcel-service
     * can authorize the call and record who made the change.
     */
    public void updateStatus(UUID parcelId, ParcelStatusUpdate update, String accessToken) {
        try {
            restClient.patch()
                    .uri("/internal/parcels/{id}/status", parcelId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(update)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException("invalid-status-transition", "Invalid status transition",
                    "Parcel " + parcelId + " cannot move to " + update.status() + " from its current status.");
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("parcel-not-found", "Parcel not found",
                    "Parcel " + parcelId + " does not exist.");
        } catch (RestClientException e) {
            // never log the response body: it may echo personal data from the request
            log.error("parcel-service status update failed for parcel {}: {}", parcelId, e.getClass().getSimpleName());
            throw new ParcelServiceException();
        }
    }
}
