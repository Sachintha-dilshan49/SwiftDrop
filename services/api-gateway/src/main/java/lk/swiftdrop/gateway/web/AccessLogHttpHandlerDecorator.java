package lk.swiftdrop.gateway.web;

import lk.swiftdrop.common.web.RequestIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

/**
 * One access-log line per request: method, path, status, duration and request ID.
 *
 * <p>Wraps the whole HTTP handler, not just the filter chain, so it sees the final status after
 * error handlers have run, and the full duration of streamed (SSE) responses. Only the path is
 * logged: never the query string, {@code Authorization}, {@code X-API-Key} or cookies.
 */
@Component
public class AccessLogHttpHandlerDecorator implements HttpHandlerDecoratorFactory {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("lk.swiftdrop.gateway.access");

    @Override
    public HttpHandler apply(HttpHandler delegate) {
        return (request, response) -> {
            long start = System.nanoTime();
            return delegate.handle(request, response)
                    .doFinally(signal -> log(request, response, start));
        };
    }

    static void log(ServerHttpRequest request, ServerHttpResponse response, long startNanos) {
        long millis = (System.nanoTime() - startNanos) / 1_000_000;
        HttpStatusCode status = response.getStatusCode();
        String requestId = response.getHeaders().getFirst(RequestIds.HEADER);
        // MDC only around our own log call: reactive code hops threads, so nothing is left behind
        if (requestId != null) {
            MDC.put(RequestIds.MDC_KEY, requestId);
        }
        try {
            ACCESS_LOG.info("{} {} {} {}ms", request.getMethod(), request.getPath().value(),
                    status == null ? 200 : status.value(), millis);
        } finally {
            MDC.remove(RequestIds.MDC_KEY);
        }
    }
}
