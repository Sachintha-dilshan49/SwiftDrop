package lk.swiftdrop.gateway.ratelimit;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate-limit bucket per client IP for the public fee estimate: {@code pricing-estimate:<sha256 hex of IP>}.
 *
 * <p>Trusts exactly one proxy hop: the last {@code X-Forwarded-For} entry, which nginx appends.
 * Entries a client adds itself come earlier and are ignored, so spoofing the header does not
 * give a fresh bucket. Without the header (Vite dev calling :8080 directly) the remote address is used.
 */
@Component("clientIpKeyResolver")
public class ClientIpKeyResolver implements KeyResolver {

    private final RemoteAddressResolver addressResolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        InetSocketAddress address = addressResolver.resolve(exchange);
        if (address == null) {
            return Mono.empty();
        }
        InetAddress inet = address.getAddress();
        String ip = inet != null ? inet.getHostAddress() : address.getHostString();
        if (ip == null || ip.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(RateLimitKeys.PRICING_ESTIMATE_PREFIX + RateLimitKeys.sha256Hex(ip));
    }
}
