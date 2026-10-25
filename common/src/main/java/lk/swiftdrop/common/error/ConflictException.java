package lk.swiftdrop.common.error;

import org.springframework.http.HttpStatus;

/**
 * 409: the request conflicts with the current state, e.g. an invalid status transition or a
 * duplicate email. Prefer the three-argument constructor with a specific problem type, e.g.
 * {@code new ConflictException("invalid-status-transition", "Invalid status transition", ...)}.
 */
public class ConflictException extends SwiftDropException {

    public ConflictException(String detail) {
        this("conflict", "Conflict", detail);
    }

    public ConflictException(String problemType, String title, String detail) {
        super(HttpStatus.CONFLICT, problemType, title, detail);
    }
}
