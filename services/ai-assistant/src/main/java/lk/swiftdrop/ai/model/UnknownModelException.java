package lk.swiftdrop.ai.model;

import java.util.List;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.ValidationException;

/** 400: the request named a model that is not configured or not enabled. */
public class UnknownModelException extends ValidationException {

    public UnknownModelException(String name) {
        super("Unknown or disabled model '" + name + "'. GET /api/v1/assistant/models lists the available ones.",
                List.of(new FieldErrorDetail("model", "is not an available model")));
    }
}
