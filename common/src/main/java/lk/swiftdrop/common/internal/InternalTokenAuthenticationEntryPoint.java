package lk.swiftdrop.common.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Writes the internal chain's 401 through Spring MVC's exception resolvers, so the body is the
 * shared ProblemDetail with {@code requestId}. No {@code WWW-Authenticate: Bearer}: a JWT does not help here.
 */
public class InternalTokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public InternalTokenAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        resolver.resolveException(request, response, null, new InternalTokenRequiredException());
    }
}
