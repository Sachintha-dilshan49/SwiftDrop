package lk.swiftdrop.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lk.swiftdrop.gateway.support.AbstractGatewayIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Merchant API (per API key) and public fee estimate (per client IP). Burst is
 * {@link #RATE_LIMIT_BURST} with 1 token/s, so {@code burst + 2} quick requests always hit the limit.
 */
class RateLimitIT extends AbstractGatewayIT {

    private static final String ORDERS = "/api/v1/merchant-api/orders";
    private static final String ESTIMATE = "/api/v1/pricing/estimate?fromZone=A&toZone=B&weightKg=1";

    @Autowired
    private ReactiveStringRedisTemplate redis;

    // â”€â”€â”€ Merchant API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    // Whitespace-only values are covered by RequireApiKeyGatewayFilterFactoryTest; Netty's client refuses to send them
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void merchantApiWithoutKeyIsUnauthorizedAndNotForwarded(boolean sendEmptyHeader) {
        WebTestClient.RequestBodySpec request = client.post().uri(ORDERS);
        if (sendEmptyHeader) {
            request.header("X-API-Key", "");
        }
        request.exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/unauthorized")
                .jsonPath("$.detail").isEqualTo("Missing X-API-Key header")
                .jsonPath("$.instance").isEqualTo(ORDERS);

        service("merchant").verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void merchantApiWithKeyIsForwardedWithoutJwt() {
        client.post().uri(ORDERS)
                .header("X-API-Key", "sd_live_abc123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(STUB_SERVICE_HEADER, "merchant")
                .expectHeader().exists("X-RateLimit-Remaining");

        service("merchant").verify(postRequestedFor(urlEqualTo(ORDERS))
                .withHeader("X-API-Key", equalTo("sd_live_abc123"))
                .withoutHeader("Authorization"));
    }

    @Test
    void merchantApiOverBurstIsTooManyRequestsProblem() {
        List<HttpStatus> statuses = merchantBurst("sd_live_burst");

        assertThat(statuses.subList(0, RATE_LIMIT_BURST)).containsOnly(HttpStatus.OK);
        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS);

        client.post().uri(ORDERS)
                .header("X-API-Key", "sd_live_burst")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().valueEquals("X-RateLimit-Remaining", "0")
                .expectHeader().exists("X-RateLimit-Burst-Capacity")
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/too-many-requests")
                .jsonPath("$.status").isEqualTo(429)
                .jsonPath("$.requestId").isNotEmpty();
    }

    @Test
    void twoMerchantKeysHaveSeparateLimits() {
        assertThat(merchantBurst("sd_live_first")).contains(HttpStatus.TOO_MANY_REQUESTS);

        client.post().uri(ORDERS)
                .header("X-API-Key", "sd_live_second")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void rawApiKeyNeverReachesRedis() {
        String rawKey = "sd_live_secret_value_9f8e";
        client.post().uri(ORDERS).header("X-API-Key", rawKey).exchange().expectStatus().isOk();

        List<String> keys = redisKeys();
        assertThat(keys).isNotEmpty().noneMatch(key -> key.contains(rawKey));
        assertThat(keys).anyMatch(key -> key.contains("merchant-api:" + sha256(rawKey)));
    }

    // â”€â”€â”€ Public fee estimate â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void estimateOverBurstFromOneClientIsTooManyRequests() {
        List<HttpStatus> statuses = estimateBurst("203.0.113.7");

        assertThat(statuses.subList(0, RATE_LIMIT_BURST)).containsOnly(HttpStatus.OK);
        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS);

        client.get().uri(ESTIMATE)
                .header("X-Forwarded-For", "203.0.113.7")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/too-many-requests");
    }

    @Test
    void twoClientIpsHaveSeparateEstimateLimits() {
        assertThat(estimateBurst("203.0.113.10")).contains(HttpStatus.TOO_MANY_REQUESTS);

        client.get().uri(ESTIMATE)
                .header("X-Forwarded-For", "203.0.113.11")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void spoofedExtraForwardedForEntryDoesNotGetAFreshBucket() {
        // nginx appends the real client address last; anything before it came from the client
        assertThat(estimateBurst("203.0.113.20")).contains(HttpStatus.TOO_MANY_REQUESTS);

        client.get().uri(ESTIMATE)
                .header("X-Forwarded-For", "198.51.100.99, 203.0.113.20")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void estimateWithoutForwardedForUsesTheRemoteAddress() {
        client.get().uri(ESTIMATE).exchange().expectStatus().isOk();

        assertThat(redisKeys()).anyMatch(key -> key.contains("pricing-estimate:"));
    }

    @Test
    void merchantAndEstimateBucketsDoNotCollide() {
        client.post().uri(ORDERS).header("X-API-Key", "127.0.0.1").exchange().expectStatus().isOk();
        client.get().uri(ESTIMATE).header("X-Forwarded-For", "127.0.0.1").exchange().expectStatus().isOk();

        assertThat(redisKeys())
                .anyMatch(key -> key.contains("merchant-api:" + sha256("127.0.0.1")))
                .anyMatch(key -> key.contains("pricing-estimate:" + sha256("127.0.0.1")))
                .noneMatch(key -> key.contains("{127.0.0.1}"));
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private List<HttpStatus> merchantBurst(String apiKey) {
        List<HttpStatus> statuses = new ArrayList<>();
        for (int i = 0; i < RATE_LIMIT_BURST + 2; i++) {
            statuses.add(HttpStatus.resolve(client.post().uri(ORDERS).header("X-API-Key", apiKey)
                    .exchange().returnResult(String.class).getStatus().value()));
        }
        return statuses;
    }

    private List<HttpStatus> estimateBurst(String clientIp) {
        List<HttpStatus> statuses = new ArrayList<>();
        for (int i = 0; i < RATE_LIMIT_BURST + 2; i++) {
            statuses.add(HttpStatus.resolve(client.get().uri(ESTIMATE).header("X-Forwarded-For", clientIp)
                    .exchange().returnResult(String.class).getStatus().value()));
        }
        return statuses;
    }

    private List<String> redisKeys() {
        return redis.keys("*").collectList().block(Duration.ofSeconds(5));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
