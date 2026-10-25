package lk.swiftdrop.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lk.swiftdrop.common.error.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 401 and 403 as ProblemDetail, both for {@code @PreAuthorize} failures inside controllers and,
 * through {@link ProblemDetailAuthenticationEntryPoint} / {@link ProblemDetailAccessDeniedHandler},
 * for failures in the security filter chain. Security exception messages are never returned.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class SecurityExceptionHandler {

    static final String UNAUTHORIZED_DETAIL = "A valid access token is required.";
    static final String FORBIDDEN_DETAIL = "You are not allowed to perform this action.";

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return unauthorized(request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // An anonymous caller denied by @PreAuthorize on a public path needs to log in: 401, not 403
        if (trustResolver.isAnonymous(SecurityContextHolder.getContext().getAuthentication())) {
            return unauthorized(request);
        }
        return respond(ProblemDetails.of(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", FORBIDDEN_DETAIL), request);
    }

    private static ResponseEntity<ProblemDetail> unauthorized(HttpServletRequest request) {
        return respond(ProblemDetails.of(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", UNAUTHORIZED_DETAIL),
                request);
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem, HttpServletRequest request) {
        ProblemDetails.complete(problem, request.getRequestURI());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
