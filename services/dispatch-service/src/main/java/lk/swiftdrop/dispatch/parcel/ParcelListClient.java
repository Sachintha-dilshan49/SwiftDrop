package lk.swiftdrop.dispatch.parcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reads parcel lists from parcel-service's public API on behalf of the signed-in dispatcher. */
@Component
public class ParcelListClient {

    /** parcel-service caps a page at 100. */
    static final int PAGE_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(ParcelListClient.class);

    private final RestClient restClient;

    public ParcelListClient(@Qualifier("parcelServiceUserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** The oldest CREATED parcels, up to {@value #PAGE_SIZE}; parcel-service checks the caller's role itself. */
    public List<WaitingParcel> oldestCreatedParcels(String dispatcherToken) {
        try {
            ParcelPage page = restClient.get()
                    .uri("/api/v1/parcels?status=CREATED&sort=createdAt,asc&size={size}", PAGE_SIZE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + dispatcherToken)
                    .retrieve()
                    .body(ParcelPage.class);
            return page == null || page.content() == null ? List.of() : page.content();
        } catch (RestClientException e) {
            // never log the response body: the parcel list holds addresses and phone numbers
            log.warn("parcel-service parcel list failed: {}", e.getClass().getSimpleName());
            throw new ServiceUnavailableException(
                    "parcel-service is not available right now. Please try again later.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ParcelPage(List<WaitingParcel> content) {
    }
}
