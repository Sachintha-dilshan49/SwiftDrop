package lk.swiftdrop.common.internal;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

/** 401 for {@code /internal/**} without a valid {@code X-Internal-Token}. */
public class InternalTokenRequiredException extends SwiftDropException {

    static final String DETAIL = "A valid " + InternalTokenAuthenticationFilter.HEADER + " header is required.";

    public InternalTokenRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", DETAIL);
    }
}
