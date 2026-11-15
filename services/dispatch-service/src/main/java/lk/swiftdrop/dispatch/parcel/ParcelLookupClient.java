package lk.swiftdrop.dispatch.parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads parcels from parcel-service's public API on behalf of the signed-in dispatcher, whose own JWT is sent so
 * parcel-service applies its usual role checks.
 */
@Component
public class ParcelLookupClient {

    /** parcel-service caps a page at 100. */
    static final int PAGE_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(ParcelLookupClient.class);

    private final RestClient restClient;

    public ParcelLookupClient(@Qualifier("parcelServiceUserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** The oldest CREATED parcels, up to {@value #PAGE_SIZE}. */
    public List<WaitingParcel> oldestCreatedParcels(String dispatcherToken) {
        try {
            ParcelPage page = restClient.get()
                    .uri("/api/v1/parcels?status=CREATED&sort=createdAt,asc&size={size}", PAGE_SIZE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + dispatcherToken)
                    .retrieve()
                    .body(ParcelPage.class);
            return page == null || page.content() == null ? List.of() : page.content();
        } catch (RestClientException e) {
            throw unavailable("parcel list", e);
        }
    }

    /** One parcel with the details a courier needs; 404 if parcel-service does not know it. */
    public AssignableParcel getParcel(UUID parcelId, String dispatcherToken) {
        try {
            AssignableParcel parcel = restClient.get()
                    .uri("/api/v1/parcels/{id}", parcelId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + dispatcherToken)
                    .retrieve()
                    .body(AssignableParcel.class);
            if (parcel == null) {
                throw unavailable("parcel details", null);
            }
            return parcel;
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("parcel-not-found", "Parcel not found",
                    "Parcel " + parcelId + " does not exist.");
        } catch (RestClientException e) {
            throw unavailable("parcel details", e);
        }
    }

    private static ServiceUnavailableException unavailable(String what, Exception cause) {
        // never log the response body: parcels hold addresses and phone numbers
        log.warn("parcel-service {} failed: {}", what, cause == null ? "empty body" : cause.getClass().getSimpleName());
        return new ServiceUnavailableException("parcel-service is not available right now. Please try again later.",
                cause);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ParcelPage(List<WaitingParcel> content) {
    }
}
