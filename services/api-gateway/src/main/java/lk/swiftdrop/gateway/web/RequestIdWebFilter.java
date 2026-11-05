package lk.swiftdrop.gateway.web;

import lk.swiftdrop.common.web.RequestIds;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * First filter of every request, before security, so 401/404 responses carry the ID too.
 *
 * <p>The ID is the incoming {@code X-Request-Id} if it is safe, otherwise a new one
 * ({@link RequestIds#sanitizeOrGenerate}). It replaces the header on the forwarded request,
 * is set on the response and is stored as the exchange attribute {@link #ATTRIBUTE}.
 */
@Component
public class RequestIdWebFilter implements WebFilter, Ordered {

    public static final String ATTRIBUTE = RequestIdWebFilter.class.getName() + ".requestId";
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = RequestIds.sanitizeOrGenerate(exchange.getRequest().getHeaders().getFirst(RequestIds.HEADER));

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(RequestIds.HEADER, requestId))
                .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();
        mutated.getAttributes().put(ATTRIBUTE, requestId);

        // Set just before the headers are sent, so a downstream X-Request-Id can not end up next to ours
        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set(RequestIds.HEADER, requestId);
            return Mono.empty();
        });
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
