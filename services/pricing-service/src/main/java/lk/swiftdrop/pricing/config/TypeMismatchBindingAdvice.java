package lk.swiftdrop.pricing.config;

import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Query parameters that cannot be converted, e.g. {@code size=HUGE} or {@code weightKg=abc}, become a field
 * error a client can read. Spring's default message names Java types and packages, which the shared handler
 * would pass on unchanged.
 */
@ControllerAdvice
public class TypeMismatchBindingAdvice {

    @InitBinder
    void useReadableTypeMismatchMessages(WebDataBinder binder) {
        binder.setBindingErrorProcessor(new ReadableTypeMismatchProcessor());
    }

    static final class ReadableTypeMismatchProcessor extends DefaultBindingErrorProcessor {

        @Override
        public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
            if (!(ex instanceof TypeMismatchException mismatch)) {
                super.processPropertyAccessException(ex, bindingResult);
                return;
            }
            String field = ex.getPropertyName();
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), field, ex.getValue(), true,
                    bindingResult.resolveMessageCodes(ex.getErrorCode(), field), null, message(mismatch)));
        }

        static String message(TypeMismatchException mismatch) {
            Class<?> type = mismatch.getRequiredType();
            if (type != null && type.isEnum()) {
                return "must be one of " + Arrays.stream(type.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
            }
            if (type != null && Number.class.isAssignableFrom(type)) {
                return "must be a number";
            }
            return "has an invalid value";
        }
    }
}
