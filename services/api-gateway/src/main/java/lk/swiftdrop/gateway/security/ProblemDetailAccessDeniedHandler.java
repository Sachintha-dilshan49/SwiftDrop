package lk.swiftdrop.gateway.security;

import lk.swiftdrop.gateway.error.ProblemResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 403 ProblemDetail. The gateway makes no role decisions, so this only covers framework denials;
 * role and ownership checks are in the services.
 */
@Component
public class ProblemDetailAccessDeniedHandler implements ServerAccessDeniedHandler {

    static final String DETAIL = "You do not have permission to access this resource.";

    private final ProblemResponseWriter problems;

    public ProblemDetailAccessDeniedHandler(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return problems.write(exchange, HttpStatus.FORBIDDEN, DETAIL);
    }
}
