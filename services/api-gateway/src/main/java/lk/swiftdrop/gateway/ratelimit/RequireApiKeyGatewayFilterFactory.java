package lk.swiftdrop.gateway.ratelimit;

import lk.swiftdrop.gateway.error.ProblemResponseWriter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Route filter {@code RequireApiKey}: rejects requests without a non-blank {@code X-API-Key}
 * with a 401 ProblemDetail, before rate limiting and without forwarding anything.
 * The key itself is verified by merchant-service.
 */
@Component
public class RequireApiKeyGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    static final String DETAIL = "Missing X-API-Key header";

    private final ProblemResponseWriter problems;

    public RequireApiKeyGatewayFilterFactory(ProblemResponseWriter problems) {
        super(Object.class);
        this.problems = problems;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(MerchantApiKeyResolver.HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                return problems.write(exchange, HttpStatus.UNAUTHORIZED, DETAIL);
            }
            return chain.filter(exchange);
        };
    }
}
