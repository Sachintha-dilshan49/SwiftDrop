package lk.swiftdrop.parcel.tracking.service;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

/** 429: the live tracking connection limit for a tracking number or for this instance is reached. */
public class TooManySubscribersException extends SwiftDropException {

    public TooManySubscribersException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "too-many-subscribers", "Too many live tracking connections",
                "Too many live tracking connections are open. Please try again later.");
    }
}
