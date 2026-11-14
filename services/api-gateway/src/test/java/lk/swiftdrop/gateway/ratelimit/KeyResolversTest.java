package lk.swiftdrop.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class KeyResolversTest {

    private final MerchantApiKeyResolver merchantResolver = new MerchantApiKeyResolver();
    private final ClientIpKeyResolver ipResolver = new ClientIpKeyResolver();
    private final SignedInUserKeyResolver userResolver = new SignedInUserKeyResolver();

    @Test
    void merchantKeyIsPrefixedSha256HexOfTheApiKey() {
        String key = merchantResolver.resolve(exchange(MockServerHttpRequest.post("/api/v1/merchant-api/orders")
                .header("X-API-Key", "abc"))).block();

        // sha256("abc")
        assertThat(key).isEqualTo("merchant-api:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void merchantKeyNeverContainsTheRawKey() {
        String key = merchantResolver.resolve(exchange(MockServerHttpRequest.post("/x")
                .header("X-API-Key", "sd_live_secret"))).block();

        assertThat(key).doesNotContain("sd_live_secret").startsWith("merchant-api:").hasSize(13 + 64);
    }

    @Test
    void merchantKeyIsEmptyWithoutOrWithBlankHeader() {
        assertThat(merchantResolver.resolve(exchange(MockServerHttpRequest.post("/x"))).blockOptional()).isEmpty();
        assertThat(merchantResolver.resolve(exchange(MockServerHttpRequest.post("/x").header("X-API-Key", "  ")))
                .blockOptional()).isEmpty();
    }

    @Test
    void ipKeyUsesTheLastForwardedForEntry() {
        String direct = ipResolver.resolve(exchange(MockServerHttpRequest.get("/x")
                .header("X-Forwarded-For", "203.0.113.5"))).block();
        String spoofed = ipResolver.resolve(exchange(MockServerHttpRequest.get("/x")
                .header("X-Forwarded-For", "10.9.9.9, 203.0.113.5"))).block();

        assertThat(direct).startsWith("pricing-estimate:").doesNotContain("203.0.113.5");
        assertThat(spoofed).isEqualTo(direct);
    }

    @Test
    void ipKeyFallsBackToTheRemoteAddress() {
        String fromRemote = ipResolver.resolve(exchange(MockServerHttpRequest.get("/x")
                .remoteAddress(new InetSocketAddress("198.51.100.4", 50000)))).block();
        String fromHeader = ipResolver.resolve(exchange(MockServerHttpRequest.get("/x")
                .header("X-Forwarded-For", "198.51.100.4"))).block();

        assertThat(fromRemote).isEqualTo(fromHeader);
    }

    @Test
    void ipKeyIsEmptyWithoutAnyAddress() {
        assertThat(ipResolver.resolve(exchange(MockServerHttpRequest.get("/x"))).blockOptional()).isEmpty();
    }

    @Test
    void sameValueGivesDifferentKeysPerLimiter() {
        String merchant = merchantResolver.resolve(exchange(MockServerHttpRequest.post("/x")
                .header("X-API-Key", "198.51.100.4"))).block();
        String ip = ipResolver.resolve(exchange(MockServerHttpRequest.get("/x")
                .header("X-Forwarded-For", "198.51.100.4"))).block();

        assertThat(merchant).isNotEqualTo(ip);
    }

    @Test
    void userKeyIsPrefixedSha256HexOfTheSubject() {
        String key = userResolver.resolve(signedIn("abc")).block();

        // sha256("abc"), so user IDs never reach Redis or logs
        assertThat(key).isEqualTo("assistant-chat:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void userKeyIsEmptyWithoutASignedInUser() {
        assertThat(userResolver.resolve(exchange(MockServerHttpRequest.post("/x"))).blockOptional()).isEmpty();
        assertThat(userResolver.resolve(signedIn(" ")).blockOptional()).isEmpty();
    }

    private static ServerWebExchange signedIn(String subject) {
        Principal principal = () -> subject;
        return exchange(MockServerHttpRequest.post("/api/v1/assistant/chat")).mutate()
                .principal(Mono.just(principal))
                .build();
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest.BaseBuilder<?> request) {
        return MockServerWebExchange.from(request);
    }
}
