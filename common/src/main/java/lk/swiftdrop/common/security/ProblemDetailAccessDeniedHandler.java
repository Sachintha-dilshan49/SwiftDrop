package lk.swiftdrop.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** 403 counterpart of {@link ProblemDetailAuthenticationEntryPoint}. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler bearer = new BearerTokenAccessDeniedHandler();
    private final HandlerExceptionResolver resolver;

    public ProblemDetailAccessDeniedHandler(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException, ServletException {
        bearer.handle(request, response, ex);
        resolver.resolveException(request, response, null, ex);
    }
}
