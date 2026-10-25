package lk.swiftdrop.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Sends the standard {@code WWW-Authenticate: Bearer} header, then lets Spring MVC's exception
 * resolvers write the body, so the 401 goes through {@link SecurityExceptionHandler} like every
 * other error instead of hand-built JSON.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
    private final HandlerExceptionResolver resolver;

    public ProblemDetailAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException, ServletException {
        bearer.commence(request, response, ex);
        resolver.resolveException(request, response, null, ex);
    }
}
