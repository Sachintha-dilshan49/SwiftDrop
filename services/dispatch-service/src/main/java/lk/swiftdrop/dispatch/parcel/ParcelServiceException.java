package lk.swiftdrop.dispatch.parcel;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

/** 502: parcel-service could not be reached or failed in a way the courier cannot fix. */
public class ParcelServiceException extends SwiftDropException {

    public ParcelServiceException() {
        super(HttpStatus.BAD_GATEWAY, "parcel-service-unavailable", "Parcel service unavailable",
                "The parcel status could not be updated right now. Try again in a moment.");
    }
}
