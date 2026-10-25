package lk.swiftdrop.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception into an RFC 7807 {@link ProblemDetail}. Spring MVC's own exceptions are
 * handled by {@link ResponseEntityExceptionHandler}; their bodies are completed with a SwiftDrop
 * {@code type}, {@code instance} and {@code requestId}. Unknown exceptions become a generic 500:
 * their message is logged (masked), never returned.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String VALIDATION_DETAIL = "One or more fields are invalid.";
    static final String INTERNAL_DETAIL = "An unexpected error occurred. Quote the requestId when reporting it.";

    @ExceptionHandler(SwiftDropException.class)
    public ResponseEntity<ProblemDetail> handleSwiftDrop(SwiftDropException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.of(ex.getStatus(), ex.getProblemType(), ex.getTitle(), ex.getMessage());
        if (ex instanceof ValidationException validation) {
            ProblemDetails.setErrors(problem, validation.getErrors());
        }
        return respond(problem, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal server error", INTERNAL_DETAIL);
        return respond(problem, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.add(new FieldErrorDetail(error.getObjectName(), error.getDefaultMessage())));
        return handleExceptionInternal(ex, validationProblem(status, errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                for (FieldError error : parameterErrors.getFieldErrors()) {
                    errors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage()));
                }
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    errors.add(new FieldErrorDetail(parameter, error.getDefaultMessage()));
                }
            }
        }
        return handleExceptionInternal(ex, validationProblem(status, errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            ProblemDetails.complete(problem, requestPath(request));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    static ProblemDetail validationProblem(HttpStatusCode status, List<FieldErrorDetail> errors) {
        ProblemDetail problem = ProblemDetails.of(status, ProblemDetails.VALIDATION_ERROR,
                ProblemDetails.VALIDATION_TITLE, VALIDATION_DETAIL);
        ProblemDetails.setErrors(problem, errors);
        return problem;
    }

    static ResponseEntity<ProblemDetail> respond(ProblemDetail problem, HttpServletRequest request) {
        ProblemDetails.complete(problem, request.getRequestURI());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static String requestPath(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
    }
}
