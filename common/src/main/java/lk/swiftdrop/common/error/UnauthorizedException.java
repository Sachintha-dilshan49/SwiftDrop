package lk.swiftdrop.common.error;

import org.springframework.http.HttpStatus;

/** 401: authentication credentials are missing, invalid, or expired. */
public class UnauthorizedException extends SwiftDropException {

    public UnauthorizedException(String detail) {
        this("unauthorized", "Unauthorized", detail);
    }

    public UnauthorizedException(String problemType, String title, String detail) {
        super(HttpStatus.UNAUTHORIZED, problemType, title, detail);
    }
}
