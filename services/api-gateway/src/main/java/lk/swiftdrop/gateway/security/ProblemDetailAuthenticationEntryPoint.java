package lk.swiftdrop.gateway.security;

import lk.swiftdrop.gateway.error.ProblemResponseWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 401 for a missing, expired or invalid token. The body and {@code WWW-Authenticate} header
 * never say which of those it was.
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    static final String DETAIL = "Authentication is required to access this resource.";

    private final ProblemResponseWriter problems;

    public ProblemDetailAuthenticationEntryPoint(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return problems.write(exchange, HttpStatus.UNAUTHORIZED, DETAIL);
    }
}
