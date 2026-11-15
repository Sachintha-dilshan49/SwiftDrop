package lk.swiftdrop.gateway.ratelimit;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate-limit key for the AI assistant's chat: the signed-in user (the JWT subject), hashed. People on one network
 * share an IP address but not a limit. Without a user the key is empty and the limiter refuses the request; the
 * chat routes need a JWT anyway.
 */
@Component("signedInUserKeyResolver")
public class SignedInUserKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .filter(name -> !name.isBlank())
                .map(name -> RateLimitKeys.ASSISTANT_CHAT_PREFIX + RateLimitKeys.sha256Hex(name));
    }
}
