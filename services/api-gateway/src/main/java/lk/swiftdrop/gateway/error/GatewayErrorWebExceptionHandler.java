package lk.swiftdrop.gateway.error;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import lk.swiftdrop.common.web.RequestIds;
import lk.swiftdrop.gateway.web.RequestIdWebFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

/**
 * Turns every error raised inside the gateway into a ProblemDetail. Ordered before Boot's
 * default handler ({@code -1}).
 *
 * <ul>
 *   <li>No route: 404.</li>
 *   <li>Downstream unreachable (connection refused, connect timeout, unknown host, closed early): 503.</li>
 *   <li>Downstream response timeout: 504.</li>
 *   <li>Rate limit exceeded: 429, keeping the {@code X-RateLimit-*} headers.</li>
 *   <li>Anything else: 500.</li>
 * </ul>
 *
 * Bodies use fixed texts: no exception messages, hostnames, ports or stack traces. Responses
 * that a service sent (including its own ProblemDetails) are not errors here and pass through untouched.
 */
@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

    static final String NOT_FOUND = "No resource found at this path.";
    static final String UNAVAILABLE = "Service temporarily unavailable";
    static final String TIMEOUT = "The service did not respond in time.";
    static final String TOO_MANY_REQUESTS = "Too many requests. Try again later.";
    static final String UNEXPECTED = "An unexpected error occurred.";

    private final ProblemResponseWriter problems;

    public GatewayErrorWebExceptionHandler(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        HttpStatus status = statusFor(ex);
        if (status.is5xxServerError()) {
            logError(exchange, status, ex);
        }
        return problems.write(exchange, status, detailFor(status));
    }

    static HttpStatus statusFor(Throwable ex) {
        if (isUnreachable(ex)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        HttpStatusCode code = null;
        if (ex instanceof ResponseStatusException rse) {
            code = rse.getStatusCode();
        } else if (ex instanceof HttpStatusCodeException hse) {
            // RequestRateLimiter with throw-on-limit raises a 429 HttpClientErrorException
            code = hse.getStatusCode();
        }
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        HttpStatus resolved = HttpStatus.resolve(code.value());
        return resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
    }

    static String detailFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> NOT_FOUND;
            case SERVICE_UNAVAILABLE -> UNAVAILABLE;
            case GATEWAY_TIMEOUT -> TIMEOUT;
            case TOO_MANY_REQUESTS -> TOO_MANY_REQUESTS;
            case INTERNAL_SERVER_ERROR -> UNEXPECTED;
            default -> status.getReasonPhrase() + ".";
        };
    }

    /** True if the cause chain shows the downstream could not be reached or dropped the connection. */
    static boolean isUnreachable(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof ConnectException // includes Netty's ConnectTimeoutException
                    || t instanceof UnknownHostException
                    || t instanceof UnresolvedAddressException
                    || t instanceof PrematureCloseException) {
                return true;
            }
        }
        return false;
    }

    private static void logError(ServerWebExchange exchange, HttpStatus status, Throwable ex) {
        String requestId = exchange.getAttribute(RequestIdWebFilter.ATTRIBUTE);
        if (requestId != null) {
            MDC.put(RequestIds.MDC_KEY, requestId);
        }
        try {
            if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("Unhandled gateway error on {} {}", exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath().value(), ex);
            } else {
                log.warn("Downstream {} on {} {}: {}", status.value(), exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath().value(), ex.getClass().getSimpleName());
            }
        } finally {
            MDC.remove(RequestIds.MDC_KEY);
        }
    }
}
