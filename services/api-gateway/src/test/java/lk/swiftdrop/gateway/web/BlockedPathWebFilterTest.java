package lk.swiftdrop.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import lk.swiftdrop.gateway.error.ProblemResponseWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

class BlockedPathWebFilterTest {

    private final BlockedPathWebFilter filter =
            new BlockedPathWebFilter(new ProblemResponseWriter(JsonMapper.builder().build()));

    @ParameterizedTest
    @ValueSource(strings = {"/internal", "/internal/users/1/contact", "/api/v1/internal", "/api/v1/internal/x",
            "/actuator", "/actuator/env", "/actuator/gateway/routes"})
    void blocksInternalAndNonHealthActuatorPaths(String path) {
        assertThat(BlockedPathWebFilter.isBlocked(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/parcels", "/internalx", "/api/v1/internals", "/api/v1/parcels/internal",
            "/actuator/health", "/actuator/health/liveness", "/actuatorx"})
    void allowsEverythingElse(String path) {
        assertThat(BlockedPathWebFilter.isBlocked(path)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/parcels/../../../internal/x, /internal/x",
            "/%69nternal/users, /internal/users",
            "//internal//users, /internal/users",
            "/internal;jsessionid=1/users, /internal/users",
            "/../internal/x, /internal/x",
            "/api/v1/parcels/1, /api/v1/parcels/1"})
    void normalizesLikeAServletContainer(String raw, String expected) {
        assertThat(BlockedPathWebFilter.normalize(raw)).isEqualTo(expected);
    }

    @Test
    void writesNotFoundProblemAndStops() {
        AtomicBoolean continued = new AtomicBoolean();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/users/1"));

        filter.filter(exchange, next -> {
            continued.set(true);
            return Mono.empty();
        }).block();

        assertThat(continued).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"type\":\"https://swiftdrop.lk/problems/not-found\"")
                .contains("\"instance\":\"/internal/users/1\"");
    }

    @Test
    void passesOtherPathsOn() {
        AtomicBoolean continued = new AtomicBoolean();

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/parcels")), next -> {
            continued.set(true);
            return Mono.empty();
        }).block();

        assertThat(continued).isTrue();
    }
}
