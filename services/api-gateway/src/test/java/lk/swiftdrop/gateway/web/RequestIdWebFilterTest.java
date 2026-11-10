package lk.swiftdrop.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

class RequestIdWebFilterTest {

    private final RequestIdWebFilter filter = new RequestIdWebFilter();

    @Test
    void generatesIdWhenAbsent() {
        Result result = run(MockServerHttpRequest.get("/api/v1/track/x"));

        assertThat(UUID.fromString(result.forwardedHeader())).isNotNull();
        assertThat(result.attribute()).isEqualTo(result.forwardedHeader());
        assertThat(result.responseHeader()).isEqualTo(result.forwardedHeader());
    }

    @Test
    void keepsValidId() {
        Result result = run(MockServerHttpRequest.get("/x").header("X-Request-Id", "abc-123"));

        assertThat(result.forwardedHeader()).isEqualTo("abc-123");
        assertThat(result.responseHeader()).isEqualTo("abc-123");
    }

    @Test
    void replacesInvalidIdRatherThanAppending() {
        Result result = run(MockServerHttpRequest.get("/x").header("X-Request-Id", "has spaces\r\n"));

        assertThat(result.forwardedHeader()).isNotEqualTo("has spaces\r\n");
        assertThat(UUID.fromString(result.forwardedHeader())).isNotNull();
        assertThat(result.forwardedCount()).isEqualTo(1);
    }

    @Test
    void runsFirst() {
        assertThat(filter.getOrder()).isEqualTo(Integer.MIN_VALUE);
        assertThat(new BlockedPathWebFilter(null).getOrder()).isGreaterThan(filter.getOrder());
    }

    private Result run(MockServerHttpRequest.BaseBuilder<?> request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> seen = new AtomicReference<>();
        WebFilterChain chain = next -> {
            seen.set(next);
            return next.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange forwarded = seen.get();
        return new Result(
                forwarded.getRequest().getHeaders().getFirst("X-Request-Id"),
                forwarded.getRequest().getHeaders().get("X-Request-Id").size(),
                forwarded.getAttribute(RequestIdWebFilter.ATTRIBUTE),
                exchange.getResponse().getHeaders().getFirst("X-Request-Id"));
    }

    private record Result(String forwardedHeader, int forwardedCount, String attribute, String responseHeader) {
    }
}
