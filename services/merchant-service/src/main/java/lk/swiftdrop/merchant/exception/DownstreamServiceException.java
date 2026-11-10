package lk.swiftdrop.merchant.exception;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

public class DownstreamServiceException extends SwiftDropException {

    public DownstreamServiceException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "downstream-unavailable", "Downstream service unavailable", detail);
    }
}
