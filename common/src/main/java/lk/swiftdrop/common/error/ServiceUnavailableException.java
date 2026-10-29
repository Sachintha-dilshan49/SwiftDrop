package lk.swiftdrop.common.error;

import org.springframework.http.HttpStatus;

/** 503: a service this request depends on timed out, could not be reached or failed with 5xx. */
public class ServiceUnavailableException extends SwiftDropException {

    public ServiceUnavailableException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "service-unavailable", "Service unavailable", detail);
    }

    public ServiceUnavailableException(String detail, Throwable cause) {
        this(detail);
        initCause(cause);
    }
}
