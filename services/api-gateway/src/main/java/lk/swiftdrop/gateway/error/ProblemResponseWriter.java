package lk.swiftdrop.gateway.error;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lk.swiftdrop.common.error.ProblemDetails;
import lk.swiftdrop.gateway.web.RequestIdWebFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes {@code application/problem+json} bodies in the shape from docs/conventions.md:
 * {@code type}, {@code title}, {@code status}, {@code detail}, {@code instance} (request path) and {@code requestId}.
 *
 * <p>Every error the gateway itself produces goes through here, so bodies never carry exception
 * messages, hostnames or ports. Existing response headers (request ID, rate-limit headers) are kept.
 */
@Component
public class ProblemResponseWriter {

    private final JsonMapper jsonMapper;

    public ProblemResponseWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        ProblemDetail problem = ProblemDetails.of(status, slug(status), status.getReasonPhrase(), detail);

        // Flat map, not ProblemDetail itself, so the JSON shape does not depend on Jackson mix-ins
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", problem.getType().toString());
        body.put("title", problem.getTitle());
        body.put("status", problem.getStatus());
        body.put("detail", problem.getDetail());
        body.put("instance", exchange.getRequest().getPath().value());
        String requestId = exchange.getAttribute(RequestIdWebFilter.ATTRIBUTE);
        if (requestId != null) {
            body.put(ProblemDetails.REQUEST_ID, requestId);
        }

        byte[] bytes = jsonMapper.writeValueAsBytes(body);
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /** {@code 503} becomes {@code service-unavailable}, matching common's slugs. */
    static String slug(HttpStatus status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
