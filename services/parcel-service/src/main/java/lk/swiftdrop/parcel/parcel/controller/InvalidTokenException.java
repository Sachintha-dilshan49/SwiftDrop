package lk.swiftdrop.parcel.parcel.controller;

import lk.swiftdrop.common.error.SwiftDropException;
import org.springframework.http.HttpStatus;

/** 401: the token is signed correctly but does not identify a user the way this service needs. */
public class InvalidTokenException extends SwiftDropException {

    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, "invalid-token", "Invalid token",
                "The access token does not identify a user.");
    }
}
