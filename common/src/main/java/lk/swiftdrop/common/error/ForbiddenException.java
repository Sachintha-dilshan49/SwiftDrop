package lk.swiftdrop.common.error;

import org.springframework.http.HttpStatus;

/** 403: authenticated, but not allowed, e.g. a courier acting on another courier's assignment. */
public class ForbiddenException extends SwiftDropException {

    public ForbiddenException(String detail) {
        this("forbidden", "Forbidden", detail);
    }

    public ForbiddenException(String problemType, String title, String detail) {
        super(HttpStatus.FORBIDDEN, problemType, title, detail);
    }
}
