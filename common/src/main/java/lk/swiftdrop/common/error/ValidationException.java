package lk.swiftdrop.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * 400: a validation rule that Bean Validation on the request DTO cannot express, e.g. a check
 * that needs data from another service. Bean Validation failures are mapped automatically.
 */
public class ValidationException extends SwiftDropException {

    private final List<FieldErrorDetail> errors;

    public ValidationException(String detail) {
        this(detail, List.of());
    }

    public ValidationException(String detail, List<FieldErrorDetail> errors) {
        super(HttpStatus.BAD_REQUEST, ProblemDetails.VALIDATION_ERROR, ProblemDetails.VALIDATION_TITLE, detail);
        this.errors = List.copyOf(errors);
    }

    public List<FieldErrorDetail> getErrors() {
        return errors;
    }
}
