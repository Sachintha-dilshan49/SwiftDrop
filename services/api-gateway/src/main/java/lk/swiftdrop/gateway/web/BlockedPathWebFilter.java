package lk.swiftdrop.gateway.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import lk.swiftdrop.gateway.error.ProblemResponseWriter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

/**
 * Answers paths that must not exist from outside with a 404 ProblemDetail, before security runs,
 * so they look missing rather than protected:
 *
 * <ul>
 *   <li>{@code /internal/**} and {@code /api/v1/internal/**} (service-to-service only, never routed)</li>
 *   <li>{@code /actuator/**} except {@code /actuator/health} and {@code /actuator/health/**}</li>
 * </ul>
 *
 * <p>The decoded and normalized path is checked too, so {@code /api/v1/parcels/../../internal/x}
 * or {@code /%69nternal/x} are blocked as well.
 */
@Component
public class BlockedPathWebFilter implements WebFilter, Ordered {

    public static final int ORDER = RequestIdWebFilter.ORDER + 1;

    private static final String NOT_FOUND_DETAIL = "No resource found at this path.";

    private final ProblemResponseWriter problems;

    public BlockedPathWebFilter(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String rawPath = exchange.getRequest().getPath().value();
        if (isBlocked(rawPath) || isBlocked(normalize(rawPath))) {
            return problems.write(exchange, HttpStatus.NOT_FOUND, NOT_FOUND_DETAIL);
        }
        return chain.filter(exchange);
    }

    static boolean isBlocked(String path) {
        if (path == null) {
            return false;
        }
        if (underPrefix(path, "/internal") || underPrefix(path, "/api/v1/internal")) {
            return true;
        }
        return underPrefix(path, "/actuator") && !underPrefix(path, "/actuator/health");
    }

    /**
     * Roughly what a servlet container does before mapping: drops {@code ;params}, decodes
     * percent-escapes, collapses duplicate slashes and resolves dot segments.
     */
    static String normalize(String rawPath) {
        try {
            String decoded = UriUtils.decode(rawPath.replaceAll(";[^/]*", ""), StandardCharsets.UTF_8)
                    .replaceAll("/{2,}", "/");
            String normalized = new URI(null, null, decoded, null).normalize().getPath();
            while (normalized.startsWith("/../")) {
                normalized = normalized.substring(3);
            }
            return normalized;
        } catch (IllegalArgumentException | URISyntaxException e) {
            return rawPath;
        }
    }

    private static boolean underPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
