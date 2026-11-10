package lk.swiftdrop.merchant.client;

import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.merchant.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ParcelServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ParcelServiceClient.class);

    private final RestClient parcelServiceRestClient;

    public ParcelServiceClient(RestClient parcelServiceRestClient) {
        this.parcelServiceRestClient = parcelServiceRestClient;
    }

    public CreateParcelInternalResponse createParcel(CreateParcelInternalRequest request) {
        try {
            log.info("Calling parcel-service to create parcel for merchantId: {}", request.merchantId());
            return parcelServiceRestClient.post()
                    .uri("/internal/parcels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CreateParcelInternalResponse.class);
        } catch (HttpClientErrorException.BadRequest e) {
            String detail = extractDetail(e);
            log.warn("Parcel service rejected parcel creation with 400: {}", detail);
            throw new ValidationException(detail);
        } catch (ServiceUnavailableException e) {
            log.error("Parcel service or pricing service unavailable: {}", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection failure calling parcel-service: {}", e.getMessage());
            throw new DownstreamServiceException("Parcel service is currently unavailable or timed out. Please try again later.");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                String detail = extractDetail(e);
                log.warn("Parcel service rejected parcel creation with 400: {}", detail);
                throw new ValidationException(detail);
            }
            log.error("Parcel service responded with status {}: {}", e.getStatusCode(), e.getStatusText());
            throw new DownstreamServiceException("Failed to create parcel in parcel service: downstream error");
        } catch (Exception e) {
            log.error("Unexpected error calling parcel-service: {}", e.getMessage(), e);
            throw new DownstreamServiceException("Unable to communicate with parcel service");
        }
    }

    private String extractDetail(RestClientResponseException e) {
        try {
            ProblemDetail problem = e.getResponseBodyAs(ProblemDetail.class);
            if (problem != null && problem.getDetail() != null && !problem.getDetail().isBlank()) {
                return problem.getDetail();
            }
        } catch (Exception ignored) {
            // fallback
        }
        String body = e.getResponseBodyAsString();
        return (body != null && !body.isBlank()) ? body : "Invalid parcel order request";
    }
}
