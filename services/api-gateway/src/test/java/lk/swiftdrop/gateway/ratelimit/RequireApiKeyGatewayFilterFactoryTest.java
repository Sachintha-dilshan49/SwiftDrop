package lk.swiftdrop.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import lk.swiftdrop.gateway.error.ProblemResponseWriter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

class RequireApiKeyGatewayFilterFactoryTest {

    private final GatewayFilter filter =
            new RequireApiKeyGatewayFilterFactory(new ProblemResponseWriter(JsonMapper.builder().build())).apply(new Object());

    @Test
    void requestWithoutKeyIsRejectedWithProblemAndNotForwarded() {
        AtomicBoolean forwarded = new AtomicBoolean();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/merchant-api/orders"));

        filter.filter(exchange, chain(forwarded)).block();

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"type\":\"https://swiftdrop.lk/problems/unauthorized\"")
                .contains("\"detail\":\"Missing X-API-Key header\"")
                .contains("\"instance\":\"/api/v1/merchant-api/orders\"");
    }

    @Test
    void blankKeyIsRejected() {
        AtomicBoolean forwarded = new AtomicBoolean();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/merchant-api/orders").header("X-API-Key", " "));

        filter.filter(exchange, chain(forwarded)).block();

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requestWithKeyContinues() {
        AtomicBoolean forwarded = new AtomicBoolean();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/merchant-api/orders").header("X-API-Key", "sd_live_x"));

        filter.filter(exchange, chain(forwarded)).block();

        assertThat(forwarded).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private static GatewayFilterChain chain(AtomicBoolean forwarded) {
        return exchange -> {
            forwarded.set(true);
            return Mono.empty();
        };
    }
}
