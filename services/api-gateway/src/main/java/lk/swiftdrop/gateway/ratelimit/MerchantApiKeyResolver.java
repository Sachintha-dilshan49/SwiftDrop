package lk.swiftdrop.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate-limit bucket per merchant API key: {@code merchant-api:<sha256 hex of X-API-Key>}.
 * The gateway does not validate the key; merchant-service does.
 *
 * <p>{@code @Primary} because the RequestRateLimiter factory needs one default resolver; with no
 * key header it resolves to nothing and the request is denied.
 */
@Primary
@Component("merchantApiKeyResolver")
public class MerchantApiKeyResolver implements KeyResolver {

    public static final String HEADER = "X-API-Key";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(RateLimitKeys.MERCHANT_API_PREFIX + RateLimitKeys.sha256Hex(apiKey.trim()));
    }
}
