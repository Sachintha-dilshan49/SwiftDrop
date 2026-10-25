package lk.swiftdrop.common.error;

import org.springframework.http.HttpStatus;

/** 404: the resource does not exist, or the caller is not allowed to know that it exists. */
public class NotFoundException extends SwiftDropException {

    public NotFoundException(String detail) {
        this("not-found", "Resource not found", detail);
    }

    public NotFoundException(String problemType, String title, String detail) {
        super(HttpStatus.NOT_FOUND, problemType, title, detail);
    }
}
